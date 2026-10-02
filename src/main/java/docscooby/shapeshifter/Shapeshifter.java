package docscooby.shapeshifter;

import com.mojang.authlib.GameProfile;
import docscooby.shapeshifter.events.ShapeshifterAttackEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static docscooby.shapeshifter.ShapeshifterDisguiseManager.sendScopedGlow;

public class Shapeshifter implements ModInitializer {
    public static final String MOD_ID = "shapeshifter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ResourceLocation SELF_SKIN_UPDATE = id("self_skin_update");
    public static final ResourceLocation REQUEST_PROFILES = id("request_profiles");
    public static final ResourceLocation PROFILES_LIST = id("profiles_list");
    public static final ResourceLocation SELECT_PROFILE = id("select_profile");
    public static final ResourceLocation APPLY_RAGE = id("apply_rage");
    public static final ResourceLocation HUNT = id("hunt");
    public static final ResourceLocation SELECT_TARGET = id("select_target");
    public static final ResourceLocation TOGGLE_RECORDING = id("toggle_recording");
    public static final ResourceLocation PLAY_RECORDING = id("play_recording");

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

        ServerPlayNetworking.registerGlobalReceiver(HUNT, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ShapeshifterData data = (ShapeshifterData) player;
                if (!data.shapeshifter$getShapeshifter()) return;

                for (ServerPlayer nearby : server.getPlayerList().getPlayers()) {
                    if (nearby != player && player.distanceTo(nearby) < 50) {
                        sendScopedGlow(server, player, nearby);
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SELECT_TARGET, (server, player, handler, buf, responseSender) -> {
            UUID target = buf.readUUID();
            server.execute(() -> ((ShapeshifterData) player).shapeshifter$setSelectedTarget(target));
        });

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_RECORDING, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ShapeshifterData data = (ShapeshifterData) player;
                UUID target = data.shapeshifter$getSelectedTarget();
                if (target == null) return;

                if (ShapeshifterVoiceManager.isRecording(target)) {
                    ShapeshifterVoiceManager.stopRecording(target);
                    player.sendSystemMessage(Component.literal("Stopped recording."));
                } else {
                    ShapeshifterVoiceManager.startRecording(target);
                    player.sendSystemMessage(Component.literal("Recording started."));
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(PLAY_RECORDING, (server, player, handler, buf, responseSender) -> {
            String name = buf.readUtf();
            server.execute(() -> {
                ShapeshifterData data = (ShapeshifterData) player;
                data.shapeshifter$getProfiles().stream()
                        .filter(p -> p.getName().equals(name))
                        .findFirst()
                        .ifPresent(profile -> {
                            List<byte[]> recording = ShapeshifterVoiceManager.getRecording(profile.getId());
                            if (!recording.isEmpty()) {
                                ShapeshifterVoiceManager.playback(ShapeshifterVoicePlugin.getApi(), player, recording);
                            }
                        });
            });
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("ss")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(ShapeshifterCommands::assignShapeshift)));
        });

        LOGGER.info("Good luck :)");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}