package docscooby.shapeshifter.client;

import com.mojang.blaze3d.platform.InputConstants;
import docscooby.shapeshifter.network.ApplyRagePayload;
import docscooby.shapeshifter.network.RequestProfilesPayload;
import docscooby.shapeshifter.network.ProfilesListPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ShapeshifterClient implements ClientModInitializer {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("shapeshifter", "main"));

    private static KeyMapping toggleList;
    private static KeyMapping rage;

	@Override
	public void onInitializeClient() {
        toggleList = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                    "key.shapeshifter.toggle",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_CAPS_LOCK,
                    CATEGORY
        ));

        rage = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.shapeshifter.rage",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        CATEGORY
                )
        );

        ClientPlayNetworking.registerGlobalReceiver(ProfilesListPayload.TYPE, (payload, context) ->
                context.client().execute(() ->
                        context.client().setScreenAndShow(new ShapeshifterListScreen(payload.names()))
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleList.consumeClick()) {
                ClientPlayNetworking.send(new RequestProfilesPayload());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (rage.consumeClick()) {
                ClientPlayNetworking.send(new ApplyRagePayload());
            }
        });



	}
}