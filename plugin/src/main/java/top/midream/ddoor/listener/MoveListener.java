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
package top.midream.ddoor.listener;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import top.midream.ddoor.door.DoorRecord;
import top.midream.ddoor.door.PortalRegistry;
import top.midream.ddoor.teleport.TeleportEngine;

/**
 * PlayerMoveEvent gate: block-coordinate coarse filter first (one comparison),
 * then O(1) registry lookups over the player's AABB-covered blocks.
 */
public class MoveListener implements Listener {

    private static final double HALF_WIDTH = 0.31;
    private static final double EYE_HEIGHT = 1.79;

    private final PortalRegistry registry;
    private final TeleportEngine engine;

    public MoveListener(PortalRegistry registry, TeleportEngine engine) {
        this.registry = registry;
        this.engine = engine;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        // gate 1: coarse filter — ignore sub-block movement
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        if (to.getWorld() == null) return;
        String world = to.getWorld().getName();

        // gate 2: O(1) index lookups over AABB-covered blocks
        int minX = (int) Math.floor(to.getX() - HALF_WIDTH);
        int maxX = (int) Math.floor(to.getX() + HALF_WIDTH);
        int minZ = (int) Math.floor(to.getZ() - HALF_WIDTH);
        int maxZ = (int) Math.floor(to.getZ() + HALF_WIDTH);
        int minY = to.getBlockY();
        int maxY = (int) Math.floor(to.getY() + EYE_HEIGHT);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    DoorRecord door = registry.at(world, x, y, z);
                    if (door != null && door.isPaired()) {
                        engine.handleEnter(event.getPlayer(), door);
                        return;
                    }
                }
            }
        }
    }
}
