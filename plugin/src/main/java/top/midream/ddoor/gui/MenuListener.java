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
package top.midream.ddoor.gui;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import top.midream.ddoor.DDoorPlugin;

/** Routes inventory events for DoorMenu sessions and chat lines for renames. */
public final class MenuListener implements Listener {

    private final DDoorPlugin plugin;
    private final DoorMenu menu;

    public MenuListener(DDoorPlugin plugin, DoorMenu menu) {
        this.plugin = plugin;
        this.menu = menu;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        menu.handleClick(event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof DoorMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        java.util.UUID doorId = menu.pollRename(event.getPlayer().getUniqueId());
        if (doorId == null) return;
        event.setCancelled(true);
        String input = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () ->
                menu.applyRename(event.getPlayer(), doorId, input));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        menu.clearRename(event.getPlayer().getUniqueId());
    }
}
