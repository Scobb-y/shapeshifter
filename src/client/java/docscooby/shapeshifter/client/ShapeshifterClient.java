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
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShapeshifterClient implements ClientModInitializer {

    public static boolean targetModeActive = false;
    public static boolean isRecording = false;
    public static boolean isShapeshifter = false;
    public static UUID highlightedTarget = null;
    public static UUID recordingTarget = null;

    private static final Map<UUID, Long> GLOW_UNTIL = new ConcurrentHashMap<>();
    private static KeyMapping toggleTargetMode;
    private static KeyMapping toggleRecording;
    private static KeyMapping toggleList;
    private static KeyMapping rage;
    private static KeyMapping hunt;

    @Override
    public void onInitializeClient() {

        RecordingOverlay.register();
        RageOverlay.register();

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
            List<String> received = buf.readList(FriendlyByteBuf::readUtf); // read the buffer here, on the network thread
            client.execute(() -> {
                if (client.player == null) return;
                List<String> names = new ArrayList<>(received);
                names.add(0, client.player.getName().getString());
                client.setScreen(new ShapeshifterListScreen(names));
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean listClicked = toggleList.consumeClick();
            boolean rageClicked = rage.consumeClick();
            boolean huntClicked = hunt.consumeClick();
            boolean targetModeClicked = toggleTargetMode.consumeClick();
            boolean recordClicked = toggleRecording.consumeClick();
            boolean[] slotClicks = RecordingOverlay.consumeKeys();

            if (client.player == null) return;

            if (!isShapeshifter) {
                if (isRecording) stopRecording();
                targetModeActive = false;
                highlightedTarget = null;
                return;
            }

            RecordingOverlay.sendClicks(slotClicks);

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
                    sendRecording(true);
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
            RecordingOverlay.clear();
            GLOW_UNTIL.clear();
        });

        ClientPlayNetworking.registerGlobalReceiver(Shapeshifter.SHAPESHIFTER_STATUS, (client, handler, buf, responseSender) -> {
            boolean value = buf.readBoolean();
            client.execute(() -> isShapeshifter = value);
        });

        ClientPlayNetworking.registerGlobalReceiver(Shapeshifter.RECORDING_STATE, (client, handler, buf, responseSender) -> {
            boolean active = buf.readBoolean();
            client.execute(() -> {
                isRecording = active;
                if (!active) recordingTarget = null;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Shapeshifter.GLOW_PLAYERS, (client, handler, buf, responseSender) -> {
            int durationTicks = buf.readVarInt();
            List<UUID> ids = buf.readList(FriendlyByteBuf::readUUID);
            client.execute(() -> {
                long until = System.currentTimeMillis() + durationTicks * 50L;
                for (UUID id : ids) GLOW_UNTIL.put(id, until);
            });
        });

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) return; // respect F1
            if (!ShapeshifterClient.isShapeshifter) return;

            int y = 6;

            if (ShapeshifterClient.isRecording) {
                graphics.fill(6, y, 10, y + 4, 0xFFFF2020);           // red dot
                graphics.drawString(mc.font, "REC", 14, y - 2, 0xFFFF5555);
                y += 12;
            }

            if (ShapeshifterClient.targetModeActive) {
                graphics.drawString(mc.font, "Target mode", 6, y, 0xFFFFFFFF);
            }
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

    private static void sendRecording(boolean start) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(start);
        ClientPlayNetworking.send(Shapeshifter.SET_RECORDING, buf);
    }

    private static void stopRecording() {
        sendRecording(false);
        isRecording = false;
        recordingTarget = null;
    }

    public static boolean isOurKey(KeyMapping key) {
        return key == toggleTargetMode || key == toggleRecording || key == toggleList
                || key == rage || key == hunt || RecordingOverlay.isSlotKey(key);
    }

    public static boolean isGlowing(UUID id) {
        Long until = GLOW_UNTIL.get(id);
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            GLOW_UNTIL.remove(id);
            return false;
        }
        return isShapeshifter;
    }
}