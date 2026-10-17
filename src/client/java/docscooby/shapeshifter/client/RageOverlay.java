package docscooby.shapeshifter.client;

import docscooby.shapeshifter.Shapeshifter;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;

public final class RageOverlay {

    private static final float MAX_ALPHA = 0.55F;
    private static final int FADE_IN_TICKS = 10;
    private static final int FADE_OUT_TICKS = 40;
    private static final float BAND_FRACTION = 0.22F;
    private static final int RED = 0xC00000;

    private static long endGameTime = 0;
    private static int totalTicks = 0;

    private RageOverlay() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(Shapeshifter.RAGE_STATE, (client, handler, buf, responseSender) -> {

            int ticks = buf.readVarInt();

            client.execute(() -> {
                if (client.level == null) return;
                totalTicks = ticks;
                endGameTime = client.level.getGameTime() + ticks;
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            endGameTime = 0;
            totalTicks = 0;
        });

        HudRenderCallback.EVENT.register(RageOverlay::render);
    }

    private static void render(GuiGraphics graphics, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        if (!ShapeshifterClient.isShapeshifter || mc.player.isDeadOrDying()) return;

        float now = mc.level.getGameTime() + tickDelta;
        float remaining = endGameTime - now;
        if (remaining <= 0 || remaining > totalTicks) return;

        float elapsed = totalTicks - remaining;
        float fade = Math.min(1.0F, Math.min(elapsed / FADE_IN_TICKS, remaining / FADE_OUT_TICKS));
        float pulse = 0.85F + 0.15F * Mth.sin(now * 0.25F);
        float strength = MAX_ALPHA * fade * pulse;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int band = (int) (Math.min(width, height) * BAND_FRACTION);


        graphics.fillGradient(0, 0, width, band, argb(strength), argb(0.0F));
        graphics.fillGradient(0, height - band, width, height, argb(0.0F), argb(strength));

        for (int i = 0; i < band; i++) {
            int color = argb(strength * (1.0F - (float) i / band));
            graphics.fill(i, 0, i + 1, height, color);
            graphics.fill(width - i - 1, 0, width - i, height, color);
        }
    }

    private static int argb(float alpha) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | RED;
    }
}
