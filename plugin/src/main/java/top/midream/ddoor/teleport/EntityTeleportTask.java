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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import top.midream.ddoor.DDoorPlugin;
import top.midream.ddoor.door.DoorRecord;
import top.midream.ddoor.log.DoorLog;
import top.midream.ddoor.visual.Fx;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entity transport for entity-enabled pairs: a one-second sweep over loaded
 * entity doors, moving LivingEntities standing in the frame to the counterpart
 * landing. Players, armor stands, ridden vehicles and passengers are excluded;
 * a per-entity cooldown prevents bounce-back oscillation.
 */
public class EntityTeleportTask extends BukkitRunnable {

    private final DDoorPlugin plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private long lastSweep = 0L;

    public EntityTeleportTask(DDoorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        var cfg = plugin.cfg();
        if (!cfg.entityEnabled) return;
        long now = System.currentTimeMillis();

        for (DoorRecord door : plugin.registry().all()) {
            if (!door.isPaired() || !door.entities() || !door.enabled() || door.expired()) continue;
            DoorRecord dst = plugin.registry().byId(door.pairedId());
            if (dst == null || !dst.enabled() || dst.expired()) continue;
            World world = Bukkit.getWorld(door.world());
            if (world == null) continue;
            if (!world.isChunkLoaded(door.x() >> 4, door.z() >> 4)) continue;
            World dstWorld = Bukkit.getWorld(dst.world());
            // Entity#teleport is synchronous — a cold destination chunk would
            // load on the main thread. Skip and retry next sweep instead.
            if (dstWorld == null || !dstWorld.isChunkLoaded(dst.x() >> 4, dst.z() >> 4)) continue;

            Block anchor = world.getBlockAt(door.x(), door.y(), door.z());
            Block front = anchor.getRelative(door.facing());
            BoundingBox box = BoundingBox.of(anchor).union(BoundingBox.of(front)).expand(0.2, 0, 0.2);

            for (Entity e : world.getNearbyEntities(box)) {
                if (!(e instanceof LivingEntity le) || e instanceof Player || e instanceof ArmorStand) continue;
                if (e.isInsideVehicle() || !e.getPassengers().isEmpty()) continue;
                if (cfg.entityNamedOnly && e.getCustomName() == null) continue;
                Long last = cooldowns.get(e.getUniqueId());
                if (last != null && now - last < cfg.entityCooldownSeconds * 1000L) continue;

                Location landing = plugin.engine().safeLandingFor(dst);
                if (landing == null) continue; // mobs cannot read error messages — skip silently
                landing.setYaw(yawOf(dst.facing()));
                landing.setPitch(0f);

                cooldowns.put(e.getUniqueId(), now);
                if (e.teleport(landing)) {
                    door.incrementUses();
                    dst.incrementUses();
                    plugin.pairs().persist(door);
                    plugin.pairs().persist(dst);
                    plugin.logs().log(door, nameOf(e), DoorLog.ACTION_TELEPORT);
                    Fx.tpArrive(landing);
                }
            }
        }

        // keep the cooldown map bounded; sweep once a minute is plenty
        if (sweepDue(now)) {
            cooldowns.values().removeIf(last -> now - last > 600_000L);
        }
    }

    private boolean sweepDue(long now) {
        if (cooldowns.isEmpty() || now - lastSweep < 60_000L) return false;
        lastSweep = now;
        return true;
    }

    private static String nameOf(Entity e) {
        String custom = e.getCustomName();
        if (custom != null && !custom.isEmpty()) {
            return custom.length() > 16 ? custom.substring(0, 16) : custom;
        }
        return e.getType().name();
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
}
