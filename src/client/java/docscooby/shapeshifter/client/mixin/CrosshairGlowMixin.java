package docscooby.shapeshifter.client.mixin;

import docscooby.shapeshifter.client.ShapeshifterClient;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class CrosshairGlowMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void shapeshifter$highlight(CallbackInfoReturnable<Boolean> cir) {
        UUID id = ((Entity) (Object) this).getUUID();
        if (id.equals(ShapeshifterClient.highlightedTarget) || ShapeshifterClient.isGlowing(id)) {
            cir.setReturnValue(true);
        }
    }
}
