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
package top.midream.ddoor.visual;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import top.midream.ddoor.DDoorPlugin;
import top.midream.ddoor.door.DoorRecord;
import top.midream.ddoor.door.PairManager;
import top.midream.ddoor.door.PortalRegistry;

/**
 * Idle/pending particle scheduler. Frequency tiers by nearest player distance:
 * full inside range, half between range and 2x range, paused beyond.
 * Packets are sent per-recipient, never world-wide.
 */
public class ParticleTask extends BukkitRunnable {

    private final DDoorPlugin plugin;
    private final PortalRegistry registry;
    private final PairManager pairs;

    public ParticleTask(DDoorPlugin plugin, PortalRegistry registry, PairManager pairs) {
        this.plugin = plugin;
        this.registry = registry;
        this.pairs = pairs;
    }

    @Override
    public void run() {
        if (!plugin.cfg().idleParticle) return;
        for (DoorRecord door : registry.all()) {
            if (door.isPaired()) {
                render(door, 2, 0.15);
            } else if (isPending(door)) {
                render(door, 4, 0.35);
            }
        }
    }

    private boolean isPending(DoorRecord door) {
        for (var session : pairs.sessions().values()) {
            Block a = session.anchor();
            if (a.getWorld() != null && a.getWorld().getName().equals(door.world())
                    && a.getX() == door.x() && a.getY() == door.y() && a.getZ() == door.z()) {
                return true;
            }
        }
        return false;
    }

    private void render(DoorRecord door, int count, double spread) {
        World world = Bukkit.getWorld(door.world());
        if (world == null || !world.isChunkLoaded(door.x() >> 4, door.z() >> 4)) return;
        Location center = new Location(world, door.x() + 0.5, door.y() + 0.5, door.z() + 0.5);
        int range = plugin.cfg().particleRange;
        double rangeSq = (double) range * range;
        double range2Sq = rangeSq * 4;
        for (Player p : world.getPlayers()) {
            double distSq = p.getLocation().distanceSquared(center);
            if (distSq > range2Sq) continue;
            int n = distSq > rangeSq ? Math.max(1, count / 2) : count;
            p.spawnParticle(Particle.PORTAL, center, n, spread, 0.8, spread, 0.02);
            if (count > 2) {
                p.spawnParticle(Particle.END_ROD, center, 1, spread, 0.9, spread, 0.01);
            }
        }
    }
}
