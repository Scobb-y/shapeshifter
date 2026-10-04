package docscooby.shapeshifter.client.mixin;

import docscooby.shapeshifter.client.PlayerInfoAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin implements PlayerInfoAccessor {

    @Shadow
    private PlayerInfo playerInfo;

    @Override
    public void shapeshifter$refreshPlayerInfo() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            this.playerInfo = connection.getPlayerInfo(((Entity) (Object) this).getUUID());
        }
    }
}
