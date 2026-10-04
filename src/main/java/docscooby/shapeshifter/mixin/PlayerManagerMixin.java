package docscooby.shapeshifter.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerManagerMixin {

    @Inject(
            method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void shapeshifter$hideJoinLeave(Component message, boolean overlay, CallbackInfo ci) {
        if (message.getContents() instanceof TranslatableContents contents) {
            String key = contents.getKey();
            if (key.equals("multiplayer.player.joined")
                    || key.equals("multiplayer.player.joined.renamed")
                    || key.equals("multiplayer.player.left")) {
                ci.cancel();
            }
        }
    }
}