package docscooby.shapeshifter;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import docscooby.shapeshifter.events.ShapeshifterAttackEvent;
import docscooby.shapeshifter.mixin.PlayerGameProfileMixin;
import docscooby.shapeshifter.network.ApplyRagePayload;
import docscooby.shapeshifter.network.ProfilesListPayload;
import docscooby.shapeshifter.network.RequestProfilesPayload;
import docscooby.shapeshifter.network.SelectProfilePayload;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;

public class Shapeshifter implements ModInitializer {
	public static final String MOD_ID = "shapeshifter";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
        ShapeshifterAttackEvent.register();

        PayloadTypeRegistry.serverboundPlay().register(RequestProfilesPayload.TYPE, RequestProfilesPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ProfilesListPayload.TYPE, ProfilesListPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SelectProfilePayload.TYPE, SelectProfilePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ApplyRagePayload.TYPE, ApplyRagePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestProfilesPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ShapeshifterData data = (ShapeshifterData) player;

            if (!data.shapeshifter$getShapeshifter()) return;

            List<String> names = data.shapeshifter$getShapeshifter()
                    ? data.shapeshifter$getProfiles().stream().map(GameProfile::name).toList()
                    : List.of();

            ServerPlayNetworking.send(player, new ProfilesListPayload(names));
        });

        ServerPlayNetworking.registerGlobalReceiver(SelectProfilePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ShapeshifterData data = (ShapeshifterData) player;

            data.shapeshifter$getProfiles().stream()
                    .filter(profile -> profile.name().equals(payload.name()))
                    .findFirst()
                    .ifPresent(profile -> ShapeshifterDisguiseManager.disguise(player, profile));
        });

        ServerPlayNetworking.registerGlobalReceiver(ApplyRagePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ShapeshifterData data = (ShapeshifterData) player;

            if (!data.shapeshifter$getShapeshifter()) return;

            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 90, 0));
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 90, 0));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 30, 1));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("ss")
                    .then(Commands.argument("value", StringArgumentType.string())
                            .executes(ShapeshifterCommands::assignShapeshift)));
        });

		LOGGER.info("Good luck :)");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
