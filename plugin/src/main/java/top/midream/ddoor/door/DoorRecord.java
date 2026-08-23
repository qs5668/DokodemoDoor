/*
 * DokodemoDoor — pair-based cross-world door portals for Minecraft.
 * Copyright (C) 2026 qs5668
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
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
    private boolean enabled = true;

    public DoorRecord(UUID id, String name, UUID owner, String world, int x, int y, int z,
                      BlockFace facing, UUID pairedId, long createdAt, long uses) {
        this(id, name, owner, world, x, y, z, facing, pairedId, createdAt, uses, true);
    }

    public DoorRecord(UUID id, String name, UUID owner, String world, int x, int y, int z,
                      BlockFace facing, UUID pairedId, long createdAt, long uses, boolean enabled) {
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
        this.enabled = enabled;
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
    public boolean enabled() { return enabled; }
    public void enabled(boolean enabled) { this.enabled = enabled; }

    public boolean isPaired() { return pairedId != null; }
}
