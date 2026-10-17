package docscooby.shapeshifter;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ShapeshifterAbilities {

    public static final int RAGE_COOLDOWN_TICKS = 20 * 60 * 5;
    public static final int HUNT_COOLDOWN_TICKS = 20 * 60 * 3;
    public static final int RAGE_DURATION_TICKS = 20 * 30;
    public static final double HUNT_RANGE = 100.0;
    public static final float SUN_DAMAGE_MULTIPLIER = 1.5F;

    private static final int SUN_CHECK_INTERVAL = 10;
    private static final int SUN_WEAKNESS_TICKS = 220;

    private static final Map<UUID, Long> RAGE_READY_AT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> HUNT_READY_AT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SUN_IMMUNE_UNTIL = new ConcurrentHashMap<>();
    private static final Set<UUID> SUN_VULNERABLE = ConcurrentHashMap.newKeySet();

    private static final int HEARTBEAT_DURATION_TICKS = 20 * 30;
    private static final int HEARTBEAT_SLOWEST_TICKS = 36;
    private static final int HEARTBEAT_FASTEST_TICKS = 12;
    private static final double HEARTBEAT_NEAR_DISTANCE = 8.0;
    private static final double HEARTBEAT_FAR_DISTANCE = 100.0;
    private static final float HEARTBEAT_VOLUME = 1.0F;

    public static final int GLOW_DURATION_TICKS = 20 * 30;

    private static final class Heartbeat {
        final UUID hunter;
        final long end;
        long nextBeat;

        Heartbeat(UUID hunter, long start, long end) {
            this.hunter = hunter;
            this.end = end;
            this.nextBeat = start;
        }
    }

    private static final Map<UUID, Heartbeat> HEARTBEATS = new ConcurrentHashMap<>();

    private ShapeshifterAbilities() {}

    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        tickHeartbeats(server, now);
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
        HEARTBEATS.remove(player);
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
        sendRageState(player, RAGE_DURATION_TICKS);

        if (player.serverLevel().isDay()) {
            SUN_IMMUNE_UNTIL.put(player.getUUID(), now + RAGE_DURATION_TICKS);
            updateSun(player, now);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, RAGE_DURATION_TICKS, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, RAGE_DURATION_TICKS, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1, false, false, true));
        }
    }

    private static void sendRageState(ServerPlayer player, int ticks) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(ticks);
        ServerPlayNetworking.send(player, Shapeshifter.RAGE_STATE, buf);
    }

    public static void hunt(ServerPlayer player) {
        long now = player.server.getTickCount();
        if (isOnCooldown(player, HUNT_READY_AT, "Hunt", now)) return;

        MinecraftServer server = player.server;
        ServerPlayer closest = null;
        float closestDistance = Float.MAX_VALUE;
        boolean foundInRange = false;

        List<UUID> glowTargets = new ArrayList<>();

        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == player
                    || !other.isAlive()
                    || other.isSpectator()
                    || other.getTags().contains("host")
                    || other.serverLevel() != player.serverLevel()) continue;

            float distance = player.distanceTo(other);
            if (distance < HUNT_RANGE) {
                glowTargets.add(other.getUUID());
                other.sendSystemMessage(Component.literal("You feel as though you are being watched... hunted...")
                        .withStyle(ChatFormatting.RED));
                startHeartbeat(other, player, now);
                foundInRange = true;
            }
            if (distance < closestDistance) {
                closest = other;
                closestDistance = distance;
            }
        }

        if (!glowTargets.isEmpty()) sendGlow(player, glowTargets);

        if (foundInRange) {
            HUNT_READY_AT.put(player.getUUID(), now + HUNT_COOLDOWN_TICKS);
            return;
        }

        if (closest == null) {
            player.displayClientMessage(Component.literal("No other players in this dimension."), true);
            return;
        }

        HUNT_READY_AT.put(player.getUUID(), now + HUNT_COOLDOWN_TICKS);
        closest.sendSystemMessage(Component.literal("A terrible chill goes up your spine...")
                .withStyle(ChatFormatting.YELLOW));
        startHeartbeat(closest, player, now);
        BlockPos pos = closest.blockPosition();
        player.sendSystemMessage(Component.literal(String.format(
                "Nearest player: %s at %d, %d, %d (%d blocks away)",
                closest.getName().getString(), pos.getX(), pos.getY(), pos.getZ(),
                Math.round(closestDistance))));
    }

    private static void startHeartbeat(ServerPlayer victim, ServerPlayer hunter, long now) {
        HEARTBEATS.put(victim.getUUID(), new Heartbeat(hunter.getUUID(), now, now + HEARTBEAT_DURATION_TICKS));
    }

    private static void tickHeartbeats(MinecraftServer server, long now) {
        Iterator<Map.Entry<UUID, Heartbeat>> it = HEARTBEATS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Heartbeat> entry = it.next();
            Heartbeat beat = entry.getValue();

            ServerPlayer victim = server.getPlayerList().getPlayer(entry.getKey());
            ServerPlayer hunter = server.getPlayerList().getPlayer(beat.hunter);
            if (victim == null || hunter == null || !victim.isAlive() || now >= beat.end) {
                it.remove();
                continue;
            }

            if (now >= beat.nextBeat) {
                victim.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, HEARTBEAT_VOLUME, 1.0F);

                double distance = hunter.serverLevel() == victim.serverLevel()
                        ? hunter.distanceTo(victim)
                        : Double.MAX_VALUE;
                float closeness = (float) Mth.clamp(
                        (distance - HEARTBEAT_NEAR_DISTANCE) / (HEARTBEAT_FAR_DISTANCE - HEARTBEAT_NEAR_DISTANCE),
                        0.0, 1.0);
                beat.nextBeat = now + Math.round(Mth.lerp(closeness, HEARTBEAT_FASTEST_TICKS, HEARTBEAT_SLOWEST_TICKS));
            }
        }
    }

    private static boolean isOnCooldown(ServerPlayer player, Map<UUID, Long> readyAt, String name, long now) {
        Long ready = readyAt.get(player.getUUID());
        if (ready == null || now >= ready) return false;

        int seconds = (int) Math.ceil((ready - now) / 20.0);
        player.displayClientMessage(Component.literal(
                String.format("%s is on cooldown: %d:%02d", name, seconds / 60, seconds % 60)), true);
        return true;
    }

    private static void sendGlow(ServerPlayer viewer, List<UUID> targets) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(GLOW_DURATION_TICKS);
        buf.writeCollection(targets, FriendlyByteBuf::writeUUID);
        ServerPlayNetworking.send(viewer, Shapeshifter.GLOW_PLAYERS, buf);
    }
}