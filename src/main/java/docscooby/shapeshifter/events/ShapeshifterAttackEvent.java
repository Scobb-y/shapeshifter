package docscooby.shapeshifter.events;

import docscooby.shapeshifter.ShapeshifterData;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public class ShapeshifterAttackEvent {
    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitresult) -> {
            if (entity instanceof ServerPlayer target) {
                if (((ShapeshifterData) player).shapeshifter$isShapeshifter()) {
                    System.out.println(
                            player.getName().getString() + " hit " +
                                    target.getName().getString()
                    );
                }
            }

            return InteractionResult.PASS;
        });
    }
}
