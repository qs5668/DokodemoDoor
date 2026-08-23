package top.midream.ddoor.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class Msg {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Plugin plugin;
    private YamlConfiguration lang;
    private Component prefix;

    public Msg(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load(String language) {
        File dir = new File(plugin.getDataFolder(), "lang");
        File file = new File(dir, language + ".yml");
        if (!file.exists()) {
            try {
                plugin.saveResource("lang/" + language + ".yml", false);
            } catch (IllegalArgumentException missing) {
                plugin.getLogger().warning("no bundled lang '" + language + "', falling back to zh_CN");
                file = new File(dir, "zh_CN.yml");
            }
        }
        if (file.exists()) {
            lang = YamlConfiguration.loadConfiguration(file);
        } else {
            lang = new YamlConfiguration();
            plugin.getLogger().warning("lang file missing: " + language + ".yml — messages will be raw keys");
        }
        String p = raw("prefix");
        prefix = p == null ? Component.empty() : MM.deserialize(p);
    }

    public String raw(String key) {
        return lang.getString(key);
    }

    public org.bukkit.configuration.file.YamlConfiguration getConfiguration() {
        return lang;
    }

    public Component parse(String key, Object... kv) {
        String raw = lang.getString(key, key);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            raw = raw.replace("{" + kv[i] + "}", String.valueOf(kv[i + 1]));
        }
        return MM.deserialize(raw);
    }

    public void send(CommandSender to, String key, Object... kv) {
        to.sendMessage(prefix.append(parse(key, kv)));
    }

    public void sendRaw(CommandSender to, String key, Object... kv) {
        to.sendMessage(parse(key, kv));
    }

    public Component prefixed(String key, Object... kv) {
        return prefix.append(parse(key, kv));
    }

    public static Component mm(String text) {
        return MM.deserialize(text);
    }

    public static Map<String, String> kv(Object... pairs) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            m.put(String.valueOf(pairs[i]), String.valueOf(pairs[i + 1]));
        }
        return m;
    }
}
