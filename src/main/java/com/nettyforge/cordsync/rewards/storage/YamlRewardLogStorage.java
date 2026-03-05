package com.nettyforge.cordsync.rewards.storage;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nettyforge.cordsync.CordSync;

public class YamlRewardLogStorage implements RewardLogStorage {

    private final CordSync plugin;
    private final File file;
    private final FileConfiguration data;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public YamlRewardLogStorage(CordSync plugin) {
        this.plugin = plugin;
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File oldFile = new File(plugin.getDataFolder(), "reward-logs.yml");
        this.file = new File(dataFolder, "reward-logs.yml");

        if (oldFile.exists() && !this.file.exists()) {
            oldFile.renameTo(this.file);
            plugin.getLogger().info("📦 Migrated reward-logs.yml to data/ folder.");
        }

        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
                plugin.getLogger().info("✅ reward-logs.yml created.");
            } catch (IOException e) {
                plugin.getLogger().severe("❌ Failed to create reward-logs.yml: " + e.getMessage());
            }
        }

        this.data = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void log(UUID player, String rewardType, String details) {
        String uuidStr = player.toString();
        String timestamp = dateFormat.format(new Date());
        String path = "logs." + uuidStr + "." + timestamp;

        data.set(path + ".reward-type", rewardType);
        data.set(path + ".details", details);

        save();
        plugin.getLogger().info("🧾 [" + rewardType + "] reward logged: " + uuidStr);
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("❌ Failed to save reward-logs.yml: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        save();
    }
}
