package docscooby.shapeshifter.client.mixin;

import docscooby.shapeshifter.ShapeshifterData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public class PlayerEntityMixin implements ShapeshifterData {

    private boolean shapeshifter = false;

    @Override
    public boolean isShapeshifter() {
        return shapeshifter;
    }

    @Override
    public void setShapeshifter(boolean value) {
        shapeshifter = value;
    }
}
