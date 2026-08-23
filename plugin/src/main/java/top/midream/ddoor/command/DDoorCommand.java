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
package top.midream.ddoor.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.midream.ddoor.DDoorPlugin;
import top.midream.ddoor.door.DoorRecord;
import top.midream.ddoor.door.PairManager;
import top.midream.ddoor.door.PortalRegistry;
import top.midream.ddoor.util.Msg;

import java.util.ArrayList;
import java.util.List;

public class DDoorCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of(
            "list", "tp", "rename", "unlink", "delete", "key", "stats", "reload", "link", "gui");

    private final DDoorPlugin plugin;
    private final PortalRegistry registry;
    private final PairManager pairs;
    private final Msg msg;

    public DDoorCommand(DDoorPlugin plugin, PortalRegistry registry, PairManager pairs) {
        this.plugin = plugin;
        this.registry = registry;
        this.pairs = pairs;
        this.msg = plugin.msg();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            usage(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "list" -> list(sender, args);
            case "tp" -> tp(sender, args);
            case "rename" -> rename(sender, args);
            case "unlink" -> unlink(sender, args);
            case "delete" -> delete(sender, args);
            case "key" -> key(sender, args);
            case "stats" -> stats(sender);
            case "reload" -> reload(sender);
            case "link" -> link(sender);
            case "gui" -> gui(sender);
            default -> usage(sender);
        }
        return true;
    }

    private void gui(CommandSender sender) {
        if (!sender.hasPermission("ddoor.gui")) {
            msg.send(sender, "cmd.no-permission");
            return;
        }
        if (!(sender instanceof Player player)) {
            msg.send(sender, "cmd.player-only");
            return;
        }
        plugin.menu().open(player);
    }

    private void usage(CommandSender sender) {
        msg.send(sender, "cmd.usage-header");
        msg.sendRaw(sender, "cmd.usage-list", "flags",
                sender.hasPermission("ddoor.list.others") ? " [-a]" : "");
        msg.sendRaw(sender, "cmd.usage-tp");
        msg.sendRaw(sender, "cmd.usage-rename");
        msg.sendRaw(sender, "cmd.usage-unlink");
        msg.sendRaw(sender, "cmd.usage-link");
        msg.sendRaw(sender, "cmd.usage-gui");
        if (sender.hasPermission("ddoor.admin")) {
            msg.sendRaw(sender, "cmd.usage-admin");
        }
    }

    private void list(CommandSender sender, String[] args) {
        boolean all = args.length > 1 && args[1].equalsIgnoreCase("-a");
        if (all && !sender.hasPermission("ddoor.list.others")) {
            msg.send(sender, "cmd.no-permission");
            return;
        }
        List<DoorRecord> doors = new ArrayList<>();
        for (DoorRecord d : registry.all()) {
            if (!d.isPaired() || d.id().compareTo(d.pairedId()) < 0) continue;
            if (all || isSelf(sender, d)) doors.add(d);
        }
        if (doors.isEmpty()) {
            msg.send(sender, "cmd.list-empty");
            return;
        }
        msg.send(sender, "cmd.list-header", "count", doors.size());
        int i = 1;
        for (DoorRecord d : doors) {
            DoorRecord other = registry.byId(d.pairedId());
            if (other == null) continue;
            msg.send(sender, "cmd.list-row",
                    "index", i++,
                    "name", d.name(),
                    "world_a", d.world(),
                    "world_b", other.world(),
                    "uses", d.uses());
        }
    }

    private void tp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ddoor.tp")) {
            msg.send(sender, "cmd.no-permission");
            return;
        }
        if (!(sender instanceof Player player)) {
            msg.send(sender, "cmd.player-only");
            return;
        }
        if (args.length < 2) {
            usage(sender);
            return;
        }
        DoorRecord door = findDoor(sender, args[1]);
        if (door == null) return;
        plugin.engine().commandTeleport(player, door);
    }

    private void rename(CommandSender sender, String[] args) {
        if (args.length < 3) {
            usage(sender);
            return;
        }
        if (!(sender instanceof Player player)) {
            msg.send(sender, "cmd.player-only");
            return;
        }
        DoorRecord door = findDoor(sender, args[1]);
        if (door == null) return;
        if (!door.owner().equals(player.getUniqueId()) && !player.hasPermission("ddoor.admin")) {
            msg.send(sender, "cmd.no-permission");
            return;
        }
        String newName = String.join(" ", List.of(args).subList(2, args.length)).trim();
        if (newName.isEmpty() || newName.length() > 16) {
            msg.send(sender, "cmd.rename-length");
            return;
        }
        String old = door.name();
        DoorRecord other = registry.byId(door.pairedId());
        door.name(newName);
        pairs.persist(door);
        if (other != null) {
            other.name(newName);
            pairs.persist(other);
        }
        msg.send(sender, "cmd.renamed", "old", old, "name", newName);
    }

    private void unlink(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender);
            return;
        }
        if (!(sender instanceof Player player)) {
            msg.send(sender, "cmd.player-only");
            return;
        }
        DoorRecord door = findDoor(sender, args[1]);
        if (door == null) return;
        if (!door.owner().equals(player.getUniqueId()) && !player.hasPermission("ddoor.admin")) {
            msg.send(sender, "cmd.no-permission");
            return;
        }
        pairs.unlinkDoor(door, PairManager.CommandSource.of(sender), true);
    }

    private void delete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ddoor.admin")) {
            msg.send(sender, "cmd.no-permission");
            return;
        }
        if (args.length < 2) {
            usage(sender);
            return;
        }
        DoorRecord door = findDoor(sender, args[1]);
        if (door == null) return;
        pairs.removeDoor(door, false);
        msg.send(sender, "cmd.deleted", "name", door.name());
    }

    private void key(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ddoor.admin")) {
            msg.send(sender, "cmd.no-permission");
            return;
        }
        int amount = 1;
        Player target = sender instanceof Player p ? p : null;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                amount = 1;
            }
        }
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                msg.send(sender, "cmd.player-offline");
                return;
            }
        }
        if (target == null) {
            msg.send(sender, "cmd.player-only");
            return;
        }
        amount = Math.max(1, Math.min(64, amount));
        target.getInventory().addItem(plugin.keyItem().create(amount));
        msg.send(sender, "cmd.key-given", "player", target.getName(), "amount", amount);
        if (!target.equals(sender)) {
            msg.send(target, "cmd.key-received", "amount", amount);
        }
    }

    private void stats(CommandSender sender) {
        if (!sender.hasPermission("ddoor.admin")) {
            msg.send(sender, "cmd.no-permission");
            return;
        }
        msg.send(sender, "cmd.stats-header");
        msg.send(sender, "cmd.stats-total",
                "pairs", registry.totalPairs(),
                "doors", registry.all().size(),
                "uses", registry.totalUses());
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("ddoor.admin")) {
            msg.send(sender, "cmd.no-permission");
            return;
        }
        plugin.reloadAll();
        msg.send(sender, "cmd.reloaded");
    }

    private void link(CommandSender sender) {
        if (!sender.hasPermission("ddoor.link.command")) {
            msg.send(sender, "cmd.no-permission");
            return;
        }
        if (!(sender instanceof Player player)) {
            msg.send(sender, "cmd.player-only");
            return;
        }
        if (pairs.hasSession(player.getUniqueId())) {
            pairs.cancelSession(player);
            msg.send(player, "link.session-cancelled");
            return;
        }
        msg.send(player, "link.mode-start", "seconds", plugin.cfg().sessionTimeoutSeconds);
    }

    private DoorRecord findDoor(CommandSender sender, String name) {
        DoorRecord door = null;
        if (sender instanceof Player p) {
            door = registry.byNameOwner(name, p.getUniqueId());
        }
        if (door == null) door = registry.byName(name);
        if (door == null) {
            msg.send(sender, "cmd.not-found", "name", name);
        }
        return door;
    }

    private boolean isSelf(CommandSender sender, DoorRecord door) {
        return sender instanceof Player p && door.owner().equals(p.getUniqueId());
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : SUBS) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2) {
            String head = args[1].toLowerCase();
            if (args[0].equalsIgnoreCase("list") && sender.hasPermission("ddoor.list.others") && "-a".startsWith(head)) {
                out.add("-a");
            }
            for (DoorRecord d : registry.all()) {
                if (d.isPaired() && d.name().toLowerCase().startsWith(head)) {
                    if (isSelf(sender, d) || sender.hasPermission("ddoor.admin")
                            || List.of("tp", "rename", "unlink", "delete").contains(args[0].toLowerCase())) {
                        out.add(d.name());
                    }
                }
            }
        }
        return out;
    }
}
