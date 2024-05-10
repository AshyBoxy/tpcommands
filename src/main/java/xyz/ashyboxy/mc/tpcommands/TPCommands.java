package xyz.ashyboxy.mc.tpcommands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class TPCommands implements ModInitializer {
	public static final String MOD_ID = "tpcommands";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static HashMap<UUID, Long> homeCooldowns = new HashMap<>();
	public static HashMap<UUID, Long> spawnCooldowns = new HashMap<>();
	public static ArrayList<Delay> delays = new ArrayList<>();

	@Override
	public void onInitialize() {
		LOGGER.info("Konnichiwa~");
		CommandRegistrationCallback.EVENT.register((dispatcher, context, env) -> {
			// spawn
			dispatcher.register(Commands.literal("spawn").executes(c -> {
				MinecraftServer server = c.getSource().getServer();
				ServerLevel level = c.getSource().getLevel();
				ServerPlayer player = c.getSource().getPlayer();
				Save save = Save.getServerData(server);

				if (player == null)
					return 2;

				if (spawnCooldowns.getOrDefault(player.getUUID(), 0L) > System.currentTimeMillis()) {
					double untilSeconds = (spawnCooldowns.get(player.getUUID()) - System.currentTimeMillis()) / 1000;
					double untilMins = Math.floor(untilSeconds / 60);
					untilSeconds = Math.floor(untilSeconds % 60);
					player.sendSystemMessage(untilMins > 0
							? Component.translatableWithFallback("tpcommands.teleport.cooldown.minutes",
									"Your %s is on cooldown for %s minutes %s seconds", "/spawn",
									(int) untilMins, (int) untilSeconds).withStyle(ChatFormatting.GOLD)
							: Component.translatableWithFallback("tpcommands.teleport.cooldown.seconds",
									"Your %s is on cooldown for %s seconds", "/spawn", (int) untilSeconds)
									.withStyle(ChatFormatting.GOLD));
					return 1;
				}

				Vec3 spawn = level.getSharedSpawnPos().getCenter();

				delays.add(new Delay(save.spawnDelayTicks, player.getUUID(),
						new Pos(new Vec3(spawn.x, level.getSharedSpawnPos().getY(), spawn.z),
								new Vec2(0f, level.getSharedSpawnAngle()), Level.OVERWORLD),
						spawnCooldowns, save.spawnCooldownTime,
						Component.translatableWithFallback("tpcommands.destination.spawn", "spawn")));

				return 0;
			}));

			// home
			dispatcher.register(Commands.literal("home").executes(c -> {
				MinecraftServer server = c.getSource().getServer();
				ServerPlayer player = c.getSource().getPlayer();
				Save save = Save.getServerData(server);

				if (player == null)
					return 2;

				if ((save.shareCooldowns ? spawnCooldowns
						: homeCooldowns).getOrDefault(player.getUUID(), 0L) > System.currentTimeMillis()) {
					double untilSeconds = ((save.shareCooldowns ? spawnCooldowns
							: homeCooldowns).get(player.getUUID()) - System.currentTimeMillis()) / 1000;
					double untilMins = Math.floor(untilSeconds / 60);
					untilSeconds = Math.floor(untilSeconds % 60);
					player.sendSystemMessage(untilMins > 0
							? Component.translatableWithFallback("tpcommands.teleport.cooldown.minutes",
									"Your %s is on cooldown for %s minutes %s seconds", "/home",
									(int) untilMins, (int) untilSeconds).withStyle(ChatFormatting.GOLD)
							: Component.translatableWithFallback("tpcommands.teleport.cooldown.seconds",
									"Your %s is on cooldown for %s seconds", "/home", (int) untilSeconds)
									.withStyle(ChatFormatting.GOLD));
					return 1;
				}

				Pos pos = Save.getPlayerHomePos(player);
				if (pos.rot() == Vec2.MAX) {
					player.sendSystemMessage(Component.translatableWithFallback("tpcommands.teleport.home.unset",
							"You haven't set a home yet! (Use /sethome)").withStyle(ChatFormatting.RED));
					return 3;
				}

				delays.add(new Delay(save.homeDelayTicks, player.getUUID(), pos,
						save.shareCooldowns ? spawnCooldowns : homeCooldowns, save.homeCooldownTime,
						Component.translatableWithFallback("tpcommands.destination.home", "home")));

				return 0;
			}));

			// sethome
			dispatcher.register(Commands.literal("sethome").executes(c -> {
				MinecraftServer server = c.getSource().getServer();
				ServerPlayer player = c.getSource().getPlayer();

				if (player == null)
					return 2;

				Pos pos = new Pos(player.position(), player.getRotationVector(),
						player.level().dimension());

				Save.getServerData(server).homes.put(player.getUUID(), pos);

				player.sendSystemMessage(
						Component.translatableWithFallback("tpcommands.sethome", "Set your home to %s %s %s",
								(int) pos.pos().x, (int) pos.pos().y, (int) pos.pos().z)
								.withStyle(ChatFormatting.GREEN));

				return 0;
			}));

			// tpconfig
			// i hate this
			dispatcher.register(Commands.literal("tpconfig")
					.requires(env.includeIntegrated ? (source) -> true : (source) -> source.hasPermission(2))
					.executes(c -> {
						Save save = Save.getServerData(c.getSource().getServer());
						c.getSource().sendSystemMessage(Component.literal((String.format(
								"homeCooldownTime: %s\nspawnCooldownTime: %s\nhomeDelayTicks: %s\nspawnDelayTicks: %s\nshareCooldowns: %s",
								save.homeCooldownTime / 1000, save.spawnCooldownTime / 1000, save.homeDelayTicks,
								save.spawnDelayTicks,
								save.shareCooldowns))));
						return 0;
					})
					.then(Commands.literal("homeCooldownTime")
							.then(Commands.argument("time", LongArgumentType.longArg(0)).executes(c -> {
								Save save = Save.getServerData(c.getSource().getServer());
								long newTime = LongArgumentType.getLong(c, "time");
								save.homeCooldownTime = newTime * 1000;
								c.getSource().sendSuccess(
										() -> Component
												.literal("Changed homeCooldownTime to " + save.homeCooldownTime / 1000),
										true);
								return 0;
							})))
					.then(Commands.literal("spawnCooldownTime")
							.then(Commands.argument("time", LongArgumentType.longArg(0)).executes(c -> {
								Save save = Save.getServerData(c.getSource().getServer());
								long newTime = LongArgumentType.getLong(c, "time");
								save.spawnCooldownTime = newTime * 1000;
								c.getSource().sendSuccess(
										() -> Component
												.literal("Changed spawnCooldownTime to "
														+ save.spawnCooldownTime / 1000),
										true);
								return 0;
							})))
					.then(Commands.literal("homeDelayTicks")
							.then(Commands.argument("ticks", LongArgumentType.longArg(0)).executes(c -> {
								Save save = Save.getServerData(c.getSource().getServer());
								save.homeDelayTicks = LongArgumentType.getLong(c, "ticks");
								c.getSource().sendSuccess(
										() -> Component
												.literal("Changed homeDelayTicks to " + save.homeDelayTicks),
										true);
								return 0;
							})))
					.then(Commands.literal("spawnDelayTicks")
							.then(Commands.argument("ticks", LongArgumentType.longArg(0)).executes(c -> {
								Save save = Save.getServerData(c.getSource().getServer());
								save.spawnDelayTicks = LongArgumentType.getLong(c, "ticks");
								c.getSource().sendSuccess(
										() -> Component
												.literal("Changed spawnDelayTicks to " + save.spawnDelayTicks),
										true);
								return 0;
							})))
					.then(Commands.literal("shareCooldowns")
							.then(Commands.argument("share", BoolArgumentType.bool()).executes(c -> {
								Save save = Save.getServerData(c.getSource().getServer());
								save.shareCooldowns = BoolArgumentType.getBool(c, "share");
								c.getSource().sendSuccess(
										() -> Component
												.literal("Changed shareCooldowns to " + save.shareCooldowns),
										true);
								return 0;
							}))));
		});
	}
}
