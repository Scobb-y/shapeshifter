package docscooby.shapeshifter.network;

import docscooby.shapeshifter.Shapeshifter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record SelectProfilePayload(String name) implements CustomPacketPayload {
    public static final Identifier ID = Shapeshifter.id("select_profile");
    public static final Type<@NotNull SelectProfilePayload> TYPE = new Type<>(ID);
    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull SelectProfilePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SelectProfilePayload::name,
                    SelectProfilePayload::new
            );

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return TYPE;
    }
}