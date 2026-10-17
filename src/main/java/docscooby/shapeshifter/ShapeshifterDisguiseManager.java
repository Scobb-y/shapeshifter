package docscooby.shapeshifter;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ShapeshifterDisguiseManager {

    public record DisguiseInfo(UUID id, String name) {}

    private static final Map<UUID, DisguiseInfo> DISGUISES = new ConcurrentHashMap<>();

    public static DisguiseInfo getDisguise(UUID player) { return DISGUISES.get(player); }
    public static void forget(UUID player) { DISGUISES.remove(player); }

    public static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "shapeshifter-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    public static void disguise(ServerPlayer target, GameProfile source) {
        GameProfile disguised = buildDisguisedProfile(target.getGameProfile(), source);
        ((GameProfileAccessor) target).shapeshifter$setGameProfile(disguised);
        DISGUISES.put(target.getUUID(), new DisguiseInfo(source.getId(), source.getName()));
        refresh(target);
        sendSelfSkinUpdate(target, source);
        OverlaySync.force(target);
    }

    public static void clearDisguise(ServerPlayer target) {
        GameProfileAccessor accessor = (GameProfileAccessor) target;
        GameProfile original = accessor.shapeshifter$getOriginalGameProfile();
        if (original == null) return;

        GameProfile restored = ProfileUtil.copy(original);
        accessor.shapeshifter$setGameProfile(restored);
        DISGUISES.remove(target.getUUID());
        refresh(target);
        sendSelfSkinUpdate(target, restored);
        OverlaySync.force(target);
    }

    private static GameProfile buildDisguisedProfile(GameProfile original, GameProfile source) {
        GameProfile copy = new GameProfile(original.getId(), original.getName());
        for (Property p : source.getProperties().get("textures")) {
            copy.getProperties().put("textures", p);
        }
        return copy;
    }

    private static void refresh(ServerPlayer target) {
        MinecraftServer server = target.serverLevel().getServer();

        ClientboundPlayerInfoRemovePacket removeInfo =
                new ClientboundPlayerInfoRemovePacket(List.of(target.getUUID()));
        ClientboundPlayerInfoUpdatePacket addInfo =
                ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(target));

        List<SynchedEntityData.DataValue<?>> entityData = target.getEntityData().getNonDefaultValues();

        List<Pair<EquipmentSlot, ItemStack>> equipment = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = target.getItemBySlot(slot);
            if (!stack.isEmpty()) equipment.add(Pair.of(slot, stack.copy()));
        }

        byte headRot = (byte) (target.getYHeadRot() * 256.0F / 360.0F);

        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            viewer.connection.send(removeInfo);
            viewer.connection.send(addInfo);

            if (viewer != target) {
                viewer.connection.send(new ClientboundRemoveEntitiesPacket(target.getId()));
                viewer.connection.send(new ClientboundAddPlayerPacket(target));

                if (entityData != null) {
                    viewer.connection.send(new ClientboundSetEntityDataPacket(target.getId(), entityData));
                }
                if (!equipment.isEmpty()) {
                    viewer.connection.send(new ClientboundSetEquipmentPacket(target.getId(), equipment));
                }
                viewer.connection.send(new ClientboundRotateHeadPacket(target, headRot));
            }
        }
    }

    private static void sendSelfSkinUpdate(ServerPlayer target, GameProfile source) {
        Property texture = source.getProperties().get("textures").stream().findFirst().orElse(null);
        if (texture == null) return;

        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(texture.getValue());
        boolean hasSignature = texture.hasSignature();
        buf.writeBoolean(hasSignature);
        if (hasSignature) buf.writeUtf(texture.getSignature());

        ServerPlayNetworking.send(target, Shapeshifter.SELF_SKIN_UPDATE, buf);
    }
}