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
package top.midream.ddoor.log;

/** One interaction event on a door: who, what, when, where. */
public final class DoorLog {

    public static final String ACTION_TELEPORT = "TELEPORT";
    public static final String ACTION_LINK = "LINK";
    public static final String ACTION_UNLINK = "UNLINK";
    public static final String ACTION_BREAK = "BREAK";

    private final java.util.UUID doorId;
    private final String doorName;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final String playerName;
    private final String action;
    private final long time;

    public DoorLog(java.util.UUID doorId, String doorName, String world, int x, int y, int z,
                   String playerName, String action, long time) {
        this.doorId = doorId;
        this.doorName = doorName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.playerName = playerName;
        this.action = action;
        this.time = time;
    }

    public java.util.UUID doorId() { return doorId; }
    public String doorName() { return doorName; }
    public String world() { return world; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public String playerName() { return playerName; }
    public String action() { return action; }
    public long time() { return time; }
}
