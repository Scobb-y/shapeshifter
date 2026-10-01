package docscooby.shapeshifter;

import com.mojang.authlib.GameProfile;

import java.util.List;
import java.util.UUID;

public interface ShapeshifterData {

    boolean shapeshifter$getShapeshifter();
    boolean shapeshifter$setShapeshifter();
    void shapeshifter$addProfile(GameProfile player);
    List<GameProfile> shapeshifter$getProfiles();
    void shapeshifter$setSelectedTarget(UUID uuid);
    UUID shapeshifter$getSelectedTarget();
}
