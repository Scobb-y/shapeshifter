package docscooby.shapeshifter.client.mixin;

import com.mojang.authlib.GameProfile;
import docscooby.shapeshifter.client.PlayerInfoAccessor;
import docscooby.shapeshifter.client.ShapeshifterClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
