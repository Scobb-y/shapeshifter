package docscooby.shapeshifter;

import com.mojang.authlib.GameProfile;

public interface GameProfileAccessor {
    void shapeshifter$setGameProfile(GameProfile profile);
    GameProfile shapeshifter$getOriginalGameProfile();
}