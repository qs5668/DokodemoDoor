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
package top.midream.ddoor;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import top.midream.ddoor.command.DDoorCommand;
import top.midream.ddoor.door.DoorRecord;
import top.midream.ddoor.door.PairManager;
import top.midream.ddoor.door.PortalRegistry;
import top.midream.ddoor.gui.DoorMenu;
import top.midream.ddoor.gui.MenuListener;
import top.midream.ddoor.hook.PapiHook;
import top.midream.ddoor.hook.VaultHook;
import top.midream.ddoor.key.KeyItem;
import top.midream.ddoor.listener.BlockWatcher;
import top.midream.ddoor.listener.KeyUseListener;
import top.midream.ddoor.listener.MoveListener;
import top.midream.ddoor.platform.TextAdapter;
import top.midream.ddoor.storage.DoorStore;
import top.midream.ddoor.storage.MysqlStore;
import top.midream.ddoor.storage.SqliteStore;
import top.midream.ddoor.storage.WriteQueue;
import top.midream.ddoor.teleport.TeleportEngine;
import top.midream.ddoor.util.Msg;
import top.midream.ddoor.visual.AuditTask;
import top.midream.ddoor.visual.ParticleTask;

import java.util.List;
import java.util.logging.Level;

public final class DDoorPlugin extends JavaPlugin {

    private volatile DDoorConfig cfg;
    private TextAdapter text;
    private Msg msg;
    private DoorStore store;
    private WriteQueue writeQueue;
    private PortalRegistry registry;
    private PairManager pairs;
    private TeleportEngine engine;
    private KeyItem keyItem;
    private DoorMenu menu;
    private VaultHook vault;
    private PapiHook papiHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfAbsent("lang/zh_CN.yml");
        saveResourceIfAbsent("lang/en_US.yml");
        reloadConfig();
        cfg = new DDoorConfig(this);

        text = new top.midream.ddoor.platform.TextAdapterImpl();
        msg = new Msg(this, text);
        msg.load(cfg.language);

        writeQueue = new WriteQueue(this);
        store = "mysql".equalsIgnoreCase(cfg.storageType) ? new MysqlStore(this) : new SqliteStore(this);
        try {
            store.init();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "storage init failed, disabling plugin", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        writeQueue.bind(store);

        registry = new PortalRegistry();
        try {
            List<DoorRecord> loaded = store.loadAll();
            for (DoorRecord d : loaded) {
                registry.register(d);
            }
            getLogger().info("loaded " + loaded.size() + " door records (" + registry.totalPairs() + " pairs)");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "failed to load doors from storage", e);
        }

        vault = new VaultHook();
        if (cfg.economyEnabled) {
            if (vault.setup()) {
                getLogger().info("Vault economy hooked");
            } else {
                getLogger().warning("economy.enabled=true but no Vault economy found — charges are skipped");
            }
        }

        pairs = new PairManager(this, registry, writeQueue, msg, vault);
        engine = new TeleportEngine(this, registry, pairs, msg, vault);
        keyItem = new KeyItem(this, msg);
        menu = new DoorMenu(this);

        getServer().getPluginManager().registerEvents(new KeyUseListener(pairs, keyItem), this);
        getServer().getPluginManager().registerEvents(new MoveListener(registry, engine), this);
        getServer().getPluginManager().registerEvents(new BlockWatcher(pairs, registry, msg), this);
        getServer().getPluginManager().registerEvents(new MenuListener(menu), this);

        PluginCommand cmd = getCommand("ddoor");
        if (cmd != null) {
            DDoorCommand executor = new DDoorCommand(this, registry, pairs);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        keyItem.registerRecipe();

        new ParticleTask(this, registry, pairs).runTaskTimer(this, 40L, 4L);
        new AuditTask(this, registry, pairs).runTaskTimer(this, 100L, cfg.auditIntervalSeconds * 20L);
        getServer().getScheduler().runTaskTimer(this, pairs::tickCleanup, 20L, 20L);

        if (cfg.hookPapi && getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiHook = new PapiHook(this, registry);
            papiHook.register();
            getLogger().info("PlaceholderAPI expansion registered");
        }

        getLogger().info("DokodemoDoor enabled (" + text.id() + " build) — place two doors, link with a key, walk through.");
    }

    @Override
    public void onDisable() {
        if (writeQueue != null) writeQueue.flush(10);
        if (store != null) store.close();
        getLogger().info("DokodemoDoor disabled — all writes flushed.");
    }

    /** Reload config and messages without touching data. */
    public void reloadAll() {
        reloadConfig();
        cfg = new DDoorConfig(this);
        msg.load(cfg.language);
    }

    private void saveResourceIfAbsent(String path) {
        java.io.File f = new java.io.File(getDataFolder(), path);
        if (!f.exists()) saveResource(path, false);
    }

    public DDoorConfig cfg() { return cfg; }
    public TextAdapter text() { return text; }
    public Msg msg() { return msg; }
    public DoorStore store() { return store; }
    public PortalRegistry registry() { return registry; }
    public PairManager pairs() { return pairs; }
    public TeleportEngine engine() { return engine; }
    public KeyItem keyItem() { return keyItem; }
    public DoorMenu menu() { return menu; }
    public VaultHook vault() { return vault; }
}
