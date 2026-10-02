package docscooby.shapeshifter;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;

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

    public static VoicechatServerApi getApi() { return serverApi; }
}