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

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Periodic reconciliation: verify door blocks still exist, drop stale records,
 * and re-index double-door twin blocks that were missed during cold load.
 *
 * Budgeted and resumable: each scheduler run processes at most BATCH doors or
 * BUDGET_MS of wall time, whichever comes first, then resumes from the queue
 * on the next run. A full pass therefore never monopolises a tick — if the
 * host stalls (GC, page faults, CPU contention) the loop brakes early instead
 * of extending the stall into a watchdog kill.
 */
public class AuditTask extends BukkitRunnable {

    private static final long BUDGET_MS = 20L;
    private static final int BATCH = 256;
    private static final long SLOW_PASS_MS = 500L;

    private final DDoorPlugin plugin;
    private final PortalRegistry registry;
    private final PairManager pairs;

    private final ArrayDeque<DoorRecord> pending = new ArrayDeque<>();
    private boolean passOpen = false;
    private long activeMs = 0L;
    private int cleaned = 0;

    public AuditTask(DDoorPlugin plugin, PortalRegistry registry, PairManager pairs) {
        this.plugin = plugin;
        this.registry = registry;
        this.pairs = pairs;
    }

    @Override
    public void run() {
        if (pending.isEmpty()) {
            pending.addAll(registry.all());
            passOpen = true;
            activeMs = 0L;
            cleaned = 0;
        }

        long start = System.currentTimeMillis();
        long deadline = start + BUDGET_MS;
        int processed = 0;
        while (!pending.isEmpty() && processed < BATCH && System.currentTimeMillis() < deadline) {
            audit(pending.poll());
            processed++;
        }
        activeMs += System.currentTimeMillis() - start;

        if (pending.isEmpty() && passOpen) {
            passOpen = false;
            if (activeMs > SLOW_PASS_MS) {
                plugin.getLogger().warning("audit pass took " + activeMs + "ms — consider raising maintenance.audit-interval-seconds");
            }
            if (cleaned > 0) {
                plugin.getLogger().info("audit: removed " + cleaned + " stale door records");
            }
        }
    }

    private void audit(DoorRecord door) {
        // the record may have been deleted or replaced while queued
        if (registry.byId(door.id()) != door) return;

        World world = Bukkit.getWorld(door.world());
        if (world == null) return;
        if (!world.isChunkLoaded(door.x() >> 4, door.z() >> 4)) return;

        Block block = world.getBlockAt(door.x(), door.y(), door.z());
        if (!DoorBlocks.isDoor(block)) {
            pairs.removeDoor(door, false);
            cleaned++;
            return;
        }
        Block anchor = DoorBlocks.anchorOf(block);
        if (anchor == null || anchor.getX() != door.x() || anchor.getY() != door.y()
                || anchor.getZ() != door.z()) {
            // block became part of another door layout — treat as gone
            pairs.removeDoor(door, false);
            cleaned++;
            return;
        }
        // re-index twin blocks only when the layout actually changed
        long[] fresh = DoorBlocks.extraBlocksOf(anchor);
        if (!Arrays.equals(fresh, door.extraBlocks())) {
            registry.unregister(door);
            registry.register(door, fresh);
        }
    }
}
