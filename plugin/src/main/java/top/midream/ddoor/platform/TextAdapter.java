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
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Platform seam between Paper (native Adventure) and Spigot (legacy strings).
 * The build selects one TextAdapterImpl: src/paper/java or src/spigot/java.
 */
public interface TextAdapter {

    /** Platform id for the startup log: "paper" or "spigot". */
    String id();

    void send(CommandSender to, Component message);

    void actionBar(Player player, Component message);

    void name(ItemMeta meta, Component name);

    void lore(ItemMeta meta, List<Component> lore);

    Inventory inventory(InventoryHolder owner, int size, Component title);

    CompletableFuture<Boolean> teleport(Entity entity, Location to);

    /** Convenience: build a fully-decorated item through the platform seam. */
    default ItemStack item(ItemStack base, Component displayName, List<Component> loreLines, ItemFlag... flags) {
        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            name(meta, displayName);
            if (loreLines != null && !loreLines.isEmpty()) lore(meta, loreLines);
            for (ItemFlag flag : flags) meta.addItemFlags(flag);
            base.setItemMeta(meta);
        }
        return base;
    }
}
