package docscooby.shapeshifter.events;

import docscooby.shapeshifter.ShapeshifterData;
import docscooby.shapeshifter.ShapeshifterDisguiseManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import com.mojang.authlib.GameProfile;

public class ShapeshifterAttackEvent {
    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitresult) -> {
            if (entity instanceof ServerPlayer target) {
                if (((ShapeshifterData) player).shapeshifter$getShapeshifter()) {
                    GameProfile profile = target.getGameProfile();

                }
            }

            return InteractionResult.PASS;
        });
    }
}
