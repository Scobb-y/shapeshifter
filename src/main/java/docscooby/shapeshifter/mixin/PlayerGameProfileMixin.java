package docscooby.shapeshifter.mixin;

import com.mojang.authlib.GameProfile;
import docscooby.shapeshifter.GameProfileAccessor;
import docscooby.shapeshifter.ShapeshifterData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;

@Mixin(Player.class)
public abstract class PlayerGameProfileMixin implements GameProfileAccessor, ShapeshifterData {

    @Mutable
    @Shadow
    @Final
    private GameProfile gameProfile;

    @Unique
    private GameProfile shapeshifter$originalProfile;

    @Unique
    private Boolean shapeshifter = false;

    @Override
    public boolean shapeshifter$setShapeshifter() {
        this.shapeshifter = !this.shapeshifter;
        return this.shapeshifter;
    }

    @Override
    public boolean shapeshifter$getShapeshifter() {
        return this.shapeshifter;
    }

    @Override
    public void shapeshifter$setGameProfile(GameProfile profile) {
        if (this.shapeshifter$originalProfile == null) {
            this.shapeshifter$originalProfile = this.gameProfile;
        }
        this.gameProfile = profile;
    }

    @Override
    public GameProfile shapeshifter$getOriginalGameProfile() {
        return this.shapeshifter$originalProfile;
    }
}