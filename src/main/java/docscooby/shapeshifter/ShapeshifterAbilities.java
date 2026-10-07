package docscooby.shapeshifter;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShapeshifterAbilities {

    public static final int RAGE_COOLDOWN_TICKS = 20 * 60 * 5;   // 5 minutes
    public static final int HUNT_COOLDOWN_TICKS = 20 * 60 * 3;   // 3 minutes
    public static final int RAGE_DURATION_TICKS = 20 * 30;       // night buffs / daytime immunity length
    public static final double HUNT_RANGE = 100.0;
    public static final float SUN_DAMAGE_MULTIPLIER = 1.5F;      // damage taken in sunlight

    private static final int SUN_CHECK_INTERVAL = 10;            // ticks between daylight checks
    private static final int SUN_WEAKNESS_TICKS = 220;

    private static final Map<UUID, Long> RAGE_READY_AT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> HUNT_READY_AT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SUN_IMMUNE_UNTIL = new ConcurrentHashMap<>();
    private static final Set<UUID> SUN_VULNERABLE = ConcurrentHashMap.newKeySet();

    private ShapeshifterAbilities() {}

    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        if (now % SUN_CHECK_INTERVAL != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updateSun(player, now);
        }
    }

    public static boolean isSunVulnerable(ServerPlayer player) {
        return SUN_VULNERABLE.contains(player.getUUID());
    }

    public static void forget(UUID player) {
        SUN_VULNERABLE.remove(player);
    }

    private static boolean isSunlit(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos eyePos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        return level.isDay() && level.canSeeSky(eyePos);
    }

    private static void updateSun(ServerPlayer player, long now) {
        UUID id = player.getUUID();
        boolean immune = now < SUN_IMMUNE_UNTIL.getOrDefault(id, 0L);
        boolean burning = Shapeshifter.isShapeshifter(player) && !immune && isSunlit(player);

        if (burning) {
            SUN_VULNERABLE.add(id);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SUN_WEAKNESS_TICKS, 0, false, false, true));
        } else if (SUN_VULNERABLE.remove(id)) {
            player.removeEffect(MobEffects.WEAKNESS);
        }
    }

    public static void rage(ServerPlayer player) {
        long now = player.server.getTickCount();
        if (isOnCooldown(player, RAGE_READY_AT, "Rage", now)) return;
        RAGE_READY_AT.put(player.getUUID(), now + RAGE_COOLDOWN_TICKS);

        if (player.serverLevel().isDay()) {
            SUN_IMMUNE_UNTIL.put(player.getUUID(), now + RAGE_DURATION_TICKS);
            updateSun(player, now);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, RAGE_DURATION_TICKS, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, RAGE_DURATION_TICKS, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1, false, false, true));
        }
    }

    public static void hunt(ServerPlayer player) {
        long now = player.server.getTickCount();
        if (isOnCooldown(player, HUNT_READY_AT, "Hunt", now)) return;

        MinecraftServer server = player.server;
        ServerPlayer closest = null;
        float closestDistance = Float.MAX_VALUE;
        boolean foundInRange = false;

        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == player || other.serverLevel() != player.serverLevel()) continue;

            float distance = player.distanceTo(other);
            if (distance < HUNT_RANGE) {
                ShapeshifterDisguiseManager.sendScopedGlow(server, player, other);
                foundInRange = true;
            }
            if (distance < closestDistance) {
                closest = other;
                closestDistance = distance;
            }
        }

        if (foundInRange) {
            HUNT_READY_AT.put(player.getUUID(), now + HUNT_COOLDOWN_TICKS);
            return;
        }

        if (closest == null) {
            player.displayClientMessage(Component.literal("No other players in this dimension."), true);
            return;
        }

        HUNT_READY_AT.put(player.getUUID(), now + HUNT_COOLDOWN_TICKS);
        BlockPos pos = closest.blockPosition();
        player.sendSystemMessage(Component.literal(String.format(
                "Nearest player: %s at %d, %d, %d (%d blocks away)",
                closest.getName().getString(), pos.getX(), pos.getY(), pos.getZ(),
                Math.round(closestDistance))));
    }

    private static boolean isOnCooldown(ServerPlayer player, Map<UUID, Long> readyAt, String name, long now) {
        Long ready = readyAt.get(player.getUUID());
        if (ready == null || now >= ready) return false;

        int seconds = (int) Math.ceil((ready - now) / 20.0);
        player.displayClientMessage(Component.literal(
                String.format("%s is on cooldown: %d:%02d", name, seconds / 60, seconds % 60)), true);
        return true;
    }
}
