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
import top.midream.ddoor.door.DoorBlocks;
import top.midream.ddoor.door.DoorRecord;
import top.midream.ddoor.door.PairManager;
import top.midream.ddoor.door.PortalRegistry;
import top.midream.ddoor.util.Msg;

import java.util.ArrayList;
import java.util.List;

/** Unlinks/removes door records the moment a door block disappears. */
public class BlockWatcher implements Listener {

    private final PairManager pairs;
    private final PortalRegistry registry;
    private final Msg msg;

    public BlockWatcher(PairManager pairs, PortalRegistry registry, Msg msg) {
        this.pairs = pairs;
        this.registry = registry;
        this.msg = msg;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        handleRemoved(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        handleRemoved(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> copy = new ArrayList<>(event.blockList());
        for (Block b : copy) handleRemoved(b);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        List<Block> copy = new ArrayList<>(event.blockList());
        for (Block b : copy) handleRemoved(b);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) handleRemoved(b);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) handleRemoved(b);
    }

    private void handleRemoved(Block block) {
        if (!DoorBlocks.isDoor(block)) return;
        Block anchor = DoorBlocks.anchorOf(block);
        if (anchor == null) return;
        DoorRecord door = registry.at(anchor.getWorld().getName(), anchor.getX(), anchor.getY(), anchor.getZ());
        if (door == null) return;
        pairs.removeDoor(door, true);
        Player owner = Bukkit.getPlayer(door.owner());
        if (owner != null && owner.isOnline()) {
            msg.send(owner, "break.notify-owner", "name", door.name());
        }
    }
}
