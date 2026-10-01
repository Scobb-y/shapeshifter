package docscooby.shapeshifter;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ShapeshifterVoiceManager {

    private static final Map<UUID, List<byte[]>> RECORDINGS = new ConcurrentHashMap<>();
    private static final Set<UUID> ACTIVELY_RECORDING = ConcurrentHashMap.newKeySet();

    public static void startRecording(UUID speaker) {
        RECORDINGS.put(speaker, new CopyOnWriteArrayList<>());
        ACTIVELY_RECORDING.add(speaker);
    }

    public static void stopRecording(UUID speaker) {
        ACTIVELY_RECORDING.remove(speaker);
    }

    public static boolean isRecording(UUID speaker) {
        return ACTIVELY_RECORDING.contains(speaker);
    }

    public static void append(UUID speaker, byte[] opusFrame) {
        List<byte[]> frames = RECORDINGS.get(speaker);
        if (frames != null) {
            frames.add(opusFrame);
        }
    }

    public static List<byte[]> getRecording(UUID speaker) {
        return RECORDINGS.getOrDefault(speaker, List.of());
    }

    public static void playback(VoicechatServerApi api, Entity target, List<byte[]> recording) {
        if (recording.isEmpty()) return;

        UUID channelId = UUID.randomUUID();
        EntityAudioChannel channel = api.createEntityAudioChannel(channelId, api.fromEntity(target));
        if (channel == null) return;

        channel.setDistance(48);

        AtomicInteger index = new AtomicInteger(0);

        Future<?>[] handle = new Future<?>[1];
        handle[0] = ShapeshifterDisguiseManager.SCHEDULER.scheduleAtFixedRate(() -> {
            int i = index.getAndIncrement();
            if (i >= recording.size()) {
                channel.flush();
                handle[0].cancel(false);
                return;
            }
            channel.send(recording.get(i));
        }, 0, 20, TimeUnit.MILLISECONDS);
    }
}