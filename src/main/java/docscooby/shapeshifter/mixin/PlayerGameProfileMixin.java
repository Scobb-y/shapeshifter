package docscooby.shapeshifter.mixin;

import com.mojang.authlib.GameProfile;
import docscooby.shapeshifter.GameProfileAccessor;
import docscooby.shapeshifter.ProfileUtil;
import docscooby.shapeshifter.ShapeshifterData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    private ArrayList<GameProfile> shapeshifter$profiles = new ArrayList<>();

    @Unique
    private GameProfile shapeshifter$originalProfile;

    @Unique
    private boolean shapeshifter$isShapeshifter = false;

    @Unique
    private UUID shapeshifter$selectedTarget;


    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void shapeshifter$save(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean("ShapeshifterFlag", this.shapeshifter$isShapeshifter);

        ListTag list = new ListTag();
        for (GameProfile profile : this.shapeshifter$profiles) {
            list.add(NbtUtils.writeGameProfile(new CompoundTag(), profile));
        }
        tag.put("ShapeshifterProfiles", list);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void shapeshifter$load(CompoundTag tag, CallbackInfo ci) {
        this.shapeshifter$isShapeshifter = tag.getBoolean("ShapeshifterFlag");

        this.shapeshifter$profiles.clear();
        for (Tag entry : tag.getList("ShapeshifterProfiles", Tag.TAG_COMPOUND)) {
            GameProfile profile = NbtUtils.readGameProfile((CompoundTag) entry);
            if (profile != null) this.shapeshifter$profiles.add(profile);
        }
    }

    // ---- ShapeshifterData ----

    @Override
    public boolean shapeshifter$getShapeshifter() {
        return this.shapeshifter$isShapeshifter;
    }

    @Override
    public void shapeshifter$setShapeshifter(boolean value) {
        this.shapeshifter$isShapeshifter = value;
    }

    @Override
    public void shapeshifter$addProfile(GameProfile profile) {
        GameProfile copy = ProfileUtil.copy(profile);

        // Replace an existing entry for the same player so a changed skin is picked up
        for (int i = 0; i < this.shapeshifter$profiles.size(); i++) {
            if (this.shapeshifter$profiles.get(i).getId().equals(copy.getId())) {
                this.shapeshifter$profiles.set(i, copy);
                return;
            }
        }
        this.shapeshifter$profiles.add(copy);
    }

    @Override
    public List<GameProfile> shapeshifter$getProfiles() {
        return this.shapeshifter$profiles;
    }

    @Override
    public void shapeshifter$setSelectedTarget(UUID uuid) {
        this.shapeshifter$selectedTarget = uuid;
    }

    @Override
    public UUID shapeshifter$getSelectedTarget() {
        return this.shapeshifter$selectedTarget;
    }

    // ---- GameProfileAccessor ----

    @Override
    public void shapeshifter$setGameProfile(GameProfile profile) {
        if (this.shapeshifter$originalProfile == null) {
            // Store a real copy, not a reference to the live profile
            this.shapeshifter$originalProfile = ProfileUtil.copy(this.gameProfile);
        }
        this.gameProfile = profile;
    }

    @Override
    public GameProfile shapeshifter$getOriginalGameProfile() {
        return this.shapeshifter$originalProfile;
    }
}