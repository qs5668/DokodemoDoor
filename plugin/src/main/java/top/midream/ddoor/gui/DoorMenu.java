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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import top.midream.ddoor.DDoorPlugin;
import top.midream.ddoor.door.DoorRecord;
import top.midream.ddoor.door.PairManager;
import top.midream.ddoor.log.DoorLog;
import top.midream.ddoor.log.DoorLogManager;
import top.midream.ddoor.player.PlayerSettings;
import top.midream.ddoor.util.Msg;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chest GUI with four pages: own pair list, per-pair detail/settings,
 * player settings, and interaction history. Pure Bukkit inventory API —
 * works on both platforms.
 */
public final class DoorMenu {

    // list page (54)
    private static final int SIZE = 54;
    private static final int DOORS_PER_PAGE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_STATS = 47;
    private static final int SLOT_SETTINGS = 48;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_KEY = 51;
    private static final int SLOT_NEXT = 53;

    // detail page (27)
    private static final int DET_SIZE = 27;
    private static final int DET_DOOR_A = 2;
    private static final int DET_INFO = 4;
    private static final int DET_DOOR_B = 6;
    private static final int DET_TP_A = 11;
    private static final int DET_RENAME = 12;
    private static final int DET_TOGGLE = 13;
    private static final int DET_UNLINK = 14;
    private static final int DET_TP_B = 15;
    private static final int DET_LOGS = 18;
    private static final int DET_BACK = 22;

    // settings page (27)
    private static final int SET_MODE = 11;
    private static final int SET_INFO = 15;
    private static final int SET_BACK = 22;

    // logs page (54)
    private static final int LOGS_PER_PAGE = 45;
    private static final int LOG_PREV = 45;
    private static final int LOG_BACK = 49;
    private static final int LOG_NEXT = 53;

    private static final SimpleDateFormat DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final DDoorPlugin plugin;
    private final Msg msg;
    private final Map<UUID, UUID> pendingRename = new ConcurrentHashMap<>();

    public DoorMenu(DDoorPlugin plugin) {
        this.plugin = plugin;
        this.msg = plugin.msg();
    }

    // ------------------------------------------------------------------ list

    public void open(Player player) {
        openList(player, 0);
    }

    public void openList(Player player, int page) {
        List<DoorRecord> pairs = ownPairs(player.getUniqueId());
        int pages = Math.max(1, (pairs.size() + DOORS_PER_PAGE - 1) / DOORS_PER_PAGE);
        int safePage = Math.max(0, Math.min(page, pages - 1));

        DoorMenuHolder holder = new DoorMenuHolder(player.getUniqueId(),
                DoorMenuHolder.Type.LIST, safePage, pages, null);
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
                inv.setItem(slot, doorItem(pairs.get(i), player));
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
        inv.setItem(SLOT_SETTINGS, item(Material.RECOVERY_COMPASS,
                msg.parse("gui.settings-name"), msg.parseList("gui.settings-lore")));
        inv.setItem(SLOT_CLOSE, item(Material.BARRIER, msg.parse("gui.close-name"), null));
        inv.setItem(SLOT_KEY, keyItem());
        for (int filler : new int[]{46, 50, 52}) {
            inv.setItem(filler, item(Material.GRAY_STAINED_GLASS_PANE, Component.empty(), null));
        }

        player.openInventory(inv);
    }

    // ---------------------------------------------------------------- detail

