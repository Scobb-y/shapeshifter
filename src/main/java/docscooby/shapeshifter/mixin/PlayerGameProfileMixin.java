package docscooby.shapeshifter.mixin;

import com.mojang.authlib.GameProfile;
import docscooby.shapeshifter.GameProfileAccessor;
import docscooby.shapeshifter.ShapeshifterData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(Player.class)
public abstract class PlayerGameProfileMixin implements GameProfileAccessor, ShapeshifterData {

    @Mutable
    @Shadow
    @Final
    private GameProfile gameProfile;

    @Unique
    private ArrayList<GameProfile> shapeshifter$shapeshifterProfiles = new ArrayList<>();

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

    @Override
    public void shapeshifter$addProfile(GameProfile profile) {
        if (this.shapeshifter$shapeshifterProfiles == null) {
            this.shapeshifter$shapeshifterProfiles = new ArrayList<>();
        }
        if (!this.shapeshifter$shapeshifterProfiles.contains(profile)) {
            this.shapeshifter$shapeshifterProfiles.add(profile);
        }
    }

    @Override
    public List<GameProfile> shapeshifter$getProfiles() {
        if (this.shapeshifter$shapeshifterProfiles == null) {
            this.shapeshifter$shapeshifterProfiles = new ArrayList<>();
        }
        return this.shapeshifter$shapeshifterProfiles;
    }

    @Unique
    private UUID shapeshifter$selectedTarget;

    @Override
    public void shapeshifter$setSelectedTarget(UUID uuid) { this.shapeshifter$selectedTarget = uuid; }

    @Override
    public UUID shapeshifter$getSelectedTarget() { return this.shapeshifter$selectedTarget; }
}