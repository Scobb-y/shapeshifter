package docscooby.shapeshifter;

import de.maxhenkel.voicechat.api.Entity;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;

import java.util.List;
import java.util.UUID;

public class ShapeshifterVoicePlugin implements VoicechatPlugin {

    private static VoicechatServerApi serverApi;

    @Override
    public String getPluginId() {
        return "shapeshifter_2.6.24";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, e -> serverApi = e.getVoicechat());
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicPacket);
    }

    private void onMicPacket(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null) return;
        UUID speaker = event.getSenderConnection().getPlayer().getUuid();

        if (ShapeshifterVoiceManager.isRecording(speaker)) {
            ShapeshifterVoiceManager.append(speaker, event.getPacket().getOpusEncodedData());
        }
    }

    public static void playback(VoicechatServerApi api, Entity target, List<byte[]> recording) {
        if (recording.isEmpty()) return;

        UUID channelId = UUID.randomUUID();
        EntityAudioChannel channel = api.createEntityAudioChannel(channelId, api.fromEntity(target));
        if (channel == null) return;

        channel.setDistance(48);

        new Thread(() -> {
            for (byte[] frame : recording) {
                channel.send(frame);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    return;
                }
            }
            channel.flush();
        }).start();
    }

    public static VoicechatServerApi getApi() { return serverApi; }
}