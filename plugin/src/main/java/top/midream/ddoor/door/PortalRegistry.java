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

import top.midream.ddoor.util.BlockKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory door registry. All lookups are O(1) hash reads:
 * world name -> packed block coordinate -> door id.
 */
public class PortalRegistry {

    private final Map<UUID, DoorRecord> byId = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, UUID>> byBlock = new ConcurrentHashMap<>();

    public void register(DoorRecord door, long... extraBlocks) {
        byId.put(door.id(), door);
        blockIndex(door.world()).put(BlockKey.pack(door.x(), door.y(), door.z()), door.id());
        for (long key : extraBlocks) {
            blockIndex(door.world()).put(key, door.id());
        }
    }

    public void unregister(DoorRecord door) {
        byId.remove(door.id());
        Map<Long, UUID> idx = blockIndex(door.world());
        idx.entrySet().removeIf(e -> e.getValue().equals(door.id()));
    }

    public DoorRecord byId(UUID id) {
        return id == null ? null : byId.get(id);
    }

    /** Find the door whose anchor (or registered twin block) is at these coords. */
    public DoorRecord at(String world, int x, int y, int z) {
        Map<Long, UUID> idx = byBlock.get(world);
        if (idx == null) return null;
        UUID id = idx.get(BlockKey.pack(x, y, z));
        return id == null ? null : byId.get(id);
    }

    public Collection<DoorRecord> all() {
        return byId.values();
    }

    public List<DoorRecord> byOwner(UUID owner) {
        List<DoorRecord> out = new ArrayList<>();
        for (DoorRecord d : byId.values()) {
            if (d.owner().equals(owner)) out.add(d);
        }
        return out;
    }

    public DoorRecord byName(String name) {
        String lower = name.toLowerCase();
        for (DoorRecord d : byId.values()) {
            if (d.name().toLowerCase().equals(lower)) return d;
        }
        return null;
    }

    public DoorRecord byNameOwner(String name, UUID owner) {
        String lower = name.toLowerCase();
        for (DoorRecord d : byId.values()) {
            if (d.owner().equals(owner) && d.name().toLowerCase().equals(lower)) return d;
        }
        return null;
    }

    /** Count of complete pairs owned by the player (a pair counts once). */
    public long pairsOf(UUID owner) {
        long count = 0;
        for (DoorRecord d : byId.values()) {
            if (d.owner().equals(owner) && d.isPaired() && d.id().compareTo(d.pairedId()) < 0) {
                count++;
            }
        }
        return count;
    }

    public long totalPairs() {
        long count = 0;
        for (DoorRecord d : byId.values()) {
            if (d.isPaired() && d.id().compareTo(d.pairedId()) < 0) count++;
        }
        return count;
    }

    public long totalUses() {
        long sum = 0;
        for (DoorRecord d : byId.values()) sum += d.uses();
        return sum;
    }

    public long usesOf(UUID owner) {
        long sum = 0;
        for (DoorRecord d : byId.values()) {
            if (d.owner().equals(owner)) sum += d.uses();
        }
        return sum;
    }

    private Map<Long, UUID> blockIndex(String world) {
        return byBlock.computeIfAbsent(world, k -> new ConcurrentHashMap<>());
    }
}
