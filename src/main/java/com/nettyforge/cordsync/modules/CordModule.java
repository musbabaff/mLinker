package com.nettyforge.cordsync.modules;

import com.nettyforge.cordsync.CordSync;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public abstract class CordModule {

    protected final CordSync plugin;
    private final String name;
    private boolean enabled = false;
    private FileConfiguration config;
    private File configFile;

    public CordModule(CordSync plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public abstract void onEnable();

    public abstract void onDisable();

    protected void loadConfig() {
        File moduleFolder = new File(plugin.getDataFolder(),
                "modules" + File.separator + name.toLowerCase().replace(" ", ""));
        if (!moduleFolder.exists()) {
            moduleFolder.mkdirs();
        }

        configFile = new File(moduleFolder, "config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                setupDefaultConfig();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create config for module " + name + ": " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    protected void setupDefaultConfig() {
        // Overlay this to write default config values inside the newly created module
        // config.yml
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    public void saveConfig() {
        if (config != null && configFile != null) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save config for module " + name + ": " + e.getMessage());
            }
        }
    }
}
