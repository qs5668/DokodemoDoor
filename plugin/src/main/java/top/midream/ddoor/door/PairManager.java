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
package top.midream.ddoor.door;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.midream.ddoor.DDoorConfig;
import top.midream.ddoor.DDoorPlugin;
import top.midream.ddoor.hook.VaultHook;
import top.midream.ddoor.storage.WriteQueue;
import top.midream.ddoor.util.Msg;
import top.midream.ddoor.visual.Fx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Pairing sessions and door-pair lifecycle: key right-click flow,
 * pair creation, unlink with cooldown, and door removal.
 */
public class PairManager {

    public record Session(Block anchor, long deadline, boolean freeKey) {}

    private final DDoorPlugin plugin;
    private final PortalRegistry registry;
    private final WriteQueue queue;
    private final Msg msg;
    private final VaultHook vault;

    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<Long, Long> unlinkCooldowns = new HashMap<>();

    public PairManager(DDoorPlugin plugin, PortalRegistry registry, WriteQueue queue, Msg msg, VaultHook vault) {
        this.plugin = plugin;
        this.registry = registry;
        this.queue = queue;
        this.msg = msg;
        this.vault = vault;
    }

    private DDoorConfig cfg() {
        return plugin.cfg();
    }

    /** Entry point for a key right-click on a block already known to be a door. */
    public void handleKeyClick(Player player, Block clicked, ItemStack key) {
        Block anchor = DoorBlocks.anchorOf(clicked);
        if (anchor == null || DoorBlocks.isFloatingUpper(clicked)) {
            msg.send(player, "link.not-a-door");
            return;
        }
        if (!worldAllowed(anchor.getWorld().getName()) && !player.hasPermission("ddoor.bypass.world")) {
            msg.send(player, "link.world-denied");
            return;
        }

        DoorRecord existing = registry.at(anchor.getWorld().getName(), anchor.getX(), anchor.getY(), anchor.getZ());
        if (existing != null && existing.isPaired()) {
            // break old pair first, then this door can be re-linked
            unlinkDoor(existing, CommandSource.of(player), false);
            msg.send(player, "link.already-paired-confirm");
            existing = registry.at(anchor.getWorld().getName(), anchor.getX(), anchor.getY(), anchor.getZ());
        }
        if (existing == null && anchorInCooldown(anchor)) {
            msg.send(player, "link.unlink-cooldown", "seconds",
                    Math.max(1, (int) ((anchorCooldownUntil(anchor) - System.currentTimeMillis()) / 1000)));
            return;
        }

        Session session = sessions.get(player.getUniqueId());
        if (session == null || System.currentTimeMillis() > session.deadline()) {
            int limit = limitOf(player);
            if (limit >= 0 && registry.pairsOf(player.getUniqueId()) >= limit) {
                msg.send(player, "link.limit-reached", "limit", limit);
                return;
            }
            sessions.put(player.getUniqueId(), new Session(anchor, System.currentTimeMillis()
                    + cfg().sessionTimeoutSeconds * 1000L, key == null));
            msg.send(player, "link.first-selected", "seconds", cfg().sessionTimeoutSeconds);
            Fx.pendingSelect(anchor.getLocation());
            return;
        }

        // second door
        if (isSameDoor(session.anchor(), anchor)) {
            msg.send(player, "link.same-door");
            return;
        }
        int limit = limitOf(player);
        if (limit >= 0 && registry.pairsOf(player.getUniqueId()) >= limit) {
            sessions.remove(player.getUniqueId());
            msg.send(player, "link.limit-reached", "limit", limit);
            return;
        }
        if (cfg().economyEnabled && vault.isEnabled() && cfg().createCost > 0
                && !vault.withdraw(player, cfg().createCost)) {
            sessions.remove(player.getUniqueId());
            msg.send(player, "tp.economy-failed", "cost", vault.format(cfg().createCost));
            return;
        }
        completePair(player, session, anchor, key);
    }

    private void completePair(Player player, Session session, Block second, ItemStack key) {
        sessions.remove(player.getUniqueId());

        DoorRecord first = ensureRecord(player, session.anchor());
        DoorRecord secondRec = ensureRecord(player, second);
        first.pairedId(secondRec.id());
        secondRec.pairedId(first.id());

        String pairName = defaultName(player);
        first.name(pairName);
        secondRec.name(pairName);

        persist(first);
        persist(secondRec);

        if (!session.freeKey() && key != null) {
            key.setAmount(key.getAmount() - 1);
        }
        Fx.pairSuccess(locOf(first), locOf(secondRec), cfg().soundOnTeleport);
        msg.send(player, "link.second-selected", "name", pairName);
    }

    private DoorRecord ensureRecord(Player owner, Block anchor) {
        String world = anchor.getWorld().getName();
        DoorRecord rec = registry.at(world, anchor.getX(), anchor.getY(), anchor.getZ());
        if (rec != null) {
            return rec;
        }
        rec = new DoorRecord(
                UUID.randomUUID(),
                defaultName(owner),
                owner.getUniqueId(),
                world,
                anchor.getX(), anchor.getY(), anchor.getZ(),
                DoorBlocks.facingOf(anchor),
                null,
                System.currentTimeMillis(),
                0);
        registry.register(rec, DoorBlocks.extraBlocksOf(anchor));
        persist(rec);
        return rec;
    }

