package xyz.ashyboxy.mc.tpcommands;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public abstract class NbtUtils {
    // i'm sure these two methods exist somewhere in minecraft but i can't find them
    public static long nbtGetLongOrDefault(String key, CompoundTag tag, long def) {
        if (tag.contains(key, Tag.TAG_LONG))
            return tag.getLong(key);
        return def;
    }

    public static boolean nbtGetBooleanOrDefault(String key, CompoundTag tag, boolean def) {
        if (tag.contains(key, Tag.TAG_BYTE))
            return tag.getBoolean(key);
        return def;
    }
}
