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
package top.midream.ddoor.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import top.midream.ddoor.platform.TextAdapter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class Msg {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Plugin plugin;
    private final TextAdapter platform;
    private YamlConfiguration lang;
    private Component prefix;

    public Msg(Plugin plugin, TextAdapter platform) {
        this.plugin = plugin;
        this.platform = platform;
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
        // Bundled file serves as defaults so installs upgraded from older versions
        // pick up newly added keys without overwriting local edits.
        java.io.InputStream bundled = plugin.getResource("lang/" + language + ".yml");
        if (bundled == null) bundled = plugin.getResource("lang/zh_CN.yml");
        if (bundled != null) {
            lang.setDefaults(YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(bundled, java.nio.charset.StandardCharsets.UTF_8)));
        }
        String p = raw("prefix");
        prefix = p == null ? Component.empty() : MM.deserialize(p);
    }

    public String raw(String key, Object... kv) {
        String raw = lang.getString(key);
        if (raw == null || kv.length == 0) return raw;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            raw = raw.replace("{" + kv[i] + "}", String.valueOf(kv[i + 1]));
        }
        return raw;
    }

    public org.bukkit.configuration.file.YamlConfiguration getConfiguration() {
        return lang;
    }

    public Component parse(String key, Object... kv) {
        // One-arg getString resolves through setDefaults; the two-arg overload
        // treats the fallback literal as the only default and skips bundled keys.
        String raw = raw(key);
        if (raw == null) raw = key;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            raw = raw.replace("{" + kv[i] + "}", String.valueOf(kv[i + 1]));
        }
        return MM.deserialize(raw);
    }

    public java.util.List<Component> parseList(String key, Object... kv) {
        java.util.List<Component> out = new java.util.ArrayList<>();
        for (String line : lang.getStringList(key)) {
            for (int i = 0; i + 1 < kv.length; i += 2) {
                line = line.replace("{" + kv[i] + "}", String.valueOf(kv[i + 1]));
            }
            out.add(MM.deserialize(line));
        }
        return out;
    }

    public void send(CommandSender to, String key, Object... kv) {
        platform.send(to, prefixed(key, kv));
    }

    public void sendRaw(CommandSender to, String key, Object... kv) {
        platform.send(to, parse(key, kv));
    }

    public void mini(CommandSender to, String text) {
        platform.send(to, mm(text));
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
