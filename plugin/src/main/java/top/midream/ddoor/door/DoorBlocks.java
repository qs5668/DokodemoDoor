package top.midream.ddoor.door;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Door;
import top.midream.ddoor.util.BlockKey;

/**
 * Door block identification: anchor resolution (LOWER half), double-door merging,
 * and facing extraction for teleport landing math.
 */
public final class DoorBlocks {

    private static final BlockFace[] HORIZONTAL = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    private DoorBlocks() {}

    public static boolean isDoor(Block block) {
        return block != null && block.getBlockData() instanceof Door;
    }

    /** Is this block a door block whose lower half sits on top of another door block? (floating upper remnant) */
    public static boolean isFloatingUpper(Block block) {
        if (!isDoor(block)) return false;
        Door data = (Door) block.getBlockData();
        if (data.getHalf() != Door.Half.TOP) return false;
        return !isDoor(block.getRelative(BlockFace.DOWN));
    }

    /**
     * Resolve the anchor block (lower half; for double doors the
     * lexicographically smaller of the two lower blocks) of any door block.
     * Returns null when the block is not a door.
     */
    public static Block anchorOf(Block block) {
        if (!isDoor(block)) return null;
        Door data = (Door) block.getBlockData();
        Block lower = data.getHalf() == Door.Half.TOP ? block.getRelative(BlockFace.DOWN) : block;
        if (!isDoor(lower)) return null;
        // double door: neighbouring lower door block of same material
        for (BlockFace face : HORIZONTAL) {
            Block neighbour = lower.getRelative(face);
            if (isDoor(neighbour) && neighbour.getType() == lower.getType()) {
                Door nd = (Door) neighbour.getBlockData();
                if (nd.getHalf() != Door.Half.BOTTOM) continue;
                boolean smaller = neighbour.getX() < lower.getX()
                        || (neighbour.getX() == lower.getX() && neighbour.getZ() < lower.getZ());
                return smaller ? neighbour : lower;
            }
        }
        return lower;
    }

    /**
     * Packed keys of the extra blocks belonging to this anchor
     * (the twin block of a double door). Empty array for single doors.
     */
    public static long[] extraBlocksOf(Block anchor) {
        for (BlockFace face : HORIZONTAL) {
            Block neighbour = anchor.getRelative(face);
            if (isDoor(neighbour) && neighbour.getType() == anchor.getType()) {
                Door nd = (Door) neighbour.getBlockData();
                if (nd.getHalf() == Door.Half.BOTTOM) {
                    return new long[]{BlockKey.pack(neighbour)};
                }
            }
        }
        return new long[0];
    }

    public static BlockFace facingOf(Block anchor) {
        Door data = (Door) anchor.getBlockData();
        return data.getFacing();
    }

    public static String describe(Material material) {
        String name = material.name().toLowerCase().replace("_door", "");
        return name.replace('_', ' ');
    }
}
