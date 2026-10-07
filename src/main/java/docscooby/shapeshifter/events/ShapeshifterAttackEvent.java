package docscooby.shapeshifter.events;

import com.mojang.authlib.GameProfile;
import docscooby.shapeshifter.GameProfileAccessor;
import docscooby.shapeshifter.ShapeshifterData;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public class ShapeshifterAttackEvent {
    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof ServerPlayer target
                    && player instanceof ServerPlayer attacker
                    && target != attacker) {

                ShapeshifterData attackerData = (ShapeshifterData) attacker;
                if (attackerData.shapeshifter$getShapeshifter()) {
                    // If the victim is currently disguised, steal their real skin, not the one they're wearing
                    GameProfile profile = ((GameProfileAccessor) target).shapeshifter$getOriginalGameProfile();
                    if (profile == null) profile = target.getGameProfile();

                    attackerData.shapeshifter$addProfile(profile);
                }
            }

            return InteractionResult.PASS;
        });
    }
}