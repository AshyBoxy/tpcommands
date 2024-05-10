package xyz.ashyboxy.mc.tpcommands.mixin;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;
import xyz.ashyboxy.mc.tpcommands.Tick;

@Debug(export = true)
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(at = @At("TAIL"), method = "tickServer")
    public void handleTick(CallbackInfo ci) {
        Tick.handleTick((MinecraftServer) (Object) this);
    }
}
