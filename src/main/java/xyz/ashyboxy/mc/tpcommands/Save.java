package xyz.ashyboxy.mc.tpcommands;

import java.util.HashMap;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class Save extends SavedData {
    public HashMap<UUID, Pos> homes = new HashMap<>();
    public long homeCooldownTime = Defaults.homeCooldownTime;
    public long spawnCooldownTime = Defaults.spawnCooldownTime;
    public long homeDelayTicks = Defaults.homeDelayTicks;
    public long spawnDelayTicks = Defaults.spawnDelayTicks;
    public boolean shareCooldowns = Defaults.shareCooldowns;

    private static Factory<Save> factory = new Factory<>(Save::new, Save::createFromNbt, null);

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        CompoundTag playerHomes = new CompoundTag();
        homes.forEach((uuid, homePos) -> {
            CompoundTag player = new CompoundTag();
            ListTag pos = new ListTag();
            pos.add(DoubleTag.valueOf(homePos.pos().x));
            pos.add(DoubleTag.valueOf(homePos.pos().y));
            pos.add(DoubleTag.valueOf(homePos.pos().z));
            player.put("pos", pos);
            ListTag rot = new ListTag();
            rot.add(FloatTag.valueOf(homePos.rot().x));
            rot.add(FloatTag.valueOf(homePos.rot().y));
            player.put("rot", rot);
            player.put("dimension", StringTag.valueOf(homePos.dimension().location().toString()));

            playerHomes.put(uuid.toString(), player);
        });
        nbt.put("homes", playerHomes);

        nbt.putLong("homeCooldownTime", homeCooldownTime);
        nbt.putLong("spawnCooldownTime", spawnCooldownTime);
        nbt.putLong("homeDelayTicks", homeDelayTicks);
        nbt.putLong("spawnDelayTicks", spawnDelayTicks);
        nbt.putBoolean("shareCooldowns", shareCooldowns);

        return nbt;
    }

    public static Save createFromNbt(CompoundTag nbt, HolderLookup.Provider provider) {
        Save save = new Save();

        CompoundTag playerHomes = nbt.getCompound("homes");
        playerHomes.getAllKeys().forEach(key -> {
            CompoundTag player = playerHomes.getCompound(key);

            ListTag pos = player.getList("pos", Tag.TAG_DOUBLE);
            ListTag rot = player.getList("rot", Tag.TAG_FLOAT);

            Pos pos2 = new Pos(new Vec3(pos.getDouble(0), pos.getDouble(1), pos.getDouble(2)),
                    new Vec2(rot.getFloat(0), rot.getFloat(1)),
                    provider.lookup(Registries.DIMENSION).get().listElementIds()
                            .filter(d -> d.location().equals(ResourceLocation.tryParse(player.getString("dimension"))))
                            .findAny().orElse(null));
            save.homes.put(UUID.fromString(key), pos2);
        });

        save.homeCooldownTime = NbtUtils.nbtGetLongOrDefault("homeCooldownTime", nbt, Defaults.homeCooldownTime);
        save.spawnCooldownTime = NbtUtils.nbtGetLongOrDefault("spawnCooldownTime", nbt, Defaults.spawnCooldownTime);
        save.homeDelayTicks = NbtUtils.nbtGetLongOrDefault("homeDelayTicks", nbt, Defaults.homeDelayTicks);
        save.spawnDelayTicks = NbtUtils.nbtGetLongOrDefault("spawnDelayTicks", nbt, Defaults.spawnDelayTicks);
        save.shareCooldowns = NbtUtils.nbtGetBooleanOrDefault("shareCooldowns", nbt, Defaults.shareCooldowns);

        return save;
    }

    public static Save getServerData(MinecraftServer server) {
        DimensionDataStorage dataStorage = server.getLevel(Level.OVERWORLD).getDataStorage();
        Save save = dataStorage.computeIfAbsent(factory, TPCommands.MOD_ID);
        save.setDirty();
        return save;
    }

    public static Pos getPlayerHomePos(ServerPlayer player) {
        Save save = getServerData(player.getServer());
        // Vec2.MAX here is used to represent an invalid rotation, so /home can tell
        // that the player hasn't run /sethome without needing to keep track of that
        // separately
        // actual rotations the player could get their camera at are clamped, so this
        // seems reasonable
        Pos pos = save.homes.computeIfAbsent(player.getUUID(),
                uuid -> new Pos(Vec3.ZERO, Vec2.MAX, Level.OVERWORLD));
        return pos;
    }
}
