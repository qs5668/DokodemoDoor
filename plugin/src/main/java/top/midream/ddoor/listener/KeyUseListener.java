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

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import top.midream.ddoor.door.DoorBlocks;
import top.midream.ddoor.door.PairManager;
import top.midream.ddoor.key.KeyItem;

public class KeyUseListener implements Listener {

    private final PairManager pairs;
    private final KeyItem keyItem;

    public KeyUseListener(PairManager pairs, KeyItem keyItem) {
        this.pairs = pairs;
        this.keyItem = keyItem;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null || !DoorBlocks.isDoor(clicked)) return;

        Player player = event.getPlayer();
        ItemStack hand = event.getItem();
        boolean holdingKey = keyItem.isKey(hand);
        if (!holdingKey && !pairs.hasSession(player.getUniqueId())) return;
        if (!holdingKey && !player.hasPermission("ddoor.link.command")) return;

        event.setCancelled(true);
        pairs.handleKeyClick(player, clicked, holdingKey ? hand : null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pairs.cancelSession(event.getPlayer());
    }
}
