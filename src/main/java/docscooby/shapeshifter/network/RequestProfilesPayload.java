package docscooby.shapeshifter.network;

import docscooby.shapeshifter.Shapeshifter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record RequestProfilesPayload() implements CustomPacketPayload {
    public static final Identifier ID = Shapeshifter.id("request_profiles");
    public static final Type<@NotNull RequestProfilesPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull RequestProfilesPayload> CODEC =
            StreamCodec.unit(new RequestProfilesPayload());

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return TYPE;
    }
}
