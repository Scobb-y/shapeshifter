package docscooby.shapeshifter;

import com.mojang.authlib.GameProfile;

import java.util.List;
import java.util.UUID;

public interface ShapeshifterData {

    boolean shapeshifter$getShapeshifter();
    void shapeshifter$setShapeshifter(boolean value);
    void shapeshifter$addProfile(GameProfile profile);
    List<GameProfile> shapeshifter$getProfiles();
    void shapeshifter$setSelectedTarget(UUID uuid);
    UUID shapeshifter$getSelectedTarget();
}