    private String defaultName(Player owner) {
        String base = owner.getName() + "的门";
        if (registry.byNameOwner(base, owner.getUniqueId()) == null) return base;
        for (int i = 2; i < 100; i++) {
            String candidate = base + i;
            if (registry.byNameOwner(candidate, owner.getUniqueId()) == null) return candidate;
        }
        return base + UUID.randomUUID().toString().substring(0, 4);
    }

    /** Unlink a door pair. Records stay for future re-linking. */
    public void unlinkDoor(DoorRecord door, CommandSource notify, boolean applyCooldown) {
        if (!door.isPaired()) return;
        DoorRecord other = registry.byId(door.pairedId());
        door.pairedId(null);
        persist(door);
        if (other != null) {
            other.pairedId(null);
            persist(other);
            notifyOwner(other);
            if (applyCooldown) putAnchorCooldown(other);
        }
        if (applyCooldown) putAnchorCooldown(door);
        if (notify != null) {
            msg.send(notify.sender(), "unlink.done", "name", door.name());
        }
    }

    /** Remove a door record entirely (block gone or admin delete). Counterpart becomes unpaired. */
    public void removeDoor(DoorRecord door, boolean notifyOwner) {
        if (door.isPaired()) {
            DoorRecord other = registry.byId(door.pairedId());
            if (other != null) {
                other.pairedId(null);
                persist(other);
                if (notifyOwner) notifyOwner(other);
                putAnchorCooldown(other);
            }
        }
        registry.unregister(door);
        queue.submit("delete-" + door.id(), () -> {
            try {
                plugin.store().delete(door.id());
            } catch (Exception e) {
                plugin.getLogger().severe("failed to delete door " + door.id() + ": " + e.getMessage());
            }
        });
        putAnchorCooldown(door);
    }

    private void notifyOwner(DoorRecord door) {
        Player owner = Bukkit.getPlayer(door.owner());
        if (owner != null && owner.isOnline()) {
            msg.send(owner, "unlink.notify-owner", "name", door.name());
        }
    }

    public void cancelSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public boolean hasSession(UUID player) {
        return sessions.containsKey(player);
    }

    public Map<UUID, Session> sessions() {
        return sessions;
    }

    /** Periodic cleanup of expired sessions and cooldowns. */
    public void tickCleanup() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Long, Long>> cd = unlinkCooldowns.entrySet().iterator();
        while (cd.hasNext()) {
            if (cd.next().getValue() < now) cd.remove();
        }
        sessions.entrySet().removeIf(e -> e.getValue().deadline() < now);
    }

    public int limitOf(Player player) {
        if (player.hasPermission("ddoor.admin")) return -1;
        int limit = cfg().defaultLimit;
        for (int n = 100; n >= 1; n--) {
            if (player.hasPermission("ddoor.limit." + n)) {
                return Math.max(limit, n);
            }
        }
        return limit;
    }

    public boolean worldAllowed(String world) {
        boolean listed = cfg().worldList.contains(world);
        return "whitelist".equalsIgnoreCase(cfg().worldMode) ? listed : !listed;
    }

    private static long anchorKey(Block anchor) {
        return anchor.getX() * 347L + anchor.getY() * 9973L + anchor.getZ() * 131071L
                + anchor.getWorld().getName().hashCode() * 7919L;
    }

    private static long anchorKey(DoorRecord door) {
        return door.x() * 347L + door.y() * 9973L + door.z() * 131071L
                + door.world().hashCode() * 7919L;
    }

    private void putAnchorCooldown(DoorRecord door) {
        unlinkCooldowns.put(anchorKey(door), System.currentTimeMillis() + cfg().unlinkCooldownSeconds * 1000L);
    }

    private boolean anchorInCooldown(Block anchor) {
        Long until = unlinkCooldowns.get(anchorKey(anchor));
        return until != null && until > System.currentTimeMillis();
    }

    private long anchorCooldownUntil(Block anchor) {
        Long until = unlinkCooldowns.get(anchorKey(anchor));
        return until == null ? 0 : until;
    }

    public void persist(DoorRecord door) {
        queue.submit("upsert-" + door.id(), () -> {
            try {
                plugin.store().upsert(door);
            } catch (Exception e) {
                plugin.getLogger().severe("failed to save door " + door.id() + ": " + e.getMessage());
            }
        });
    }

    private boolean isSameDoor(Block a, Block b) {
        if (a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ()
                && a.getWorld().equals(b.getWorld())) return true;
        return a.getWorld().equals(b.getWorld()) && a.getLocation().distanceSquared(b.getLocation()) <= 1.01;
    }

    private Location locOf(DoorRecord door) {
        var world = Bukkit.getWorld(door.world());
        return world == null ? null : new Location(world, door.x(), door.y(), door.z());
    }

    /** Adapter so unlink() can notify a player or console. */
    public interface CommandSource {
        CommandSender sender();

        static CommandSource of(CommandSender s) {
            return () -> s;
        }
    }
}
