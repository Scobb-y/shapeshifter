package docscooby.shapeshifter;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class ShapeshifterDisguiseManager {

    public static void disguise(ServerPlayer target, GameProfile source) {
        GameProfile disguised = buildDisguisedProfile(target.getGameProfile(), source);
        ((GameProfileAccessor) target).shapeshifter$setGameProfile(disguised);
        refresh(target);
    }

    public static void clearDisguise(ServerPlayer target, GameProfile originalProfile) {
        ((GameProfileAccessor) target).shapeshifter$setGameProfile(originalProfile);
        refresh(target);
    }

    private static GameProfile buildDisguisedProfile(GameProfile original, GameProfile source) {
        GameProfile copy = new GameProfile(original.id(), original.name());
        for (Property p : source.properties().get("textures")) {
            copy.properties().put("textures", p);
        }
        return copy;
    }

    private static void refresh(ServerPlayer target) {
        MinecraftServer server = target.level().getServer();
        if (server == null) return;

        ClientboundPlayerInfoRemovePacket remove =
                new ClientboundPlayerInfoRemovePacket(List.of(target.getUUID()));
        ClientboundPlayerInfoUpdatePacket add =
                ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(target));

        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            viewer.connection.send(remove);
            viewer.connection.send(add);
        }
    }
}
