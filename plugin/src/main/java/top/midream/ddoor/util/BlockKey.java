package top.midream.ddoor.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class BlockKey {

    private BlockKey() {}

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    public static long pack(Block block) {
        return pack(block.getX(), block.getY(), block.getZ());
    }

    public static World world(String name) {
        return Bukkit.getWorld(name);
    }
}
