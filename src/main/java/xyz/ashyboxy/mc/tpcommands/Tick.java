package xyz.ashyboxy.mc.tpcommands;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public abstract class Tick {
    public static void handleTick(MinecraftServer server) {
        TPCommands.delays.removeIf(d -> {
            ServerPlayer player = server.getPlayerList().getPlayer(d.uuid);
            long ticks = d.ticks;
            d.dec();
            if (player == null)
                return !(d.ticks > 0);

            // we want the current ticks left, after the dec ticks is minus the current one
            int seconds = (int) Math.floor(ticks / 20);

            if (ticks % 20 == 0 && ticks > 0) {
                player.sendSystemMessage(Component.translatableWithFallback("tpcommands.teleport.countdown",
                        "Teleporting to %s in %s...", d.name, seconds).withStyle(ChatFormatting.YELLOW));
            }

            d.dec();

            if (d.ticks > 0)
                return false;
            player.sendSystemMessage(Component.translatableWithFallback("tpcommands.teleport.final", "Teleporting...")
                    .withStyle(ChatFormatting.GREEN));
            d.teleport(player);
            return true;
        });
    }
}
