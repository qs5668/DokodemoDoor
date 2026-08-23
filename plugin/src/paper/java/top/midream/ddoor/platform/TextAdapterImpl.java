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
package top.midream.ddoor.platform;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Paper/Leaves/Purpur: Adventure is provided by the server. */
public final class TextAdapterImpl implements TextAdapter {

    @Override
    public String id() {
        return "paper";
    }

    @Override
    public void send(CommandSender to, Component message) {
        to.sendMessage(message);
    }

    @Override
    public void actionBar(Player player, Component message) {
        player.sendActionBar(message);
    }

    @Override
    public void name(ItemMeta meta, Component name) {
        meta.displayName(name);
    }

    @Override
    public void lore(ItemMeta meta, List<Component> lore) {
        meta.lore(lore);
    }

    @Override
    public Inventory inventory(InventoryHolder owner, int size, Component title) {
        return Bukkit.createInventory(owner, size, title);
    }

    @Override
    public CompletableFuture<Boolean> teleport(Entity entity, Location to) {
        return entity.teleportAsync(to);
    }
}
