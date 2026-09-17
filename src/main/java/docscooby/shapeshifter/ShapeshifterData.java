package docscooby.shapeshifter;

import com.mojang.authlib.GameProfile;

import java.util.ArrayList;
import java.util.List;

public interface ShapeshifterData {
    boolean shapeshifter$isShapeshifter();
    void shapeshifter$setShapeshifter(boolean value);
    void shapeshifter$addSkin(GameProfile player);
    void shapeshifter$setSkin(GameProfile player);
}
