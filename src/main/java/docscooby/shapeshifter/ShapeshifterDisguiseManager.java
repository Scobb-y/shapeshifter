package docscooby.shapeshifter;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import docscooby.shapeshifter.mixin.EntitySharedFlagsAccessor;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ShapeshifterDisguiseManager {

    private static final int FLAG_GLOWING = 6;
    public static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);

    public static void disguise(ServerPlayer target, GameProfile source) {
        GameProfile disguised = buildDisguisedProfile(target.getGameProfile(), source);
        ((GameProfileAccessor) target).shapeshifter$setGameProfile(disguised);
        refresh(target);
        sendSelfSkinUpdate(target, source);
    }

    public static void clearDisguise(ServerPlayer target) {
        GameProfileAccessor accessor = (GameProfileAccessor) target;
        GameProfile original = accessor.shapeshifter$getOriginalGameProfile();
        if (original == null) return;

        accessor.shapeshifter$setGameProfile(original);
        refresh(target);
        sendSelfSkinUpdate(target, original);
    }

    private static GameProfile buildDisguisedProfile(GameProfile original, GameProfile source) {
        GameProfile copy = new GameProfile(original.getId(), original.getName());
        for (Property p : source.getProperties().get("textures")) {
            copy.getProperties().put("textures", p);
        }
        return copy;
    }

    private static void refresh(ServerPlayer target) {
        ServerLevel serverLevel = target.serverLevel();
        MinecraftServer server = serverLevel.getServer();

        ClientboundPlayerInfoRemovePacket removeInfo =
                new ClientboundPlayerInfoRemovePacket(List.of(target.getUUID()));
        ClientboundPlayerInfoUpdatePacket addInfo =
                ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(target));

        List<SynchedEntityData.DataValue<?>> entityData = target.getEntityData().getNonDefaultValues();

        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            viewer.connection.send(removeInfo);
            viewer.connection.send(addInfo);

            if (viewer != target) {
                viewer.connection.send(new ClientboundRemoveEntitiesPacket(target.getId()));
                viewer.connection.send(new ClientboundAddPlayerPacket(target)); // <-- was ClientboundAddEntityPacket

                if (entityData != null) {
                    viewer.connection.send(new ClientboundSetEntityDataPacket(target.getId(), entityData));
                }
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

    public static void sendScopedGlow(MinecraftServer server, ServerPlayer viewer, ServerPlayer target) {
        EntityDataAccessor<Byte> flagsAccessor = EntitySharedFlagsAccessor.getDataSharedFlagsId();
        byte currentFlags = target.getEntityData().get(flagsAccessor);
        byte glowingFlags = (byte) (currentFlags | (1 << FLAG_GLOWING));

        viewer.connection.send(new ClientboundSetEntityDataPacket(
                target.getId(), List.of(SynchedEntityData.DataValue.create(flagsAccessor, glowingFlags))
        ));

        SCHEDULER.schedule(() -> server.execute(() -> {
            byte realFlags = target.getEntityData().get(flagsAccessor);
            viewer.connection.send(new ClientboundSetEntityDataPacket(
                    target.getId(), List.of(SynchedEntityData.DataValue.create(flagsAccessor, realFlags))
            ));
        }), 30, TimeUnit.SECONDS);
    }
}