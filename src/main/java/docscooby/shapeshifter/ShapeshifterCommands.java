package docscooby.shapeshifter;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ShapeshifterCommands {
    public static int assignShapeshift(CommandContext<CommandSourceStack> context) {
        String value = StringArgumentType.getString(context, "value");
        ServerPlayer player = context.getSource()
                .getServer()
                .getPlayerList()
                .getPlayerByName(value);

        if (player == null) {
            context.getSource().sendFailure(
                    Component.literal("Player is not online.")
            );
            return 0;
        }

        ShapeshifterData data = (ShapeshifterData) player;
        boolean result = data.shapeshifter$setShapeshifter();

        if (result) {
            player.sendSystemMessage(Component.literal("Player set to Shapeshifter"));
            return 1;
        } else {
            ShapeshifterDisguiseManager.clearDisguise(player);
            player.sendSystemMessage(Component.literal("Player set to not Shapeshifter"));
            return 0;
        }
    }
}