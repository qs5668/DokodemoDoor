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
package top.midream.ddoor.player;

import org.bukkit.entity.Player;
import top.midream.ddoor.DDoorPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player preferences: teleport trigger mode and info verbosity.
 * Cached in memory, written through to storage asynchronously.
 */
public final class PlayerSettings {

    public enum Mode { WALK, RIGHT_CLICK, LEFT_CLICK }

    private static final Mode[] CYCLE = {Mode.WALK, Mode.RIGHT_CLICK, Mode.LEFT_CLICK};

    private final DDoorPlugin plugin;
    private final Map<UUID, Mode> modes = new ConcurrentHashMap<>();
    private final Set<UUID> simpleInfo = ConcurrentHashMap.newKeySet();
    private volatile Mode defaultMode = Mode.WALK;

    public PlayerSettings(DDoorPlugin plugin) {
        this.plugin = plugin;
    }

    /** Load cached settings from storage; called once at enable. */
    public void load() {
        defaultMode = parse(plugin.cfg().defaultTeleportMode);
        try {
            Map<UUID, top.midream.ddoor.storage.PlayerPrefs> saved = plugin.store().loadPlayerSettings();
            for (Map.Entry<UUID, top.midream.ddoor.storage.PlayerPrefs> e : saved.entrySet()) {
                Mode m = parse(e.getValue().mode());
                if (m != defaultMode) modes.put(e.getKey(), m);
                if (e.getValue().simpleInfo()) simpleInfo.add(e.getKey());
            }
            plugin.getLogger().info("loaded " + saved.size() + " player setting overrides");
        } catch (Exception e) {
            plugin.getLogger().warning("failed to load player settings: " + e.getMessage());
        }
    }

    public Mode defaultMode() {
        return defaultMode;
    }

    public Mode modeOf(UUID player) {
        return modes.getOrDefault(player, defaultMode);
    }

    public boolean simpleOf(UUID player) {
        return simpleInfo.contains(player);
    }

    /** Advance WALK -> RIGHT_CLICK -> LEFT_CLICK -> WALK and persist. */
    public Mode cycleMode(Player player) {
        Mode next = CYCLE[(modeOf(player.getUniqueId()).ordinal() + 1) % CYCLE.length];
        setMode(player, next);
        return next;
    }

    public void setMode(Player player, Mode mode) {
        modes.put(player.getUniqueId(), mode);
        persist(player.getUniqueId(), mode, simpleInfo.contains(player.getUniqueId()));
    }

    public void setSimple(Player player, boolean simple) {
        if (simple) {
            simpleInfo.add(player.getUniqueId());
        } else {
            simpleInfo.remove(player.getUniqueId());
        }
        persist(player.getUniqueId(), modeOf(player.getUniqueId()), simple);
    }

    private void persist(UUID uuid, Mode mode, boolean simple) {
        plugin.writes().submit("settings-" + uuid, () -> {
            try {
                plugin.store().savePlayerSettings(uuid, mode.name(), simple);
            } catch (Exception e) {
                plugin.getLogger().warning("failed to save player settings: " + e.getMessage());
            }
        });
    }

    private static Mode parse(String raw) {
        if (raw == null) return Mode.WALK;
        try {
            return Mode.valueOf(raw.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return Mode.WALK;
        }
    }
}
