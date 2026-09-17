package docscooby.shapeshifter.mixin;

import com.mojang.authlib.GameProfile;
import docscooby.shapeshifter.ShapeshifterData;
import docscooby.shapeshifter.ShapeshifterPlayerProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(Player.class)
public class PlayerEntityMixin implements ShapeshifterData {

    @Unique
    private boolean shapeshifter = false;

    @Unique
    private UUID shapeshiftTarget;

    @Unique
    List<ShapeshifterPlayerProfile> stolenSkins = new ArrayList<>();

    @Override
    public boolean shapeshifter$isShapeshifter() {
        return shapeshifter;
    }

    @Override
    public void shapeshifter$setShapeshifter(boolean value) {
        shapeshifter = value;
    }

    @Override
    public void shapeshifter$addSkin(GameProfile player) {
        this.stolenSkins.add(new ShapeshifterPlayerProfile(player));
    }

    @Override
    public void shapeshifter$setSkin(GameProfile player) {
        PlayerSkin skinTextures =
                Minecraft.getInstance()
                        .getSkinManager()
                        .createLookup(player,true).get();
    }

    @Unique
    public UUID shapeshifter$getTarget() {
        return shapeshiftTarget;
    }

    @Unique
    public void shapeshifter$setTarget(UUID target) {
        this.shapeshiftTarget = target;
    }
}
