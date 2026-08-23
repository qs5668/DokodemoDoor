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

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import top.midream.ddoor.DDoorPlugin;
import top.midream.ddoor.door.DoorBlocks;
import top.midream.ddoor.door.DoorRecord;
import top.midream.ddoor.door.PairManager;
import top.midream.ddoor.door.PortalRegistry;
import top.midream.ddoor.util.Msg;

import java.util.ArrayList;
import java.util.List;

/** Unlinks/removes door records the moment a door block disappears. */
public class BlockWatcher implements Listener {

    private final DDoorPlugin plugin;
    private final PairManager pairs;
    private final PortalRegistry registry;
    private final Msg msg;

    public BlockWatcher(DDoorPlugin plugin, PairManager pairs, PortalRegistry registry, Msg msg) {
        this.plugin = plugin;
        this.pairs = pairs;
        this.registry = registry;
        this.msg = msg;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        handleRemoved(event.getBlock(), event.getPlayer().getName());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        handleRemoved(event.getBlock(), null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> copy = new ArrayList<>(event.blockList());
        for (Block b : copy) handleRemoved(b, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        List<Block> copy = new ArrayList<>(event.blockList());
        for (Block b : copy) handleRemoved(b, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) handleRemoved(b, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) handleRemoved(b, null);
    }

    private void handleRemoved(Block block, String actor) {
        if (!DoorBlocks.isDoor(block)) return;
        Block anchor = DoorBlocks.anchorOf(block);
        if (anchor == null) return;
        DoorRecord door = registry.at(anchor.getWorld().getName(), anchor.getX(), anchor.getY(), anchor.getZ());
        if (door == null) return;
        plugin.logs().log(door, actor == null ? "World" : actor, top.midream.ddoor.log.DoorLog.ACTION_BREAK);
        pairs.removeDoor(door, true);
        Player owner = Bukkit.getPlayer(door.owner());
        if (owner != null && owner.isOnline()) {
            msg.send(owner, "break.notify-owner", "name", door.name());
        }
    }
}
