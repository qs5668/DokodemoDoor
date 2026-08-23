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
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import top.midream.ddoor.DDoorPlugin;
import top.midream.ddoor.util.Msg;

import java.util.ArrayList;
import java.util.List;

/**
 * Door key item factory and identification via PersistentDataContainer tag.
 * PDC byte tag: 1 = normal key, 2 = entity key. Entity keys additionally
 * carry an int "hours" tier — the lifetime granted to the DOOR PAIR upon
 * linking (the key itself never expires while unused). Craftable tiers are
 * fixed: 3/8/12/20/35/42/48h; each tier above 3h is crafted by merging two
 * keys of the previous tier. Admins may hand out any duration (0 = permanent).
 */
public final class KeyItem {

    public enum Type { NORMAL, ENTITY }

    /** Fixed craftable entity-key tiers, ascending. 3h is the base recipe. */
    public static final int[] ENTITY_TIERS = {3, 8, 12, 20, 35, 42, 48};
    /** Legacy v1.0.7 entity keys carry no tier tag — they grant 48h doors. */
    public static final int LEGACY_HOURS = 48;

    private static final byte TAG_NORMAL = 1;
    private static final byte TAG_ENTITY = 2;

    private final DDoorPlugin plugin;
    private final Msg msg;
    private final NamespacedKey tag;
    private final NamespacedKey hoursKey;

    public KeyItem(DDoorPlugin plugin, Msg msg) {
        this.plugin = plugin;
        this.msg = msg;
        this.tag = new NamespacedKey(plugin, "door_key");
        this.hoursKey = new NamespacedKey(plugin, "door_key_hours");
    }

    public ItemStack create(int amount) {
        return build(cfg().keyItem == null ? Material.AMETHYST_SHARD : cfg().keyItem,
                amount, Type.NORMAL, 0);
    }

    /** Entity key granting a pair lifetime of {@code hours} (0 = permanent). */
    public ItemStack createEntity(int amount, int hours) {
        return build(cfg().entityKeyItem == null ? Material.AMETHYST_SHARD : cfg().entityKeyItem,
                amount, Type.ENTITY, hours);
    }

    private ItemStack build(Material base, int amount, Type type, int hours) {
        ItemStack item = new ItemStack(base, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        boolean entity = type == Type.ENTITY;
        String hoursText = entity ? hoursText(hours) : "";
        plugin.text().name(meta, msg.parse(entity ? "key-entity.name" : "key.name"));
        List<Component> lore = new ArrayList<>();
        for (String line : msg.getConfiguration().getStringList(entity ? "key-entity.lore" : "key.lore")) {
            lore.add(Msg.mm(line.replace("{hours}", hoursText)));
        }
        plugin.text().lore(meta, lore);
        meta.setCustomModelData(entity ? cfg().entityKeyCustomModelData : cfg().keyCustomModelData);
        meta.getPersistentDataContainer().set(tag, PersistentDataType.BYTE, entity ? TAG_ENTITY : TAG_NORMAL);
        if (entity) {
            meta.getPersistentDataContainer().set(hoursKey, PersistentDataType.INTEGER, Math.max(0, hours));
        }
        item.setItemMeta(meta);
        return item;
    }

    public boolean isKey(ItemStack item) {
        return typeOf(item) != null;
    }

    public boolean isEntityKey(ItemStack item) {
        return typeOf(item) == Type.ENTITY;
    }

    public Type typeOf(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return null;
        Byte v = item.getItemMeta().getPersistentDataContainer().get(tag, PersistentDataType.BYTE);
        if (v == null) return null;
        return v == TAG_ENTITY ? Type.ENTITY : Type.NORMAL;
    }

    /** Pair lifetime in hours granted by this entity key (0 = permanent). */
    public int hoursOf(ItemStack item) {
        if (typeOf(item) != Type.ENTITY) return 0;
        Integer v = item.getItemMeta().getPersistentDataContainer().get(hoursKey, PersistentDataType.INTEGER);
        return v == null ? LEGACY_HOURS : v;
    }

    private String hoursText(int hours) {
        if (hours <= 0) return msg.raw("key-entity.permanent");
        return msg.raw("key-entity.hours", "hours", hours);
    }

    public void registerRecipe() {
        var cfg = plugin.cfg();
        if (cfg.keyCraftable) {
            ItemStack result = create(cfg.keyRecipeOutput);
            ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "door_key"), result);
            recipe.shape("AIA", "IEI", "AIA");
            recipe.setIngredient('A', Material.AMETHYST_SHARD);
            recipe.setIngredient('I', Material.IRON_INGOT);
            recipe.setIngredient('E', Material.ENDER_EYE);
            plugin.getServer().addRecipe(recipe);
        }
        if (cfg.entityKeyCraftable) {
            // base tier (3h): amethyst x4 + ender pearl x4 + ghast tear x1
            ItemStack result = createEntity(Math.max(1, cfg.entityKeyRecipeOutput), ENTITY_TIERS[0]);
            ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "entity_door_key"), result);
            recipe.shape("APA", "PGP", "APA");
            recipe.setIngredient('A', Material.AMETHYST_SHARD);
            recipe.setIngredient('P', Material.ENDER_PEARL);
            recipe.setIngredient('G', Material.GHAST_TEAR);
            plugin.getServer().addRecipe(recipe);

            // upgrade chain: two keys of tier N merge into one key of tier N+1
            for (int i = 1; i < ENTITY_TIERS.length; i++) {
                ItemStack from = createEntity(1, ENTITY_TIERS[i - 1]);
                ItemStack to = createEntity(1, ENTITY_TIERS[i]);
                ShapelessRecipe upgrade = new ShapelessRecipe(upgradeKey(ENTITY_TIERS[i]), to);
                upgrade.addIngredient(new RecipeChoice.ExactChoice(from));
                upgrade.addIngredient(new RecipeChoice.ExactChoice(from));
                plugin.getServer().addRecipe(upgrade);
            }
        }
    }

    public NamespacedKey recipeKey() {
        return new NamespacedKey(plugin, "door_key");
    }

    public NamespacedKey entityRecipeKey() {
        return new NamespacedKey(plugin, "entity_door_key");
    }

    private NamespacedKey upgradeKey(int toHours) {
        return new NamespacedKey(plugin, "entity_door_key_up_" + toHours);
    }

    /** All entity recipe keys (base + upgrades) for recipe-book discovery. */
    public List<NamespacedKey> entityRecipeKeys() {
        List<NamespacedKey> out = new ArrayList<>();
        out.add(entityRecipeKey());
        for (int i = 1; i < ENTITY_TIERS.length; i++) {
            out.add(upgradeKey(ENTITY_TIERS[i]));
        }
        return out;
    }

    private top.midream.ddoor.DDoorConfig cfg() {
        return plugin.cfg();
    }
}
