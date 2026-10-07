package docscooby.shapeshifter.client;

import com.mojang.blaze3d.platform.InputConstants;
import docscooby.shapeshifter.Shapeshifter;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class RecordingOverlay {
    private record Slot(int seconds, boolean live) {}

    private static final int SLOT_COUNT = 3;
    private static final KeyMapping[] SLOT_KEYS = new KeyMapping[SLOT_COUNT];

    // Written on the client thread only
    private static String disguiseName = null;
    private static List<Slot> slots = List.of();

    private RecordingOverlay() {}

    public static void register() {
        int[] defaults = {GLFW.GLFW_KEY_KP_4, GLFW.GLFW_KEY_KP_5, GLFW.GLFW_KEY_KP_6};
        for (int i = 0; i < SLOT_COUNT; i++) {
            SLOT_KEYS[i] = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                    "key.shapeshifter.play_slot_" + (i + 1), InputConstants.Type.KEYSYM,
                    defaults[i], "key.categories.shapeshifter"
            ));
        }

        ClientPlayNetworking.registerGlobalReceiver(Shapeshifter.DISGUISE_OVERLAY, (client, handler, buf, responseSender) -> {
            boolean active = buf.readBoolean();
            String name = active ? buf.readUtf() : null;
            List<Slot> parsed = new ArrayList<>();
            if (active) {
                int count = buf.readVarInt();
                for (int i = 0; i < count; i++) {
                    parsed.add(new Slot(buf.readVarInt(), buf.readBoolean()));
                }
            }
            client.execute(() -> {
                disguiseName = name;
                slots = parsed;
            });
        });

        HudRenderCallback.EVENT.register(RecordingOverlay::render);
    }

    public static boolean[] consumeKeys() {
        boolean[] clicks = new boolean[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            clicks[i] = SLOT_KEYS[i].consumeClick();
        }
        return clicks;
    }

    public static void sendClicks(boolean[] clicks) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (clicks[i] && i < slots.size()) {
                FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeVarInt(i);
                ClientPlayNetworking.send(Shapeshifter.PLAY_DISGUISE_RECORDING, buf);
            }
        }
    }

    public static boolean isSlotKey(KeyMapping key) {
        for (KeyMapping slotKey : SLOT_KEYS) {
            if (slotKey == key) return true;
        }
        return false;
    }

    public static void clear() {
        disguiseName = null;
        slots = List.of();
    }

    private static void render(GuiGraphics graphics, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!ShapeshifterClient.isShapeshifter || disguiseName == null) return;

        int panelWidth = 150;
        int x = mc.getWindow().getGuiScaledWidth() - panelWidth - 6;
        int y = 6;

        int rows = Math.max(slots.size(), 1);
        graphics.fill(x - 4, y - 4, x + panelWidth, y + 14 + rows * 12 + 2, 0x90000000);

        graphics.drawString(mc.font, mc.font.plainSubstrByWidth(disguiseName, panelWidth - 4), x, y, 0xFFFFFF55);
        y += 14;

        if (slots.isEmpty()) {
            graphics.drawString(mc.font, "No recordings", x, y, 0xFFAAAAAA);
            return;
        }

        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            String keyName = SLOT_KEYS[i].getTranslatedKeyMessage().getString();
            String time = String.format("%d:%02d", slot.seconds() / 60, slot.seconds() % 60);
            String label = "[" + keyName + "] " + (slot.live() ? "Recording " : "Clip " + (i + 1) + "  ") + time;

            graphics.drawString(mc.font, label, x, y, slot.live() ? 0xFFFF5555 : 0xFFFFFFFF);
            y += 12;
        }
    }
}