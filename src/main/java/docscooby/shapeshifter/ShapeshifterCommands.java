package docscooby.shapeshifter;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ShapeshifterCommands {
    public static int assignShapeshift(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        ShapeshifterData data = (ShapeshifterData) target;

        boolean newValue = !data.shapeshifter$getShapeshifter();
        data.shapeshifter$setShapeshifter(newValue);
        Shapeshifter.syncStatus(target, newValue);

        if (!newValue) {
            ShapeshifterDisguiseManager.clearDisguise(target);
            if (ShapeshifterVoiceManager.stopRecordingBy(target.getUUID())) {
                Shapeshifter.sendRecordingState(target, false);
            }
        }

        String name = target.getGameProfile().getName();
        String adminMessage = name + (newValue ? " is now a Shapeshifter" : " is no longer a Shapeshifter");

        context.getSource().sendSuccess(() -> Component.literal(adminMessage), false);

        if (context.getSource().getEntity() != target) {
            target.sendSystemMessage(Component.literal(
                    newValue ? "You are now a Shapeshifter." : "You are no longer a Shapeshifter."));
        }

        return newValue ? 1 : 0;
    }
}