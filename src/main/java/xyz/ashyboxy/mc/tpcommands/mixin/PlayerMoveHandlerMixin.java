package xyz.ashyboxy.mc.tpcommands.mixin;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import xyz.ashyboxy.mc.tpcommands.TPCommands;

@Debug(export = true)
@Mixin(ServerGamePacketListenerImpl.class)
public class PlayerMoveHandlerMixin {
    @Shadow
    ServerPlayer player;

    @Inject(at = @At("HEAD"), method = "handleMovePlayer")
    public void handleMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (!packet.hasPosition())
            return;
        if ((packet.getX(player.getX()) == player.getX()) && (packet.getY(player.getY()) == player.getY())
                && (packet.getZ(player.getZ()) == player.getZ()))
            return;
        // at this point i'm pretty sure the player's moved
        if (TPCommands.delays.removeIf(d -> d.uuid.equals(player.getUUID())))
            player.sendSystemMessage(Component.translatableWithFallback("tpcommands.teleport.moved",
                    "You cannot move while teleporting").withStyle(ChatFormatting.RED));
    }
}
