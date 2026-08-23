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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Door key item factory and identification via PersistentDataContainer tag.
 * PDC byte tag: 1 = normal key, 2 = entity key. Entity keys additionally
 * carry a creation timestamp and expire after entity-key.expire-hours.
 */
public final class KeyItem {

    public enum Type { NORMAL, ENTITY }

    private static final byte TAG_NORMAL = 1;
    private static final byte TAG_ENTITY = 2;

    private final DDoorPlugin plugin;
    private final Msg msg;
    private final NamespacedKey tag;
    private final NamespacedKey createdAt;

    public KeyItem(DDoorPlugin plugin, Msg msg) {
        this.plugin = plugin;
        this.msg = msg;
        this.tag = new NamespacedKey(plugin, "door_key");
        this.createdAt = new NamespacedKey(plugin, "door_key_created");
    }

    public ItemStack create(int amount) {
        return build(cfg().keyItem == null ? Material.AMETHYST_SHARD : cfg().keyItem,
                amount, Type.NORMAL);
    }

    public ItemStack createEntity(int amount) {
        return build(cfg().entityKeyItem == null ? Material.AMETHYST_SHARD : cfg().entityKeyItem,
                amount, Type.ENTITY);
    }

    private ItemStack build(Material base, int amount, Type type) {
        ItemStack item = new ItemStack(base, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        boolean entity = type == Type.ENTITY;
        plugin.text().name(meta, msg.parse(entity ? "key-entity.name" : "key.name"));
        List<Component> lore = new ArrayList<>();
        long created = System.currentTimeMillis();
        String expireText = expiryText(entity, created);
        for (String line : msg.getConfiguration().getStringList(entity ? "key-entity.lore" : "key.lore")) {
            lore.add(Msg.mm(line.replace("{expire}", expireText)));
        }
        plugin.text().lore(meta, lore);
        meta.setCustomModelData(entity ? cfg().entityKeyCustomModelData : cfg().keyCustomModelData);
        meta.getPersistentDataContainer().set(tag, PersistentDataType.BYTE, entity ? TAG_ENTITY : TAG_NORMAL);
        if (entity) {
            meta.getPersistentDataContainer().set(createdAt, PersistentDataType.LONG, created);
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

    /** Absolute expiry timestamp, or 0 for keys that never expire. */
    public long expiryAt(ItemStack item) {
        if (typeOf(item) != Type.ENTITY) return 0L;
        int hours = cfg().entityKeyExpireHours;
        if (hours <= 0) return 0L;
        Long created = item.getItemMeta().getPersistentDataContainer().get(createdAt, PersistentDataType.LONG);
        if (created == null) return 0L;
        return created + hours * 3600_000L;
    }

    public boolean expired(ItemStack item) {
        long at = expiryAt(item);
        return at > 0 && System.currentTimeMillis() > at;
    }

    private String expiryText(boolean entity, long created) {
        if (!entity) return "";
        int hours = cfg().entityKeyExpireHours;
        if (hours <= 0) return msg.raw("key-entity.never-expire");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        return fmt.format(new Date(created + hours * 3600_000L));
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
            ItemStack result = createEntity(cfg.entityKeyRecipeOutput);
            ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "entity_door_key"), result);
            recipe.shape("APA", "PGP", "APA");
            recipe.setIngredient('A', Material.AMETHYST_SHARD);
            recipe.setIngredient('P', Material.ENDER_PEARL);
            recipe.setIngredient('G', Material.GHAST_TEAR);
            plugin.getServer().addRecipe(recipe);
        }
    }

    public NamespacedKey recipeKey() {
        return new NamespacedKey(plugin, "door_key");
    }

    public NamespacedKey entityRecipeKey() {
        return new NamespacedKey(plugin, "entity_door_key");
    }

    private top.midream.ddoor.DDoorConfig cfg() {
        return plugin.cfg();
    }
}
