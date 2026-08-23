package top.midream.ddoor.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.midream.ddoor.DDoorPlugin;
import top.midream.ddoor.door.PortalRegistry;

public class PapiHook extends PlaceholderExpansion {

    private final DDoorPlugin plugin;
    private final PortalRegistry registry;

    public PapiHook(DDoorPlugin plugin, PortalRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ddoor";
    }

    @Override
    public @NotNull String getAuthor() {
        return "qs5668";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String getPlugin() {
        return null;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return params.equalsIgnoreCase("total") ? String.valueOf(registry.totalPairs()) : null;
        }
        switch (params.toLowerCase()) {
            case "uses" -> {
                return String.valueOf(registry.usesOf(player.getUniqueId()));
            }
            case "pairs" -> {
                return String.valueOf(registry.pairsOf(player.getUniqueId()));
            }
            case "limit" -> {
                return String.valueOf(plugin.pairs().limitOf(player));
            }
            case "total" -> {
                return String.valueOf(registry.totalPairs());
            }
            default -> {
                return null;
            }
        }
    }
}