    public void openDetail(Player player, UUID doorId) {
        DoorRecord door = plugin.registry().byId(doorId);
        if (door == null || !door.isPaired()) {
            openList(player, 0);
            return;
        }
        DoorRecord other = plugin.registry().byId(door.pairedId());
        if (other == null) {
            openList(player, 0);
            return;
        }

        DoorMenuHolder holder = new DoorMenuHolder(player.getUniqueId(),
                DoorMenuHolder.Type.DETAIL, 0, 1, doorId);
        Inventory inv = plugin.text().inventory(holder, DET_SIZE,
                msg.parse("gui.detail-title", "name", door.name()));
        holder.inventory(inv);

        inv.setItem(DET_DOOR_A, doorEndItem(door, "gui.detail-end-a"));
        inv.setItem(DET_INFO, pairInfoItem(door, other));
        inv.setItem(DET_DOOR_B, doorEndItem(other, "gui.detail-end-b"));

        inv.setItem(DET_TP_A, item(Material.ENDER_PEARL, msg.parse("gui.detail-tp-a-name"),
                msg.parseList("gui.detail-tp-lore", "world", door.world(),
                        "x", door.x(), "y", door.y(), "z", door.z())));
        inv.setItem(DET_TP_B, item(Material.ENDER_PEARL, msg.parse("gui.detail-tp-b-name"),
                msg.parseList("gui.detail-tp-lore", "world", other.world(),
                        "x", other.x(), "y", other.y(), "z", other.z())));
        inv.setItem(DET_RENAME, item(Material.NAME_TAG, msg.parse("gui.detail-rename-name"),
                msg.parseList("gui.detail-rename-lore")));
        inv.setItem(DET_TOGGLE, item(door.enabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                msg.parse(door.enabled() ? "gui.detail-toggle-on-name" : "gui.detail-toggle-off-name"),
                msg.parseList(door.enabled() ? "gui.detail-toggle-on-lore" : "gui.detail-toggle-off-lore")));
        inv.setItem(DET_UNLINK, item(Material.SHEARS, msg.parse("gui.detail-unlink-name"),
                msg.parseList("gui.detail-unlink-lore")));
        inv.setItem(DET_LOGS, item(Material.WRITABLE_BOOK, msg.parse("gui.detail-logs-name"),
                msg.parseList("gui.detail-logs-lore")));
        inv.setItem(DET_BACK, item(Material.ARROW, msg.parse("gui.back-name"), null));

        player.openInventory(inv);
    }

    // -------------------------------------------------------------- settings

    public void openSettings(Player player) {
        DoorMenuHolder holder = new DoorMenuHolder(player.getUniqueId(),
                DoorMenuHolder.Type.SETTINGS, 0, 1, null);
        Inventory inv = plugin.text().inventory(holder, DET_SIZE, msg.parse("gui.settings-title"));
        holder.inventory(inv);

        PlayerSettings.Mode mode = plugin.settings().modeOf(player.getUniqueId());
        String modeKey = switch (mode) {
            case WALK -> "walk";
            case RIGHT_CLICK -> "right";
            case LEFT_CLICK -> "left";
        };
        inv.setItem(SET_MODE, item(Material.RECOVERY_COMPASS, msg.parse("gui.mode-name"),
                msg.parseList("gui.mode-lore", "mode", msg.raw("gui.mode-" + modeKey))));
        inv.setItem(SET_INFO, item(plugin.settings().simpleOf(player.getUniqueId())
                        ? Material.PAPER : Material.SPYGLASS,
                msg.parse("gui.info-mode-name"),
                msg.parseList(plugin.settings().simpleOf(player.getUniqueId())
                        ? "gui.info-mode-simple-lore" : "gui.info-mode-detailed-lore")));
        inv.setItem(SET_BACK, item(Material.ARROW, msg.parse("gui.back-name"), null));

        player.openInventory(inv);
    }

    // ------------------------------------------------------------------ logs

