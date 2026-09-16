package docscooby.shapeshifter.client.mixin;

import docscooby.shapeshifter.ShapeshifterData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public class PlayerEntityMixin implements ShapeshifterData {

    @Unique
    private boolean shapeshifter = false;

    @Override
    public boolean shapeshifter$isShapeshifter() {
        return shapeshifter;
    }

    @Override
    public void shapeshifter$setShapeshifter(boolean value) {
        shapeshifter = value;
    }
}
