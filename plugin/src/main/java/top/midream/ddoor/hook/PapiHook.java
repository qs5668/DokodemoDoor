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
package top.midream.ddoor.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.midream.ddoor.DDoorPlugin;
import top.midream.ddoor.door.PortalRegistry;

public class PapiHook extends PlaceholderExpansion {

    private final DDoorPlugin plugin;
    private final PortalRegistry registry;

    public PapiHook(DDoorPlugin plugin, PortalRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ddoor";
    }

    @Override
    public @NotNull String getAuthor() {
        return "qs5668";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String getPlugin() {
        return null;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return params.equalsIgnoreCase("total") ? String.valueOf(registry.totalPairs()) : null;
        }
        switch (params.toLowerCase()) {
            case "uses" -> {
                return String.valueOf(registry.usesOf(player.getUniqueId()));
            }
            case "pairs" -> {
                return String.valueOf(registry.pairsOf(player.getUniqueId()));
            }
            case "limit" -> {
                return String.valueOf(plugin.pairs().limitOf(player));
            }
            case "total" -> {
                return String.valueOf(registry.totalPairs());
            }
            default -> {
                return null;
            }
        }
    }
}
