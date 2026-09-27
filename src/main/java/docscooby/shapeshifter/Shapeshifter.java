package docscooby.shapeshifter;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import docscooby.shapeshifter.events.ShapeshifterAttackEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Shapeshifter implements ModInitializer {
    public static final String MOD_ID = "shapeshifter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ResourceLocation SELF_SKIN_UPDATE = id("self_skin_update");
    public static final ResourceLocation REQUEST_PROFILES = id("request_profiles");
    public static final ResourceLocation PROFILES_LIST = id("profiles_list");
    public static final ResourceLocation SELECT_PROFILE = id("select_profile");
    public static final ResourceLocation APPLY_RAGE = id("apply_rage");

    @Override
    public void onInitialize() {
        ShapeshifterAttackEvent.register();

        ServerPlayNetworking.registerGlobalReceiver(REQUEST_PROFILES, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ShapeshifterData data = (ShapeshifterData) player;
                if (!data.shapeshifter$getShapeshifter()) return;

                List<String> names = data.shapeshifter$getProfiles().stream()
                        .map(GameProfile::getName).toList();

                FriendlyByteBuf out = PacketByteBufs.create();
                out.writeCollection(names, FriendlyByteBuf::writeUtf);
                ServerPlayNetworking.send(player, PROFILES_LIST, out);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SELECT_PROFILE, (server, player, handler, buf, responseSender) -> {
            String name = buf.readUtf();
            server.execute(() -> {
                ShapeshifterData data = (ShapeshifterData) player;
                data.shapeshifter$getProfiles().stream()
                        .filter(profile -> profile.getName().equals(name))
                        .findFirst()
                        .ifPresent(profile -> ShapeshifterDisguiseManager.disguise(player, profile));
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(APPLY_RAGE, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ShapeshifterData data = (ShapeshifterData) player;
                if (!data.shapeshifter$getShapeshifter()) return;

                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 30, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 30, 0));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1));
            });
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("ss")
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(ShapeshifterCommands::assignShapeshift)));
        });

        LOGGER.info("Good luck :)");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}