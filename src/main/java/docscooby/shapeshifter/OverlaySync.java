package docscooby.shapeshifter;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OverlaySync {
    private static final Map<UUID, String> LAST_SENT = new ConcurrentHashMap<>();

    private OverlaySync() {}

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sync(player, false);
        }
    }

    public static void force(ServerPlayer player) {
        sync(player, true);
    }

    public static void forget(UUID player) {
        LAST_SENT.remove(player);
    }

    private static void sync(ServerPlayer player, boolean force) {
        ShapeshifterDisguiseManager.DisguiseInfo info = ShapeshifterDisguiseManager.getDisguise(player.getUUID());
        boolean active = info != null && Shapeshifter.isShapeshifter(player);

        List<List<byte[]>> clips = active ? ShapeshifterVoiceManager.getVisibleRecordings(info.id()) : List.of();
        boolean speakerLive = active && ShapeshifterVoiceManager.isRecording(info.id());

        // Cheap change-detection key
        StringBuilder key = new StringBuilder(active ? info.name() : "none");
        for (int i = 0; i < clips.size(); i++) {
            boolean live = speakerLive && i == clips.size() - 1;
            key.append('|').append(clips.get(i).size() / 50).append(live ? "L" : "");
        }
        String keyString = key.toString();

        if (!force && keyString.equals(LAST_SENT.get(player.getUUID()))) return;
        LAST_SENT.put(player.getUUID(), keyString);

        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(active);
        if (active) {
            buf.writeUtf(info.name());
            buf.writeVarInt(clips.size());
            for (int i = 0; i < clips.size(); i++) {
                buf.writeVarInt(clips.get(i).size() / 50); // frames are 20 ms each -> 50 per second
                buf.writeBoolean(speakerLive && i == clips.size() - 1);
            }
        }
        ServerPlayNetworking.send(player, Shapeshifter.DISGUISE_OVERLAY, buf);
    }
}