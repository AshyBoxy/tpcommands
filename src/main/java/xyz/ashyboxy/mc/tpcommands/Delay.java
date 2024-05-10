package xyz.ashyboxy.mc.tpcommands;

import java.util.HashMap;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class Delay {
    public long ticks;
    public UUID uuid;
    public Pos pos;
    private HashMap<UUID, Long> cooldowns;
    private long cooldownTime;
    public Component name;

    public Delay(long ticks, UUID uuid, Pos pos, HashMap<UUID, Long> cooldowns, long cooldownTime, Component name) {
        this.ticks = ticks;
        this.uuid = uuid;
        this.pos = pos;
        this.cooldowns = cooldowns;
        this.cooldownTime = cooldownTime;
        this.name = name;
    }

    public boolean teleport(ServerPlayer player) {
        cooldowns.put(uuid, System.currentTimeMillis() + cooldownTime);
        return this.pos.teleport(player);
    }

    public void dec() {
        this.ticks--;
    }
}
