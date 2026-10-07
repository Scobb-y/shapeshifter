package docscooby.shapeshifter;

import com.mojang.authlib.GameProfile;

public final class ProfileUtil {
    private ProfileUtil() {}

    public static GameProfile copy(GameProfile source) {
        GameProfile copy = new GameProfile(source.getId(), source.getName());
        copy.getProperties().putAll(source.getProperties());
        return copy;
    }
}
