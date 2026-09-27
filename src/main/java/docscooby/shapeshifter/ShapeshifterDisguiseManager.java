package docscooby.shapeshifter;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class ShapeshifterDisguiseManager {

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
}