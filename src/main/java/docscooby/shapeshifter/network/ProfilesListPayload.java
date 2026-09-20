package docscooby.shapeshifter.network;

import docscooby.shapeshifter.Shapeshifter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ProfilesListPayload(List<String> names) implements CustomPacketPayload {
    public static final Identifier ID = Shapeshifter.id("profiles_list");
    public static final Type<@NotNull ProfilesListPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull ProfilesListPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ProfilesListPayload::names,
                    ProfilesListPayload::new
            );

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return TYPE;
    }
}
