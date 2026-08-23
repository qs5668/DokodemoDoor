package top.midream.ddoor.door;

import org.bukkit.block.BlockFace;

import java.util.UUID;

public class DoorRecord {

    private final UUID id;
    private String name;
    private final UUID owner;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final BlockFace facing;
    private UUID pairedId;
    private final long createdAt;
    private long uses;

    public DoorRecord(UUID id, String name, UUID owner, String world, int x, int y, int z,
                      BlockFace facing, UUID pairedId, long createdAt, long uses) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.facing = facing;
        this.pairedId = pairedId;
        this.createdAt = createdAt;
        this.uses = uses;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public void name(String name) { this.name = name; }
    public UUID owner() { return owner; }
    public String world() { return world; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public BlockFace facing() { return facing; }
    public UUID pairedId() { return pairedId; }
    public void pairedId(UUID pairedId) { this.pairedId = pairedId; }
    public long createdAt() { return createdAt; }
    public long uses() { return uses; }
    public void uses(long uses) { this.uses = uses; }
    public void incrementUses() { uses++; }

    public boolean isPaired() { return pairedId != null; }
}
