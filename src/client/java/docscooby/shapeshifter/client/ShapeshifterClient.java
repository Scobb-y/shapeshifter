package docscooby.shapeshifter.client;

import com.mojang.blaze3d.platform.InputConstants;
import docscooby.shapeshifter.Shapeshifter;
import docscooby.shapeshifter.ShapeshifterData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.FriendlyByteBuf;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ShapeshifterClient implements ClientModInitializer {

    private static KeyMapping toggleList;
    private static KeyMapping rage;

    @Override
    public void onInitializeClient() {
        toggleList = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.shapeshifter.toggle", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_CAPS_LOCK, "key.categories.shapeshifter"
        ));

        rage = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.shapeshifter.rage", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R, "key.categories.shapeshifter"
        ));

        ClientPlayNetworking.registerGlobalReceiver(Shapeshifter.PROFILES_LIST, (client, handler, buf, responseSender) -> {
            List<String> names = buf.readList(FriendlyByteBuf::readUtf);
            client.execute(() -> client.setScreen(new ShapeshifterListScreen(names)));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleList.consumeClick()) {
                ClientPlayNetworking.send(Shapeshifter.REQUEST_PROFILES, PacketByteBufs.empty());
            }
            if (rage.consumeClick()) {
                ClientPlayNetworking.send(Shapeshifter.APPLY_RAGE, PacketByteBufs.empty());
            }
        });
    }
}