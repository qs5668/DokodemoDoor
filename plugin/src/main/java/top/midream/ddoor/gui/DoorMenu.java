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

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import top.midream.ddoor.DDoorPlugin;
import top.midream.ddoor.door.DoorRecord;
import top.midream.ddoor.door.PairManager;
import top.midream.ddoor.util.Msg;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Chest GUI over the player's own door pairs: paginated list with
 * click-to-teleport and shift-right-click-to-unlink, plus stats and
 * a key recipe hint. Pure Bukkit inventory API — works on both platforms.
 */
public final class DoorMenu {

    private static final int SIZE = 54;
    private static final int DOORS_PER_PAGE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_STATS = 47;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_KEY = 51;
    private static final int SLOT_NEXT = 53;

    private final DDoorPlugin plugin;
    private final Msg msg;

    public DoorMenu(DDoorPlugin plugin) {
        this.plugin = plugin;
        this.msg = plugin.msg();
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        List<DoorRecord> pairs = ownPairs(player.getUniqueId());
        int pages = Math.max(1, (pairs.size() + DOORS_PER_PAGE - 1) / DOORS_PER_PAGE);
        int safePage = Math.max(0, Math.min(page, pages - 1));

        DoorMenuHolder holder = new DoorMenuHolder(player.getUniqueId(), safePage, pages);
        Inventory inv = plugin.text().inventory(holder, SIZE,
                msg.parse("gui.title", "page", safePage + 1, "pages", pages));
        holder.inventory(inv);

        if (pairs.isEmpty()) {
            inv.setItem(22, item(Material.OAK_DOOR, msg.parse("gui.empty-name"),
                    msg.parseList("gui.empty-lore")));
        } else {
            int from = safePage * DOORS_PER_PAGE;
            int to = Math.min(pairs.size(), from + DOORS_PER_PAGE);
            for (int i = from; i < to; i++) {
                int slot = i - from;
                inv.setItem(slot, doorItem(pairs.get(i)));
                holder.map(slot, pairs.get(i).id());
            }
        }

        if (safePage > 0) {
            inv.setItem(SLOT_PREV, item(Material.ARROW, msg.parse("gui.prev-name"), null));
        }
        if (safePage + 1 < pages) {
            inv.setItem(SLOT_NEXT, item(Material.ARROW, msg.parse("gui.next-name"), null));
        }
        inv.setItem(SLOT_STATS, statsItem(player));
        inv.setItem(SLOT_CLOSE, item(Material.BARRIER, msg.parse("gui.close-name"), null));
        inv.setItem(SLOT_KEY, keyItem());
        for (int filler : new int[]{46, 48, 50, 52}) {
            inv.setItem(filler, item(Material.GRAY_STAINED_GLASS_PANE, Component.empty(), null));
        }

        player.openInventory(inv);
    }

    void handleClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DoorMenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        UUID doorId = holder.doorAt(event.getSlot());
        if (doorId != null) {
            DoorRecord door = plugin.registry().byId(doorId);
            if (door == null || !door.isPaired()) {
                open(player, holder.page());
                return;
            }
            if (event.isShiftClick() && event.isRightClick()) {
                unlink(player, door, holder);
            } else if (event.isLeftClick()) {
                teleport(player, door);
            }
            return;
        }

        switch (event.getSlot()) {
            case SLOT_PREV -> {
                if (holder.page() > 0) open(player, holder.page() - 1);
            }
            case SLOT_NEXT -> {
                if (holder.page() + 1 < holder.pages()) open(player, holder.page() + 1);
            }
            case SLOT_CLOSE -> player.closeInventory();
            default -> { }
        }
    }

    private void teleport(Player player, DoorRecord door) {
        if (!player.getUniqueId().equals(door.owner()) && !player.hasPermission("ddoor.tp")) {
            msg.send(player, "cmd.no-permission");
            return;
        }
        player.closeInventory();
        plugin.engine().commandTeleport(player, door);
    }

    private void unlink(Player player, DoorRecord door, DoorMenuHolder holder) {
        if (!player.getUniqueId().equals(door.owner()) && !player.hasPermission("ddoor.admin")) {
            msg.send(player, "cmd.no-permission");
            return;
        }
        plugin.pairs().unlinkDoor(door, PairManager.CommandSource.of(player), true);
        open(player, holder.page());
    }

    private ItemStack doorItem(DoorRecord door) {
        DoorRecord other = plugin.registry().byId(door.pairedId());
        String otherWorld = other == null ? "?" : other.world();
        int ox = other == null ? 0 : other.x();
        int oy = other == null ? 0 : other.y();
        int oz = other == null ? 0 : other.z();
        List<Component> lore = msg.parseList("gui.door-lore",
                "name", door.name(),
                "world_a", door.world(), "x_a", door.x(), "y_a", door.y(), "z_a", door.z(),
                "world_b", otherWorld, "x_b", ox, "y_b", oy, "z_b", oz,
                "uses", door.uses() + (other == null ? 0 : other.uses()));
        return item(Material.OAK_DOOR, msg.parse("gui.door-name", "name", door.name()), lore);
    }

    private ItemStack statsItem(Player player) {
        int limit = plugin.pairs().limitOf(player);
        String limitText = limit < 0 ? "∞" : String.valueOf(limit);
        List<Component> lore = msg.parseList("gui.stats-lore",
                "count", plugin.registry().pairsOf(player.getUniqueId()),
                "limit", limitText,
                "uses", plugin.registry().usesOf(player.getUniqueId()));
        return item(Material.BOOK, msg.parse("gui.stats-name"), lore);
    }

    private ItemStack keyItem() {
        return item(Material.AMETHYST_SHARD, msg.parse("gui.key-name"), msg.parseList("gui.key-lore"));
    }

    private ItemStack item(Material material, Component name, List<Component> lore) {
        return plugin.text().item(new ItemStack(material), name, lore, ItemFlag.HIDE_ATTRIBUTES);
    }

    private List<DoorRecord> ownPairs(UUID owner) {
        List<DoorRecord> out = new ArrayList<>();
        for (DoorRecord d : plugin.registry().all()) {
            if (d.isPaired() && d.owner().equals(owner)
                    && d.id().compareTo(d.pairedId()) < 0) {
                out.add(d);
            }
        }
        out.sort((a, b) -> Long.compare(b.createdAt(), a.createdAt()));
        return out;
    }
}
