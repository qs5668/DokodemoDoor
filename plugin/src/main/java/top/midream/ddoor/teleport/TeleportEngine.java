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
package top.midream.ddoor.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import top.midream.ddoor.DDoorConfig;
import top.midream.ddoor.DDoorPlugin;
import top.midream.ddoor.door.DoorBlocks;
import top.midream.ddoor.door.DoorRecord;
import top.midream.ddoor.door.PairManager;
import top.midream.ddoor.door.PortalRegistry;
import top.midream.ddoor.hook.VaultHook;
import top.midream.ddoor.util.Msg;
import top.midream.ddoor.visual.Fx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The six-gate teleport chain, executed on the main thread, microsecond-scale
 * per gate: cooldown -> permission -> counterpart validity -> destination
 * world -> landing safety -> teleport.
 */
public class TeleportEngine {

    private final DDoorPlugin plugin;
    private final PortalRegistry registry;
    private final PairManager pairs;
    private final Msg msg;
    private final VaultHook vault;

    private final Map<UUID, Long> lastTeleport = new HashMap<>();

    public TeleportEngine(DDoorPlugin plugin, PortalRegistry registry, PairManager pairs, Msg msg, VaultHook vault) {
        this.plugin = plugin;
        this.registry = registry;
        this.pairs = pairs;
        this.msg = msg;
        this.vault = vault;
    }

    private DDoorConfig cfg() {
        return plugin.cfg();
    }

    /** Player body entered the frame space of a registered door. */
    public void handleEnter(Player player, DoorRecord entry) {
        // gate 1: cooldown
        if (!player.hasPermission("ddoor.bypass.cooldown")) {
            Long last = lastTeleport.get(player.getUniqueId());
            long now = System.currentTimeMillis();
            if (last != null && now - last < cfg().cooldownSeconds * 1000L) {
                action(player, "tp.cooldown");
                return;
            }
        }
        // gate 2: use permission
        if (!player.hasPermission("ddoor.use")) {
            msg.send(player, "tp.no-permission");
            return;
        }
        // gate 3: counterpart door still exists as a door block
        DoorRecord dst = registry.byId(entry.pairedId());
        if (dst == null || !isDoorAt(dst)) {
            pairs.unlinkDoor(entry, null, true);
            msg.send(player, "tp.pair-broken");
            return;
        }
        // gate 4: destination world usable
        World world = Bukkit.getWorld(dst.world());
        if (world == null) {
            msg.send(player, "tp.world-denied");
            return;
        }
        if (!player.hasPermission("ddoor.bypass.world") && !pairs.worldAllowed(dst.world())) {
            msg.send(player, "tp.world-denied");
            return;
        }
        // gate 5: landing safety (with side probing)
        Block dstAnchor = world.getBlockAt(dst.x(), dst.y(), dst.z());
        Location landing = safeLanding(dstAnchor, dst.facing());
        if (landing == null) {
            msg.send(player, "tp.unsafe");
            return;
        }
        // economy charge
        if (cfg().economyEnabled && vault.isEnabled() && cfg().useCost > 0) {
            if (!vault.withdraw(player, cfg().useCost)) {
                msg.send(player, "tp.economy-failed", "cost", vault.format(cfg().useCost));
                return;
            }
        }
        // gate 6: execute
        execute(player, entry, dst, landing);
    }

