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
package top.midream.ddoor.visual;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import top.midream.ddoor.DDoorPlugin;
import top.midream.ddoor.door.DoorBlocks;
import top.midream.ddoor.door.DoorRecord;
import top.midream.ddoor.door.PairManager;
import top.midream.ddoor.door.PortalRegistry;

/**
 * Periodic reconciliation: verify door blocks still exist, drop stale records,
 * and re-index double-door twin blocks that were missed during cold load.
 */
public class AuditTask extends BukkitRunnable {

    private final DDoorPlugin plugin;
    private final PortalRegistry registry;
    private final PairManager pairs;

    public AuditTask(DDoorPlugin plugin, PortalRegistry registry, PairManager pairs) {
        this.plugin = plugin;
        this.registry = registry;
        this.pairs = pairs;
    }

    @Override
    public void run() {
        int cleaned = 0;
        for (DoorRecord door : registry.all()) {
            World world = Bukkit.getWorld(door.world());
            if (world == null) continue;
            if (!world.isChunkLoaded(door.x() >> 4, door.z() >> 4)) continue;

            Block block = world.getBlockAt(door.x(), door.y(), door.z());
            if (!DoorBlocks.isDoor(block)) {
                pairs.removeDoor(door, false);
                cleaned++;
                continue;
            }
            Block anchor = DoorBlocks.anchorOf(block);
            if (anchor == null || anchor.getX() != door.x() || anchor.getY() != door.y()
                    || anchor.getZ() != door.z()) {
                // block became part of another door layout — treat as gone
                pairs.removeDoor(door, false);
                cleaned++;
                continue;
            }
            // refresh twin-block index entries (idempotent re-register)
            registry.unregister(door);
            registry.register(door, DoorBlocks.extraBlocksOf(anchor));
        }
        if (cleaned > 0) {
            plugin.getLogger().info("audit: removed " + cleaned + " stale door records");
        }
    }
}
