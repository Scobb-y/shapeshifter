package docscooby.shapeshifter.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.blaze3d.platform.InputConstants;
import docscooby.shapeshifter.Shapeshifter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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
    public static boolean isRecording = false;
    public static boolean isShapeshifter = false;
    public static UUID highlightedTarget = null;
    public static UUID recordingTarget = null;

    private static KeyMapping toggleTargetMode;
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
                "key.shapeshifter.hunt", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Z, "key.categories.shapeshifter"
        ));

        toggleTargetMode = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.shapeshifter.target_mode", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_1, "key.categories.shapeshifter"
        ));

        toggleRecording = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.shapeshifter.toggle_recording", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_2, "key.categories.shapeshifter"
        ));

        ClientPlayNetworking.registerGlobalReceiver(Shapeshifter.PROFILES_LIST, (client, handler, buf, responseSender) -> {
            List<String> names = buf.readList(FriendlyByteBuf::readUtf);
            client.execute(() -> client.setScreen(new ShapeshifterListScreen(names)));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean listClicked = toggleList.consumeClick();
            boolean rageClicked = rage.consumeClick();
            boolean huntClicked = hunt.consumeClick();
            boolean targetModeClicked = toggleTargetMode.consumeClick();
            boolean recordClicked = toggleRecording.consumeClick();

            if (client.player == null) return;

            if (!isShapeshifter) {
                if (isRecording) stopRecording();
                targetModeActive = false;
                highlightedTarget = null;
                return;
            }

            if (listClicked)  ClientPlayNetworking.send(Shapeshifter.REQUEST_PROFILES, PacketByteBufs.empty());
            if (rageClicked)  ClientPlayNetworking.send(Shapeshifter.APPLY_RAGE, PacketByteBufs.empty());
            if (huntClicked)  ClientPlayNetworking.send(Shapeshifter.HUNT, PacketByteBufs.empty());
            if (targetModeClicked) targetModeActive = !targetModeActive;

            if (isRecording && distanceTo(client, recordingTarget) >= 10.0) {
                stopRecording();
            }

            highlightedTarget = isRecording
                    ? recordingTarget
                    : (targetModeActive ? raycastPlayer(client) : null);

            if (recordClicked) {
                if (isRecording) {
                    stopRecording();
                } else if (highlightedTarget != null) {
                    FriendlyByteBuf buf = PacketByteBufs.create();
                    buf.writeUUID(highlightedTarget);
                    ClientPlayNetworking.send(Shapeshifter.SELECT_TARGET, buf);
                    ClientPlayNetworking.send(Shapeshifter.TOGGLE_RECORDING, PacketByteBufs.empty());
                    recordingTarget = highlightedTarget;
                    isRecording = true;
                }
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

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            isShapeshifter = false;
            targetModeActive = false;
            isRecording = false;
            recordingTarget = null;
            highlightedTarget = null;
        });

        ClientPlayNetworking.registerGlobalReceiver(Shapeshifter.SHAPESHIFTER_STATUS, (client, handler, buf, responseSender) -> {
            boolean value = buf.readBoolean();
            client.execute(() -> isShapeshifter = value);
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
                100.0
        );

        return hit != null ? hit.getEntity().getUUID() : null;
    }

    private static double distanceTo(Minecraft client, UUID uuid) {
        if (uuid == null || client.player == null || client.level == null) return Double.MAX_VALUE;
        Player target = client.level.getPlayerByUUID(uuid);
        if (target == null) return Double.MAX_VALUE;
        return client.player.distanceTo(target);
    }

    private static void stopRecording() {
        ClientPlayNetworking.send(Shapeshifter.TOGGLE_RECORDING, PacketByteBufs.empty());
        isRecording = false;
        recordingTarget = null;
    }

    public static boolean isOurKey(KeyMapping key) {
        return key == toggleTargetMode || key == toggleRecording || key == toggleList
                || key == rage || key == hunt;
    }
}