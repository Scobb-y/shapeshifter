package docscooby.shapeshifter.network;

import docscooby.shapeshifter.Shapeshifter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ApplyRagePayload() implements CustomPacketPayload {
    public static final Identifier ID = Shapeshifter.id("apply_rage");
    public static final Type<ApplyRagePayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ApplyRagePayload> CODEC =
            StreamCodec.unit(new ApplyRagePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
