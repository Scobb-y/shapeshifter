package docscooby.shapeshifter.client.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void shapeshifter$hideTabList(GuiGraphics graphics, int width, Scoreboard scoreboard,
                                          Objective objective, CallbackInfo ci) {
        ci.cancel();
    }
}