    private void execute(Player player, DoorRecord entry, DoorRecord dst, Location landing) {
        lastTeleport.put(player.getUniqueId(), System.currentTimeMillis());

        Fx.tpGather(entryLoc(entry));
        if (cfg().fadeEffect) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 8, 0, false, false, false));
        }
        if (cfg().antiFallTicks > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,
                    cfg().antiFallTicks, 4, false, false, false));
        }

        Location to = landing.clone();
        to.setYaw(yawOf(dst.facing()));
        to.setPitch(0f);

        plugin.text().teleport(player, to).thenAccept(ok -> {
            if (Boolean.TRUE.equals(ok)) {
                Fx.tpArrive(to);
                entry.incrementUses();
                dst.incrementUses();
                pairs.persist(entry);
                pairs.persist(dst);
                if (cfg().economyEnabled && vault.isEnabled() && cfg().useCost > 0) {
                    msg.send(player, "tp.charged", "cost", vault.format(cfg().useCost));
                }
                action(player, "tp.success", "world", to.getWorld().getName());
            }
        });
    }

    private boolean isDoorAt(DoorRecord door) {
        World world = Bukkit.getWorld(door.world());
        if (world == null) return false;
        if (!world.isChunkLoaded(door.x() >> 4, door.z() >> 4)) return true; // cannot verify, keep the pair
        Block block = world.getBlockAt(door.x(), door.y(), door.z());
        if (!DoorBlocks.isDoor(block)) return false;
        Block anchor = DoorBlocks.anchorOf(block);
        return anchor != null && anchor.getX() == door.x() && anchor.getY() == door.y() && anchor.getZ() == door.z();
    }

    /**
     * Landing = one block outside the door along its facing, feet and head
     * passable, floor solid. On failure probe sideways (up to 2 blocks each).
     */
    private Location safeLanding(Block anchor, BlockFace facing) {
        Block front = anchor.getRelative(facing);
        if (isSafe(front)) return center(front);
        BlockFace left = rotateLeft(facing);
        BlockFace right = rotateRight(facing);
        for (int d = 1; d <= 2; d++) {
            Block probe = front.getRelative(left, d);
            if (isSafe(probe)) return center(probe);
            probe = front.getRelative(right, d);
            if (isSafe(probe)) return center(probe);
        }
        return null;
    }

    private boolean isSafe(Block feet) {
        if (!feet.isPassable()) return false;
        Block head = feet.getRelative(BlockFace.UP);
        if (!head.isPassable()) return false;
        return feet.getRelative(BlockFace.DOWN).getType().isSolid();
    }

    private Location center(Block block) {
        return new Location(block.getWorld(), block.getX() + 0.5, block.getY(), block.getZ() + 0.5);
    }

    private Location entryLoc(DoorRecord door) {
        World world = Bukkit.getWorld(door.world());
        return world == null ? null : new Location(world, door.x(), door.y(), door.z());
    }

    private void action(Player player, String key, Object... kv) {
        plugin.text().actionBar(player, msg.prefixed(key, kv));
    }

    /**
     * Direct teleport to a door's front (command/GUI path). Unlike the
     * walk-through chain this trusts the caller's permission checks.
     */
    public void commandTeleport(Player player, DoorRecord door) {
        World world = Bukkit.getWorld(door.world());
        if (world == null) {
            msg.send(player, "tp.world-denied");
            return;
        }
        BlockFace facing = door.facing();
        Location dest = new Location(world,
                door.x() + 0.5 + facing.getModX(),
                door.y(),
                door.z() + 0.5 + facing.getModZ());
        dest.setYaw(yawOf(facing));
        dest.setPitch(0f);
        lastTeleport.put(player.getUniqueId(), System.currentTimeMillis());
        plugin.text().teleport(player, dest).thenAccept(ok -> {
            if (Boolean.TRUE.equals(ok)) {
                door.incrementUses();
                pairs.persist(door);
                Fx.tpArrive(dest);
                action(player, "tp.success", "world", world.getName());
            }
        });
    }

    private static float yawOf(BlockFace face) {
        return switch (face) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> 270f;
            default -> 0f;
        };
    }

    private static BlockFace rotateLeft(BlockFace face) {
        return switch (face) {
            case SOUTH -> BlockFace.EAST;
            case WEST -> BlockFace.SOUTH;
            case NORTH -> BlockFace.WEST;
            case EAST -> BlockFace.NORTH;
            default -> face;
        };
    }

    private static BlockFace rotateRight(BlockFace face) {
        return switch (face) {
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            default -> face;
        };
    }
}
