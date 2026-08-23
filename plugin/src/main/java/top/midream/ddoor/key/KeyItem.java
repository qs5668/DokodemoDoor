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
package top.midream.ddoor.key;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import top.midream.ddoor.DDoorPlugin;
import top.midream.ddoor.util.Msg;

import java.util.ArrayList;
import java.util.List;

/** Door key item factory and identification via PersistentDataContainer tag. */
public final class KeyItem {

    private final DDoorPlugin plugin;
    private final Msg msg;
    private final NamespacedKey tag;

    public KeyItem(DDoorPlugin plugin, Msg msg) {
        this.plugin = plugin;
        this.msg = msg;
        this.tag = new NamespacedKey(plugin, "door_key");
    }

    public ItemStack create(int amount) {
        var cfg = plugin.cfg();
        ItemStack item = new ItemStack(cfg.keyItem == null ? Material.AMETHYST_SHARD : cfg.keyItem, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            plugin.text().name(meta, msg.parse("key.name"));
            List<Component> lore = new ArrayList<>();
            for (String line : msg.getConfiguration().getStringList("key.lore")) {
                lore.add(Msg.mm(line));
            }
            plugin.text().lore(meta, lore);
            meta.setCustomModelData(cfg.keyCustomModelData);
            meta.getPersistentDataContainer().set(tag, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isKey(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        Byte v = item.getItemMeta().getPersistentDataContainer().get(tag, PersistentDataType.BYTE);
        return v != null && v == (byte) 1;
    }

    public void registerRecipe() {
        var cfg = plugin.cfg();
        if (!cfg.keyCraftable) return;
        ItemStack result = create(cfg.keyRecipeOutput);
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "door_key"), result);
        recipe.shape("AIA", "IEI", "AIA");
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('E', Material.ENDER_EYE);
        plugin.getServer().addRecipe(recipe);
    }

    public NamespacedKey recipeKey() {
        return new NamespacedKey(plugin, "door_key");
    }
}
