package docscooby.shapeshifter.events;

import docscooby.shapeshifter.ShapeshifterData;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;

public class ShapeshifterAttackEvent {
    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitresult) -> {
            if (entity instanceof ServerPlayer target) {
                if (((ShapeshifterData) player).shapeshifter$isShapeshifter()) {
                    GameProfile profile = target.getGameProfile();
                    ((ShapeshifterData) player).shapeshifter$addSkin(profile);
                }
            }

            return InteractionResult.PASS;
        });
    }
}
