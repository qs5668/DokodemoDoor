package top.midream.ddoor.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import top.midream.ddoor.DDoorPlugin;
import top.midream.ddoor.door.DoorRecord;
import top.midream.ddoor.door.DoorBlocks;
import top.midream.ddoor.door.PairManager;
import top.midream.ddoor.door.PortalRegistry;
import top.midream.ddoor.util.Msg;

import java.util.ArrayList;
import java.util.List;

public class DDoorCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of(
            "list", "tp", "rename", "unlink", "delete", "key", "stats", "reload", "link");

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
            default -> usage(sender);
        }
        return true;
    }

    private void usage(CommandSender sender) {
        msg.send(sender, "cmd.usage-header");
        sender.sendMessage(Msg.mm("<gray>/ddoor list " + (sender.hasPermission("ddoor.list.others") ? "[-a]" : "")
                + " <dark_gray>— 门对列表</dark_gray></gray>"));
        sender.sendMessage(Msg.mm("<gray>/ddoor tp <门名> <dark_gray>— 传送到门</dark_gray></gray>"));
        sender.sendMessage(Msg.mm("<gray>/ddoor rename <门名> <新名> <dark_gray>— 重命名</dark_gray></gray>"));
        sender.sendMessage(Msg.mm("<gray>/ddoor unlink <门名> <dark_gray>— 解除配对</dark_gray></gray>"));
        sender.sendMessage(Msg.mm("<gray>/ddoor link <dark_gray>— 免钥匙绑定模式（右键两扇门）</dark_gray></gray>"));
        if (sender.hasPermission("ddoor.admin")) {
            sender.sendMessage(Msg.mm("<gray>/ddoor delete <门名> · key [数量] [玩家] · stats · reload</gray>"));
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
        var world = Bukkit.getWorld(door.world());
        if (world == null) {
            msg.send(sender, "tp.world-denied");
            return;
        }
        BlockFace facing = door.facing();
        Location dest = new Location(world,
                door.x() + 0.5 + facing.getModX(),
                door.y(),
                door.z() + 0.5 + facing.getModZ());
        dest.setYaw(yawOf(facing));
        player.teleportAsync(dest);
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
            sender.sendMessage(Msg.mm("<red>门名长度需在 1~16 字符之间</red>"));
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
        sender.sendMessage(Msg.mm("<green>已删除门记录：" + door.name() + "</green>"));
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
                sender.sendMessage(Msg.mm("<red>玩家不在线</red>"));
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
        sender.sendMessage(Msg.mm(
                "<green>免钥匙绑定模式：右键第一扇门，再右键第二扇门。再次输入 /ddoor link 取消。</green>"));
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

    private float yawOf(BlockFace face) {
        return switch (face) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> 270f;
            default -> 0f;
        };
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
