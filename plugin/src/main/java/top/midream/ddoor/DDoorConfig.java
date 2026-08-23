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

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class DDoorConfig {

    public final String language;
    public final Material keyItem;
    public final String keyName;
    public final int keyCustomModelData;
    public final boolean keyCraftable;
    public final int keyRecipeOutput;

    public final Material entityKeyItem;
    public final int entityKeyCustomModelData;
    public final boolean entityKeyCraftable;
    public final int entityKeyRecipeOutput;
    public final int entityKeyExpireHours;

    public final boolean entityEnabled;
    public final int entityCooldownSeconds;
    public final boolean entityNamedOnly;

    public final int cooldownSeconds;
    public final boolean fadeEffect;
    public final int antiFallTicks;
    public final boolean denyVehicles;
    public final String defaultTeleportMode;

    public final int sessionTimeoutSeconds;
    public final int defaultLimit;
    public final int unlinkCooldownSeconds;

    public final boolean logsEnabled;
    public final int logsRetentionDays;
    public final int logsLoadDays;

    public final boolean idleParticle;
    public final int particleRange;
    public final boolean soundOnTeleport;

    public final String worldMode;   // blacklist / whitelist
    public final java.util.List<String> worldList;

    public final boolean economyEnabled;
    public final double createCost;
    public final double useCost;

    public final String storageType;
    public final int auditIntervalSeconds;
    public final boolean hookPapi;

    public DDoorConfig(JavaPlugin plugin) {
        language = plugin.getConfig().getString("language", "zh_CN");
        keyItem = Material.matchMaterial(plugin.getConfig().getString("key.item", "AMETHYST_SHARD"));
        keyName = plugin.getConfig().getString("key.name", "门之钥");
        keyCustomModelData = plugin.getConfig().getInt("key.custom-model-data", 21001);
        keyCraftable = plugin.getConfig().getBoolean("key.craftable", true);
        keyRecipeOutput = plugin.getConfig().getInt("key.recipe-output", 2);

        ConfigurationSection ek = plugin.getConfig().getConfigurationSection("entity-key");
        entityKeyItem = Material.matchMaterial(plugin.getConfig().getString("entity-key.item", "AMETHYST_SHARD"));
        entityKeyCustomModelData = plugin.getConfig().getInt("entity-key.custom-model-data", 21002);
        entityKeyCraftable = ek.getBoolean("craftable", true);
        entityKeyRecipeOutput = ek.getInt("recipe-output", 1);
        entityKeyExpireHours = ek.getInt("expire-hours", 48);

        ConfigurationSection ent = plugin.getConfig().getConfigurationSection("entity");
        entityEnabled = ent.getBoolean("enabled", true);
        entityCooldownSeconds = ent.getInt("cooldown-seconds", 5);
        entityNamedOnly = ent.getBoolean("named-only", false);

        ConfigurationSection tp = plugin.getConfig().getConfigurationSection("teleport");
        cooldownSeconds = tp.getInt("cooldown-seconds", 3);
        fadeEffect = tp.getBoolean("fade-effect", true);
        antiFallTicks = tp.getInt("anti-fall-ticks", 40);
        denyVehicles = tp.getBoolean("deny-vehicles", true);
        defaultTeleportMode = tp.getString("default-mode", "WALK");

        ConfigurationSection pair = plugin.getConfig().getConfigurationSection("pairing");
        sessionTimeoutSeconds = pair.getInt("session-timeout-seconds", 60);
        defaultLimit = pair.getInt("default-limit", 3);
        unlinkCooldownSeconds = pair.getInt("unlink-cooldown-seconds", 10);

        ConfigurationSection logs = plugin.getConfig().getConfigurationSection("logs");
        logsEnabled = logs.getBoolean("enabled", true);
        logsRetentionDays = logs.getInt("retention-days", 30);
        logsLoadDays = logs.getInt("load-days", 7);

        ConfigurationSection visual = plugin.getConfig().getConfigurationSection("visual");
        idleParticle = visual.getBoolean("idle-particle", true);
        particleRange = visual.getInt("particle-range", 32);
        soundOnTeleport = visual.getBoolean("sound-on-teleport", true);

        ConfigurationSection worlds = plugin.getConfig().getConfigurationSection("worlds");
        worldMode = worlds.getString("mode", "blacklist");
        worldList = worlds.getStringList("list");

        ConfigurationSection eco = plugin.getConfig().getConfigurationSection("economy");
        economyEnabled = eco.getBoolean("enabled", false);
        createCost = eco.getDouble("create-cost", 0.0);
        useCost = eco.getDouble("use-cost", 0.0);

        storageType = plugin.getConfig().getString("storage.type", "sqlite");
        auditIntervalSeconds = plugin.getConfig().getInt("maintenance.audit-interval-seconds", 300);
        hookPapi = plugin.getConfig().getBoolean("hooks.placeholderapi", true);
    }
}
