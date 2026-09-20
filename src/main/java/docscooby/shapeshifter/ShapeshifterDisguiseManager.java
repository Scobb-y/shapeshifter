package docscooby.shapeshifter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
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
    }

    /** Restores whatever profile the target had before it was first disguised. */
    public static void clearDisguise(ServerPlayer target) {
        GameProfileAccessor accessor = (GameProfileAccessor) target;
        GameProfile original = accessor.shapeshifter$getOriginalGameProfile();
        if (original == null) return; // never disguised, nothing to do

        accessor.shapeshifter$setGameProfile(original);
        refresh(target);
    }

    private static GameProfile buildDisguisedProfile(GameProfile original, GameProfile source) {
        Multimap<String, Property> backing = ArrayListMultimap.create();
        backing.putAll("textures", source.properties().get("textures"));

        System.out.println("[Shapeshifter] copying " + backing.get("textures").size() + " textures from " + source.name());

        PropertyMap properties = new PropertyMap(backing);
        return new GameProfile(original.id(), original.name(), properties);
    }

    private static void refresh(ServerPlayer target) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;
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
                viewer.connection.send(new ClientboundAddEntityPacket(target, 0, target.blockPosition()));

                if (entityData != null) {
                    viewer.connection.send(new ClientboundSetEntityDataPacket(target.getId(), entityData));
                }
            }
        }
    }
}