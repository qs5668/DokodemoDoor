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
package top.midream.ddoor.storage;

import org.bukkit.plugin.Plugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Single-writer async queue: the main thread only enqueues mutations,
 * a background thread applies them to the store. Flushed on disable.
 */
public class WriteQueue {

    private final Plugin plugin;
    private final ExecutorService executor;
    private volatile DoorStore store;

    public WriteQueue(Plugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DDoor-Writer");
            t.setDaemon(false);
            return t;
        });
    }

    public void bind(DoorStore store) {
        this.store = store;
    }

    public void submit(String op, Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "write op '" + op + "' failed", e);
            }
        });
    }

    /** Drain pending writes. Called from onDisable. */
    public void flush(int seconds) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(seconds, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("write queue did not drain in " + seconds + "s — some writes may be lost");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
