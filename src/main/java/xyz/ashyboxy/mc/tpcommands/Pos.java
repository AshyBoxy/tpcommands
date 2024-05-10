package xyz.ashyboxy.mc.tpcommands;

import java.util.Collections;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public record Pos(Vec3 pos, Vec2 rot, ResourceKey<Level> dimension) {
    public boolean teleport(ServerPlayer player) {
        return player.teleportTo(player.getServer().getLevel(this.dimension()), this.pos().x, this.pos().y,
                this.pos().z, Collections.emptySet(), this.rot().y, this.rot().x);
    }
}
