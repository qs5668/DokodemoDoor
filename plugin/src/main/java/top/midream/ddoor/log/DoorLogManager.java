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
package top.midream.ddoor.log;

import top.midream.ddoor.DDoorPlugin;
import top.midream.ddoor.door.DoorRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Interaction history per door: ring buffer in memory (newest last),
 * mirrored to storage asynchronously. GUI reads never touch SQL.
 */
public final class DoorLogManager {

    private static final int PER_DOOR_CAP = 50;

    private final DDoorPlugin plugin;
    private final boolean enabled;
    private final int retentionDays;
    private final int loadDays;
    private final Map<UUID, Deque<DoorLog>> byDoor = new ConcurrentHashMap<>();

    public DoorLogManager(DDoorPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.cfg().logsEnabled;
        this.retentionDays = plugin.cfg().logsRetentionDays;
        this.loadDays = plugin.cfg().logsLoadDays;
    }

    /** Load recent history into memory and prune expired rows; called once at enable. */
    public void load() {
        if (!enabled) return;
        long now = System.currentTimeMillis();
        try {
            long since = loadDays > 0 ? now - loadDays * 86400_000L : 0L;
            for (DoorLog l : plugin.store().loadRecentLogs(since)) {
                Deque<DoorLog> q = byDoor.computeIfAbsent(l.doorId(), k -> new ConcurrentLinkedDeque<>());
                if (q.size() < PER_DOOR_CAP) q.addFirst(l);
            }
            plugin.getLogger().info("loaded interaction logs (last " + loadDays + "d)");
        } catch (Exception e) {
            plugin.getLogger().warning("failed to load interaction logs: " + e.getMessage());
        }
        cleanup(now);
    }

    /** Periodic retention cleanup; safe to call from any task. */
    public void cleanup(long now) {
        if (!enabled || retentionDays <= 0) return;
        long before = now - retentionDays * 86400_000L;
        plugin.writes().submit("logs-cleanup", () -> {
            try {
                plugin.store().cleanupLogs(before);
            } catch (Exception e) {
                plugin.getLogger().warning("failed to cleanup logs: " + e.getMessage());
            }
        });
    }

    public void log(DoorRecord door, String playerName, String action) {
        if (!enabled || door == null) return;
        DoorLog entry = new DoorLog(door.id(), door.name(), door.world(),
                door.x(), door.y(), door.z(),
                playerName == null ? "?" : playerName, action, System.currentTimeMillis());
        Deque<DoorLog> q = byDoor.computeIfAbsent(door.id(), k -> new ConcurrentLinkedDeque<>());
        q.addLast(entry);
        while (q.size() > PER_DOOR_CAP) q.pollFirst();
        plugin.writes().submit("log-" + door.id() + "-" + entry.time(), () -> {
            try {
                plugin.store().insertLog(entry);
            } catch (Exception e) {
                plugin.getLogger().warning("failed to insert log: " + e.getMessage());
            }
        });
    }

    public boolean enabled() {
        return enabled;
    }

    /** Newest-first history of a pair, merged from both doors. */
    public List<DoorLog> pairLogs(UUID doorA, UUID doorB) {
        List<DoorLog> out = new ArrayList<>();
        Deque<DoorLog> a = byDoor.get(doorA);
        if (a != null) out.addAll(a);
        if (doorB != null && !doorB.equals(doorA)) {
            Deque<DoorLog> b = byDoor.get(doorB);
            if (b != null) out.addAll(b);
        }
        out.sort(Comparator.comparingLong(DoorLog::time).reversed());
        return out;
    }

    public long countLoaded() {
        return byDoor.values().stream().mapToLong(Deque::size).sum();
    }
}
