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
package top.midream.ddoor.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Identifies a DoorMenu inventory and carries its per-open session state. */
public final class DoorMenuHolder implements InventoryHolder {

    private final UUID viewer;
    private final int page;
    private final int pages;
    private final Map<Integer, UUID> slotDoors = new HashMap<>();
    private Inventory inventory;

    public DoorMenuHolder(UUID viewer, int page, int pages) {
        this.viewer = viewer;
        this.page = page;
        this.pages = pages;
    }

    void map(int slot, UUID doorId) {
        slotDoors.put(slot, doorId);
    }

    public UUID doorAt(int slot) {
        return slotDoors.get(slot);
    }

    public UUID viewer() {
        return viewer;
    }

    public int page() {
        return page;
    }

    public int pages() {
        return pages;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void inventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
