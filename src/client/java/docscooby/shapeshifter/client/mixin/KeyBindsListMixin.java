package docscooby.shapeshifter.client.mixin;

import docscooby.shapeshifter.client.ShapeshifterClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBindsList.class)
public abstract class KeyBindsListMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void shapeshifter$hideKeys(KeyBindsScreen screen, Minecraft mc, CallbackInfo ci) {
        if (ShapeshifterClient.isShapeshifter) return;

        ((KeyBindsList) (Object) this).children().removeIf(entry -> {
            if (entry instanceof KeyBindsList.KeyEntry keyEntry) {
                return ShapeshifterClient.isOurKey(((KeyEntryAccessor) keyEntry).shapeshifter$getKey());
            }
            if (entry instanceof KeyBindsList.CategoryEntry categoryEntry) {
                return ((CategoryEntryAccessor) categoryEntry).shapeshifter$getName().getContents()
                        instanceof TranslatableContents tc
                        && tc.getKey().equals("key.categories.shapeshifter");
            }
            return false;
        });
    }
}