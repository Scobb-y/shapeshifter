package docscooby.shapeshifter;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ShapeshifterCommands {
    public static int assignShapeshift(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");

        ShapeshifterData data = (ShapeshifterData) player;
        boolean result = data.shapeshifter$setShapeshifter();
        Shapeshifter.syncStatus(player, result);

        if (result) {
            player.sendSystemMessage(Component.literal(player.getName().getString() + " set to Shapeshifter"));
            return 1;
        } else {
            ShapeshifterDisguiseManager.clearDisguise(player);
            player.sendSystemMessage(Component.literal(player.getName().getString() + " set to not Shapeshifter"));
            return 0;
        }
    }
}