    public void openLogs(Player player, UUID doorId, int page) {
        DoorRecord door = plugin.registry().byId(doorId);
        if (door == null) {
            openList(player, 0);
            return;
        }
        DoorLogManager logs = plugin.logs();
        List<DoorLog> entries = logs.enabled() ? logs.pairLogs(door.id(), door.pairedId()) : List.of();
        int pages = Math.max(1, (entries.size() + LOGS_PER_PAGE - 1) / LOGS_PER_PAGE);
        int safePage = Math.max(0, Math.min(page, pages - 1));

        DoorMenuHolder holder = new DoorMenuHolder(player.getUniqueId(),
                DoorMenuHolder.Type.LOGS, safePage, pages, doorId);
        Inventory inv = plugin.text().inventory(holder, SIZE,
                msg.parse("gui.logs-title", "name", door.name(),
                        "page", safePage + 1, "pages", pages));
        holder.inventory(inv);

        if (!logs.enabled()) {
            inv.setItem(22, item(Material.BARRIER, msg.parse("logs.disabled"), null));
        } else {
            int from = safePage * LOGS_PER_PAGE;
            int to = Math.min(entries.size(), from + LOGS_PER_PAGE);
            if (entries.isEmpty()) {
                inv.setItem(22, item(Material.OAK_DOOR, msg.parse("logs.empty"), null));
            }
            for (int i = from; i < to; i++) {
                inv.setItem(i - from, logItem(entries.get(i)));
            }
            if (safePage > 0) {
                inv.setItem(LOG_PREV, item(Material.ARROW, msg.parse("gui.prev-name"), null));
            }
            if (safePage + 1 < pages) {
                inv.setItem(LOG_NEXT, item(Material.ARROW, msg.parse("gui.next-name"), null));
            }
        }
        inv.setItem(LOG_BACK, item(Material.ARROW, msg.parse("gui.back-name"), null));

        player.openInventory(inv);
    }

    // ---------------------------------------------------------------- clicks

    void handleClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DoorMenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        if (!player.getUniqueId().equals(holder.viewer())) return;

