package top.midream.ddoor.key;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
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
        item.editMeta(meta -> {
            meta.displayName(msg.parse("key.name"));
            List<Component> lore = new ArrayList<>();
            for (String line : msg.getConfiguration().getStringList("key.lore")) {
                lore.add(Msg.mm(line));
            }
            meta.lore(lore);
            meta.setCustomModelData(cfg.keyCustomModelData);
            meta.getPersistentDataContainer().set(tag, PersistentDataType.BYTE, (byte) 1);
        });
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
}
