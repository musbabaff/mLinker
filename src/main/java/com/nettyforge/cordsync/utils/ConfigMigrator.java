package com.nettyforge.cordsync.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nettyforge.cordsync.CordSync;

/**
 * Automatically migrates config.yml when new keys are added.
 * Preserves existing user settings while adding missing defaults.
 */
public class ConfigMigrator {

    private final CordSync plugin;

    public ConfigMigrator(CordSync plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks the user's config against the default config shipped with the JAR.
     * Any missing keys are added with their default values.
     * Returns true if migration was performed.
     */
    public boolean migrate() {
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream == null)
            return false;

        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
        FileConfiguration userConfig = plugin.getConfig();

        int defaultVersion = defaultConfig.getInt("config-version", 1);
        int userVersion = userConfig.getInt("config-version", 0);

        if (userVersion >= defaultVersion) {
            return false; // Already up to date
        }

        int addedKeys = 0;
        addedKeys += mergeDefaults(userConfig, defaultConfig, "");

        // Update config version
        userConfig.set("config-version", defaultVersion);
        addedKeys++;

        if (addedKeys > 0) {
            try {
                userConfig.save(new File(plugin.getDataFolder(), "config.yml"));
                plugin.getLogger().info("⬆ Config migrated: " + addedKeys + " new keys added (v" + userVersion + " → v"
                        + defaultVersion + ")");
            } catch (IOException e) {
                plugin.getLogger().severe("❌ Config migration failed: " + e.getMessage());
                return false;
            }
        }

        return addedKeys > 0;
    }

    /**
     * Recursively merges missing keys from defaults into user config.
     */
    private int mergeDefaults(FileConfiguration userConfig, ConfigurationSection defaults, String path) {
        int added = 0;

        Set<String> keys = defaults.getKeys(false);
        for (String key : keys) {
            String fullPath = path.isEmpty() ? key : path + "." + key;

            if (defaults.isConfigurationSection(key)) {
                // Recurse into sections
                if (!userConfig.isConfigurationSection(fullPath)) {
                    // Entire section is missing — add it
                    userConfig.createSection(fullPath);
                }
                ConfigurationSection sub = defaults.getConfigurationSection(key);
                if (sub != null) {
                    added += mergeDefaults(userConfig, sub, fullPath);
                }
            } else {
                // Leaf key — add if missing
                if (!userConfig.contains(fullPath)) {
                    userConfig.set(fullPath, defaults.get(key));
                    added++;
                    plugin.getLogger().info("  + Added missing config key: " + fullPath);
                }
            }
        }

        return added;
    }
}
