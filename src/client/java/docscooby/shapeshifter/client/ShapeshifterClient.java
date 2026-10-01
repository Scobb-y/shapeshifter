package docscooby.shapeshifter.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.blaze3d.platform.InputConstants;
import docscooby.shapeshifter.Shapeshifter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

public class ShapeshifterClient implements ClientModInitializer {

    public static boolean targetModeActive = false;
    public static UUID highlightedTarget = null;

    private static KeyMapping toggleTargetMode;
    private static KeyMapping selectTarget;
    private static KeyMapping toggleRecording;
    private static KeyMapping toggleList;
    private static KeyMapping rage;
    private static KeyMapping hunt;

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

        hunt = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.shapeshifter.copy", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Z, "key.categories.shapeshifter"
        ));

        toggleTargetMode = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.shapeshifter.target_mode", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_T, "key.categories.shapeshifter"
        ));

        selectTarget = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.shapeshifter.select_target", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X, "key.categories.shapeshifter"
        ));

        toggleRecording = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.shapeshifter.toggle_recording", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V, "key.categories.shapeshifter"
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
            if (hunt.consumeClick()) {
                ClientPlayNetworking.send(Shapeshifter.HUNT, PacketByteBufs.empty());
            }

            if (toggleTargetMode.consumeClick()) {
                targetModeActive = !targetModeActive;
            }

            highlightedTarget = targetModeActive ? raycastPlayer(client) : null;

            if (selectTarget.consumeClick() && highlightedTarget != null) {
                FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeUUID(highlightedTarget);
                ClientPlayNetworking.send(Shapeshifter.SELECT_TARGET, buf);
            }

            if (toggleRecording.consumeClick()) {
                ClientPlayNetworking.send(Shapeshifter.TOGGLE_RECORDING, PacketByteBufs.empty());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(Shapeshifter.SELF_SKIN_UPDATE, (client, handler, buf, responseSender) -> {
            String value = buf.readUtf();
            boolean hasSignature = buf.readBoolean();
            String signature = hasSignature ? buf.readUtf() : null;

            client.execute(() -> {
                if (client.player == null) return;

                GameProfile profile = client.player.getGameProfile();
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", new Property("textures", value, signature));

                client.getSkinManager().registerSkins(profile, (type, location, texture) -> {}, true);
                ((PlayerInfoAccessor) client.player).shapeshifter$refreshPlayerInfo();
            });
        });
    }

    private static UUID raycastPlayer(Minecraft client) {
        if (client.player == null || client.level == null) return null;
        Player player = client.player;
        Vec3 eyePos = player.getEyePosition();
        Vec3 reach = eyePos.add(player.getViewVector(1.0F).scale(10.0));
        AABB searchBox = player.getBoundingBox().expandTowards(reach.subtract(eyePos)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, eyePos, reach, searchBox,
                entity -> entity instanceof Player && entity != player,
                10.0
        );

        return hit != null ? hit.getEntity().getUUID() : null;
    }
}