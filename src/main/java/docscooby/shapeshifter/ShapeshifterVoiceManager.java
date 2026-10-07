package docscooby.shapeshifter;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
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

    public static final int MAX_RECORDINGS = 3;

    private static final Map<UUID, List<List<byte[]>>> RECORDINGS = new ConcurrentHashMap<>();

    private static final Set<UUID> ACTIVELY_RECORDING = ConcurrentHashMap.newKeySet();

    private static final Map<UUID, UUID> RECORDER_TO_SPEAKER = new ConcurrentHashMap<>();

    public static boolean startRecording(UUID recorder, UUID speaker) {
        stopRecordingBy(recorder);

        if (ACTIVELY_RECORDING.contains(speaker)) return false;

        List<List<byte[]>> clips = RECORDINGS.computeIfAbsent(speaker, k -> new CopyOnWriteArrayList<>());
        clips.add(new CopyOnWriteArrayList<>());
        while (clips.size() > MAX_RECORDINGS) clips.remove(0);

        ACTIVELY_RECORDING.add(speaker);
        RECORDER_TO_SPEAKER.put(recorder, speaker);
        return true;
    }

    public static boolean stopRecordingBy(UUID recorder) {
        UUID speaker = RECORDER_TO_SPEAKER.remove(recorder);
        if (speaker == null) return false;
        finish(speaker);
        return true;
    }

    public static UUID stopRecordingOf(UUID speaker) {
        UUID found = null;
        for (Map.Entry<UUID, UUID> entry : RECORDER_TO_SPEAKER.entrySet()) {
            if (entry.getValue().equals(speaker)) {
                found = entry.getKey();
                RECORDER_TO_SPEAKER.remove(found);
                break;
            }
        }
        finish(speaker);
        return found;
    }

    private static void finish(UUID speaker) {
        ACTIVELY_RECORDING.remove(speaker);

        // Drop the newest clip if nothing was captured (started, then stopped without speech)
        List<List<byte[]>> clips = RECORDINGS.get(speaker);
        if (clips != null && !clips.isEmpty() && clips.get(clips.size() - 1).isEmpty()) {
            clips.remove(clips.size() - 1);
        }
    }

    public static boolean isRecording(UUID speaker) {
        return ACTIVELY_RECORDING.contains(speaker);
    }

    public static void append(UUID speaker, byte[] opusFrame) {
        List<List<byte[]>> clips = RECORDINGS.get(speaker);
        if (clips == null || clips.isEmpty()) return;
        clips.get(clips.size() - 1).add(opusFrame);
    }

    public static List<List<byte[]>> getVisibleRecordings(UUID speaker) {
        List<List<byte[]>> clips = RECORDINGS.get(speaker);
        if (clips == null) return List.of();

        boolean live = ACTIVELY_RECORDING.contains(speaker);
        List<List<byte[]>> visible = new ArrayList<>();
        for (int i = 0; i < clips.size(); i++) {
            List<byte[]> clip = clips.get(i);
            boolean isLive = live && i == clips.size() - 1;
            if (!clip.isEmpty() || isLive) visible.add(clip);
        }
        return visible;
    }

    public static List<byte[]> getRecording(UUID speaker) {
        List<List<byte[]>> visible = getVisibleRecordings(speaker);
        return visible.isEmpty() ? List.of() : visible.get(visible.size() - 1);
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