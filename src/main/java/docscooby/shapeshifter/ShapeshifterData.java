package docscooby.shapeshifter;

import com.mojang.authlib.GameProfile;

import java.util.List;

public interface ShapeshifterData {

    boolean shapeshifter$getShapeshifter();
    boolean shapeshifter$setShapeshifter();
    void shapeshifter$addProfile(GameProfile player);
    List<GameProfile> shapeshifter$getProfiles();
}
