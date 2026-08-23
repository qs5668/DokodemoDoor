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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Spigot/CraftBukkit: Adventure is shaded and relocated by the spigot build;
 * components are rendered to legacy section strings (§x hex) for display.
 */
public final class TextAdapterImpl implements TextAdapter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    @Override
    public String id() {
        return "spigot";
    }

    @Override
    public void send(CommandSender to, Component message) {
        to.sendMessage(LEGACY.serialize(message));
    }

    @Override
    public void actionBar(Player player, Component message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(LEGACY.serialize(message)));
    }

    @Override
    public void name(ItemMeta meta, Component name) {
        meta.setDisplayName(LEGACY.serialize(name));
    }

    @Override
    public void lore(ItemMeta meta, List<Component> lore) {
        List<String> lines = new ArrayList<>(lore.size());
        for (Component line : lore) lines.add(LEGACY.serialize(line));
        meta.setLore(lines);
    }

    @Override
    public Inventory inventory(InventoryHolder owner, int size, Component title) {
        return Bukkit.createInventory(owner, size, LEGACY.serialize(title));
    }

    @Override
    public CompletableFuture<Boolean> teleport(Entity entity, Location to) {
        // Synchronous teleport: every call site runs on the main thread.
        return CompletableFuture.completedFuture(entity.teleport(to));
    }
}
