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
import org.bukkit.inventory.EquipmentSlot;
import top.midream.ddoor.door.DoorRecord;
import top.midream.ddoor.door.PairManager;
import top.midream.ddoor.door.PortalRegistry;
import top.midream.ddoor.key.KeyItem;
import top.midream.ddoor.player.PlayerSettings;
import top.midream.ddoor.teleport.TeleportEngine;

/**
 * Click-to-teleport: players whose teleport mode is RIGHT_CLICK or
 * LEFT_CLICK trigger the door by clicking it instead of walking in.
 * Key pairing and vanilla interactions (sneak-use) always win.
 */
public class InteractListener implements Listener {

    private final PlayerSettings settings;
    private final PortalRegistry registry;
    private final TeleportEngine engine;
    private final KeyItem keyItem;
    private final PairManager pairs;

    public InteractListener(PlayerSettings settings, PortalRegistry registry,
                            TeleportEngine engine, KeyItem keyItem, PairManager pairs) {
        this.settings = settings;
        this.registry = registry;
        this.engine = engine;
        this.keyItem = keyItem;
        this.pairs = pairs;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Player player = event.getPlayer();
        PlayerSettings.Mode mode = settings.modeOf(player.getUniqueId());
        if (mode == PlayerSettings.Mode.WALK) return;
        if (mode == PlayerSettings.Mode.RIGHT_CLICK && action != Action.RIGHT_CLICK_BLOCK) return;
        if (mode == PlayerSettings.Mode.LEFT_CLICK && action != Action.LEFT_CLICK_BLOCK) return;
        if (keyItem.isKey(event.getItem())) return;                 // pairing flow takes precedence
        if (pairs.hasSession(player.getUniqueId())) return;         // pairing session in progress
        if (player.isSneaking()) return;                            // sneak = vanilla behavior

        DoorRecord door = registry.at(clicked.getWorld().getName(),
                clicked.getX(), clicked.getY(), clicked.getZ());
        if (door == null || !door.isPaired()) return;

        event.setCancelled(true); // suppress door toggle / break start
        engine.handleEnter(player, door);
    }
}
