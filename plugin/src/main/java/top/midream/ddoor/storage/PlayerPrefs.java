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
package top.midream.ddoor.storage;

/** Persisted per-player preferences as loaded from the settings table. */
public record PlayerPrefs(String mode, boolean simpleInfo) {
}
