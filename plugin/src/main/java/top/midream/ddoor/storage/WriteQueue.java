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
