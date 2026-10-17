package docscooby.shapeshifter;

import com.mojang.authlib.GameProfile;
import docscooby.shapeshifter.events.ShapeshifterAttackEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class Shapeshifter implements ModInitializer {
    public static final String MOD_ID = "shapeshifter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final double MAX_SELECT_DISTANCE = 12.0;

    public static final ResourceLocation SELF_SKIN_UPDATE = id("self_skin_update");
    public static final ResourceLocation REQUEST_PROFILES = id("request_profiles");
    public static final ResourceLocation PROFILES_LIST = id("profiles_list");
    public static final ResourceLocation SELECT_PROFILE = id("select_profile");
    public static final ResourceLocation APPLY_RAGE = id("apply_rage");
    public static final ResourceLocation HUNT = id("hunt");
    public static final ResourceLocation SELECT_TARGET = id("select_target");
    public static final ResourceLocation SET_RECORDING = id("set_recording");       // client -> server (boolean start)
    public static final ResourceLocation RECORDING_STATE = id("recording_state");   // server -> client (boolean active)
    public static final ResourceLocation PLAY_RECORDING = id("play_recording");
    public static final ResourceLocation SHAPESHIFTER_STATUS = id("status");
    public static final ResourceLocation PLAY_DISGUISE_RECORDING = id("play_disguise_recording"); // client -> server (slot)
    public static final ResourceLocation DISGUISE_OVERLAY = id("disguise_overlay");
    public static final ResourceLocation RAGE_STATE = id("rage_state");
    public static final ResourceLocation GLOW_PLAYERS = id("glow_players");

    @Override
    public void onInitialize() {
        ShapeshifterAttackEvent.register();

        ServerPlayNetworking.registerGlobalReceiver(REQUEST_PROFILES, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                if (!isShapeshifter(player)) return;

                List<String> names = ((ShapeshifterData) player).shapeshifter$getProfiles().stream()
                        .map(GameProfile::getName).toList();

                FriendlyByteBuf out = PacketByteBufs.create();
                out.writeCollection(names, FriendlyByteBuf::writeUtf);
                ServerPlayNetworking.send(player, PROFILES_LIST, out);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SELECT_PROFILE, (server, player, handler, buf, responseSender) -> {
            String name = buf.readUtf();
            server.execute(() -> {
                if (!isShapeshifter(player)) return;
                ShapeshifterData data = (ShapeshifterData) player;

                if (name.equals(player.getGameProfile().getName())) {
                    ShapeshifterDisguiseManager.clearDisguise(player);
                    return;
                }

                data.shapeshifter$getProfiles().stream()
                        .filter(profile -> profile.getName().equals(name))
                        .findFirst()
                        .ifPresent(profile -> ShapeshifterDisguiseManager.disguise(player, profile));
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(APPLY_RAGE, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                if (!isShapeshifter(player)) return;
                ShapeshifterAbilities.rage(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(HUNT, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                if (!isShapeshifter(player)) return;
                ShapeshifterAbilities.hunt(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SELECT_TARGET, (server, player, handler, buf, responseSender) -> {
            UUID targetId = buf.readUUID();
            server.execute(() -> {
                if (!isShapeshifter(player)) return;
                ShapeshifterData data = (ShapeshifterData) player;

                ServerPlayer victim = server.getPlayerList().getPlayer(targetId);
                if (victim == null || victim == player || player.distanceTo(victim) > MAX_SELECT_DISTANCE) {
                    data.shapeshifter$setSelectedTarget(null);
                    return;
                }
                data.shapeshifter$setSelectedTarget(targetId);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SET_RECORDING, (server, player, handler, buf, responseSender) -> {
            boolean start = buf.readBoolean();
            server.execute(() -> {
                if (!isShapeshifter(player)) {
                    ShapeshifterVoiceManager.stopRecordingBy(player.getUUID());
                    sendRecordingState(player, false);
                    return;
                }

                if (start) {
                    UUID target = ((ShapeshifterData) player).shapeshifter$getSelectedTarget();
                    if (target == null) {
                        sendRecordingState(player, false);
                        return;
                    }

                    boolean started = ShapeshifterVoiceManager.startRecording(player.getUUID(), target);
                    player.sendSystemMessage(Component.literal(
                            started ? "Recording started." : "That player is already being recorded."));
                    sendRecordingState(player, started);
                } else {
                    if (ShapeshifterVoiceManager.stopRecordingBy(player.getUUID())) {
                        player.sendSystemMessage(Component.literal("Stopped recording."));
                    }
                    sendRecordingState(player, false);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(PLAY_RECORDING, (server, player, handler, buf, responseSender) -> {
            String name = buf.readUtf();
            server.execute(() -> {
                if (!isShapeshifter(player)) return;

                ((ShapeshifterData) player).shapeshifter$getProfiles().stream()
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

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            syncStatus(player, isShapeshifter(player));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();

            ShapeshifterVoiceManager.stopRecordingBy(player.getUUID());

            UUID recorderId = ShapeshifterVoiceManager.stopRecordingOf(player.getUUID());
            if (recorderId != null) {
                ServerPlayer recorder = server.getPlayerList().getPlayer(recorderId);
                if (recorder != null) sendRecordingState(recorder, false);
            }

            ShapeshifterDisguiseManager.forget(player.getUUID());
            OverlaySync.forget(player.getUUID());

            ShapeshifterAbilities.forget(player.getUUID());
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            ShapeshifterData oldData = (ShapeshifterData) oldPlayer;
            ShapeshifterData newData = (ShapeshifterData) newPlayer;

            newData.shapeshifter$setShapeshifter(oldData.shapeshifter$getShapeshifter());
            newData.shapeshifter$getProfiles().clear();
            oldData.shapeshifter$getProfiles().forEach(newData::shapeshifter$addProfile);

            if (((GameProfileAccessor) oldPlayer).shapeshifter$getOriginalGameProfile() != null) {
                ((GameProfileAccessor) newPlayer).shapeshifter$setGameProfile(
                        ProfileUtil.copy(oldPlayer.getGameProfile()));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(PLAY_DISGUISE_RECORDING, (server, player, handler, buf, responseSender) -> {
            int slot = buf.readVarInt();
            server.execute(() -> {
                if (!isShapeshifter(player)) return;

                ShapeshifterDisguiseManager.DisguiseInfo info = ShapeshifterDisguiseManager.getDisguise(player.getUUID());
                if (info == null) return;

                var api = ShapeshifterVoicePlugin.getApi();
                if (api == null) return;

                List<List<byte[]>> clips = ShapeshifterVoiceManager.getVisibleRecordings(info.id());
                if (slot < 0 || slot >= clips.size()) return;

                ShapeshifterVoiceManager.playback(api, player, clips.get(slot));
            });
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ShapeshifterAbilities.tick(server);
            if (server.getTickCount() % 20 == 0) OverlaySync.tick(server);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                syncStatus(newPlayer, isShapeshifter(newPlayer)));

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

    public static boolean isShapeshifter(ServerPlayer player) {
        return ((ShapeshifterData) player).shapeshifter$getShapeshifter();
    }

    public static void syncStatus(ServerPlayer player, boolean value) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(value);
        ServerPlayNetworking.send(player, SHAPESHIFTER_STATUS, buf);
    }

    public static void sendRecordingState(ServerPlayer player, boolean active) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(active);
        ServerPlayNetworking.send(player, RECORDING_STATE, buf);
    }
}