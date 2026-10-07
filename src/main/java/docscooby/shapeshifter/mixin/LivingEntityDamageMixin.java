package docscooby.shapeshifter.mixin;

import docscooby.shapeshifter.ShapeshifterAbilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float shapeshifter$extraSunDamage(float amount) {
        if ((Object) this instanceof ServerPlayer player
                && amount < 1000.0F
                && ShapeshifterAbilities.isSunVulnerable(player)) {
            return amount * ShapeshifterAbilities.SUN_DAMAGE_MULTIPLIER;
        }
        return amount;
    }
}