        switch (holder.type()) {
            case LIST -> handleListClick(player, event, holder);
            case DETAIL -> handleDetailClick(player, event, holder);
            case SETTINGS -> handleSettingsClick(player, event, holder);
            case LOGS -> handleLogsClick(player, event, holder);
        }
    }

    private void handleListClick(Player player, InventoryClickEvent event, DoorMenuHolder holder) {
        UUID doorId = holder.doorAt(event.getSlot());
        if (doorId != null) {
            DoorRecord door = plugin.registry().byId(doorId);
            if (door == null || !door.isPaired()) {
                openList(player, holder.page());
                return;
            }
            if (event.isShiftClick() && event.isRightClick()) {
                unlink(player, door, holder.page());
            } else if (event.isRightClick()) {
                openDetail(player, doorId);
            } else if (event.isLeftClick()) {
                teleport(player, door);
            }
            return;
        }

        switch (event.getSlot()) {
            case SLOT_PREV -> {
                if (holder.page() > 0) openList(player, holder.page() - 1);
            }
            case SLOT_NEXT -> {
                if (holder.page() + 1 < holder.pages()) openList(player, holder.page() + 1);
            }
            case SLOT_SETTINGS -> openSettings(player);
            case SLOT_CLOSE -> player.closeInventory();
            default -> { }
        }
    }

    private void handleDetailClick(Player player, InventoryClickEvent event, DoorMenuHolder holder) {
        DoorRecord door = plugin.registry().byId(holder.doorId());
        if (door == null || !door.isPaired()) {
            openList(player, 0);
            return;
        }
        DoorRecord other = plugin.registry().byId(door.pairedId());
        boolean owner = player.getUniqueId().equals(door.owner()) || player.hasPermission("ddoor.admin");

        switch (event.getSlot()) {
            case DET_TP_A -> teleport(player, door);
            case DET_TP_B -> {
                if (other != null) teleport(player, other);
            }
            case DET_RENAME -> {
                if (owner) beginRename(player, door);
            }
            case DET_TOGGLE -> {
                if (owner) togglePair(player, door, other);
            }
            case DET_UNLINK -> {
                if (owner) unlink(player, door, 0);
            }
            case DET_LOGS -> openLogs(player, door.id(), 0);
            case DET_BACK -> openList(player, 0);
            default -> { }
        }
    }

    private void handleSettingsClick(Player player, InventoryClickEvent event, DoorMenuHolder holder) {
        switch (event.getSlot()) {
            case SET_MODE -> {
                PlayerSettings.Mode next = plugin.settings().cycleMode(player);
                String key = switch (next) {
                    case WALK -> "walk";
                    case RIGHT_CLICK -> "right";
                    case LEFT_CLICK -> "left";
                };
                msg.send(player, "gui.mode-set", "mode", msg.raw("gui.mode-" + key));
                openSettings(player);
            }
            case SET_INFO -> {
                boolean nowSimple = !plugin.settings().simpleOf(player.getUniqueId());
                plugin.settings().setSimple(player, nowSimple);
                msg.send(player, "gui.info-mode-set",
                        "mode", msg.raw(nowSimple ? "gui.info-mode-simple" : "gui.info-mode-detailed"));
                openSettings(player);
            }
            case SET_BACK -> openList(player, 0);
            default -> { }
        }
    }

    private void handleLogsClick(Player player, InventoryClickEvent event, DoorMenuHolder holder) {
        switch (event.getSlot()) {
            case LOG_PREV -> {
                if (holder.page() > 0) openLogs(player, holder.doorId(), holder.page() - 1);
            }
            case LOG_NEXT -> {
                if (holder.page() + 1 < holder.pages()) openLogs(player, holder.doorId(), holder.page() + 1);
            }
            case LOG_BACK -> openDetail(player, holder.doorId());
            default -> { }
        }
    }

    // ---------------------------------------------------------------- rename

    private void beginRename(Player player, DoorRecord door) {
        player.closeInventory();
        pendingRename.put(player.getUniqueId(), door.id());
        msg.send(player, "gui.rename-prompt");
    }

    /** Poll (and clear) a pending rename request for this player. */
    public UUID pollRename(UUID player) {
        return pendingRename.remove(player);
    }

    public void clearRename(UUID player) {
        pendingRename.remove(player);
    }

    /** Apply a chat line as the new pair name; runs back on the main thread. */
    public void applyRename(Player player, UUID doorId, String input) {
        String name = input.trim();
        DoorRecord door = plugin.registry().byId(doorId);
        if (door == null) {
            msg.send(player, "cmd.not-found", "name", name);
            return;
        }
        if (name.equalsIgnoreCase("cancel") || name.equalsIgnoreCase("取消")) {
            msg.send(player, "gui.rename-cancelled");
            return;
        }
        if (name.isEmpty() || name.length() > 16) {
            msg.send(player, "cmd.rename-length");
            return;
        }
        if (!player.getUniqueId().equals(door.owner()) && !player.hasPermission("ddoor.admin")) {
            msg.send(player, "cmd.no-permission");
            return;
        }
        String old = door.name();
        DoorRecord other = plugin.registry().byId(door.pairedId());
        door.name(name);
        plugin.pairs().persist(door);
        if (other != null) {
            other.name(name);
            plugin.pairs().persist(other);
        }
        msg.send(player, "cmd.renamed", "old", old, "name", name);
    }

    // ----------------------------------------------------------------- logic

    private void teleport(Player player, DoorRecord door) {
        if (!player.getUniqueId().equals(door.owner()) && !player.hasPermission("ddoor.tp")) {
            msg.send(player, "cmd.no-permission");
            return;
        }
        player.closeInventory();
        plugin.engine().commandTeleport(player, door);
    }

    private void unlink(Player player, DoorRecord door, int backPage) {
        if (!player.getUniqueId().equals(door.owner()) && !player.hasPermission("ddoor.admin")) {
            msg.send(player, "cmd.no-permission");
            return;
        }
        plugin.pairs().unlinkDoor(door, PairManager.CommandSource.of(player), true);
        openList(player, backPage);
    }

    private void togglePair(Player player, DoorRecord door, DoorRecord other) {
        boolean enable = !door.enabled();
        door.enabled(enable);
        plugin.pairs().persist(door);
        if (other != null) {
            other.enabled(enable);
            plugin.pairs().persist(other);
        }
        msg.send(player, enable ? "gui.toggle-on-done" : "gui.toggle-off-done", "name", door.name());
        openDetail(player, door.id());
    }

    // ----------------------------------------------------------------- items

    private ItemStack doorItem(DoorRecord door, Player viewer) {
        DoorRecord other = plugin.registry().byId(door.pairedId());
        String otherWorld = other == null ? "?" : other.world();
        int ox = other == null ? 0 : other.x();
        int oy = other == null ? 0 : other.y();
        int oz = other == null ? 0 : other.z();
        long uses = door.uses() + (other == null ? 0 : other.uses());

        String loreKey = plugin.settings().simpleOf(viewer.getUniqueId())
                ? "gui.door-lore-simple" : "gui.door-lore";
        List<Component> lore = msg.parseList(loreKey,
                "name", door.name(),
                "world_a", door.world(), "x_a", door.x(), "y_a", door.y(), "z_a", door.z(),
                "world_b", otherWorld, "x_b", ox, "y_b", oy, "z_b", oz,
                "facing_a", facingText(door.facing()),
                "biome_a", biomeOf(door),
                "facing_b", other == null ? "?" : facingText(other.facing()),
                "biome_b", other == null ? "?" : biomeOf(other),
                "created", DATE.format(new Date(door.createdAt())),
                "uses", uses);
        return item(Material.OAK_DOOR, msg.parse("gui.door-name", "name", door.name()), lore);
    }

    private ItemStack doorEndItem(DoorRecord door, String titleKey) {
        List<Component> lore = msg.parseList("gui.detail-end-lore",
                "world", door.world(),
                "x", door.x(), "y", door.y(), "z", door.z(),
                "facing", facingText(door.facing()),
                "biome", biomeOf(door),
                "uses", door.uses());
        return item(Material.OAK_DOOR, msg.parse(titleKey), lore);
    }

    private ItemStack pairInfoItem(DoorRecord a, DoorRecord b) {
        long uses = a.uses() + b.uses();
        List<Component> lore = msg.parseList("gui.detail-info-lore",
                "created", DATE.format(new Date(Math.min(a.createdAt(), b.createdAt()))),
                "uses", uses,
                "status", msg.raw(a.enabled() && b.enabled()
                        ? "gui.status-enabled" : "gui.status-disabled"));
        return item(Material.BOOK, msg.parse("gui.detail-info-name", "name", a.name()), lore);
    }

    private ItemStack logItem(DoorLog entry) {
        String actionKey = switch (entry.action()) {
            case DoorLog.ACTION_LINK -> "logs.action-link";
            case DoorLog.ACTION_UNLINK -> "logs.action-unlink";
            case DoorLog.ACTION_BREAK -> "logs.action-break";
            default -> "logs.action-teleport";
        };
        List<Component> lore = msg.parseList("logs.entry-lore",
                "action", msg.raw(actionKey),
                "time", DATE.format(new Date(entry.time())),
                "world", entry.world(),
                "x", entry.x(), "y", entry.y(), "z", entry.z());
        return item(Material.PAPER, Msg.mm("<white>" + entry.playerName() + "</white>"), lore);
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

    // ----------------------------------------------------------------- utils

    private String facingText(BlockFace face) {
        String raw = msg.raw("gui.facing-" + face.name().toLowerCase());
        return raw == null ? face.name() : raw;
    }

    private String biomeOf(DoorRecord door) {
        World world = Bukkit.getWorld(door.world());
        if (world == null) return "?";
        String biome = world.getBlockAt(door.x(), door.y(), door.z()).getBiome().name();
        return biome.toLowerCase().replace('_', ' ');
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
