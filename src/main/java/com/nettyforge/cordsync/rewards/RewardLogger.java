package com.nettyforge.cordsync.rewards;

import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.rewards.storage.MySQLRewardLogStorage;
import com.nettyforge.cordsync.rewards.storage.RewardLogStorage;
import com.nettyforge.cordsync.rewards.storage.SQLiteRewardLogStorage;
import com.nettyforge.cordsync.rewards.storage.YamlRewardLogStorage;


public class RewardLogger {

    private final CordSync plugin;
    private final RewardLogStorage storage;

    public RewardLogger(CordSync plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("reward-logs.type", "YAML").toUpperCase();

        switch (type) {
            case "MYSQL" -> {
                plugin.getLogger().info("ğŸ’¾ Ã–dÃ¼l loglama yÃ¶ntemi: MySQL");
                storage = new MySQLRewardLogStorage(plugin);
            }
            case "SQLITE" -> {
                plugin.getLogger().info("ğŸ’¾ Ã–dÃ¼l loglama yÃ¶ntemi: SQLite");
                storage = new SQLiteRewardLogStorage(plugin);
            }
            default -> {
                plugin.getLogger().info("ğŸ’¾ Ã–dÃ¼l loglama yÃ¶ntemi: YAML (varsayÄ±lan)");
                storage = new YamlRewardLogStorage(plugin);
            }
        }
    }


    public void logReward(UUID player, String rewardType, String details) {
        if (storage == null) {
            plugin.getLogger().warning("RewardLogger aktif deÄŸil, loglama atlandÄ±.");
            return;
        }

        try {
            storage.log(player, rewardType, details);
        } catch (Exception e) {
            plugin.getLogger().severe("âŒ Ã–dÃ¼l loglama hatasÄ±: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void close() {
        if (storage != null) {
            storage.close();
        }
    }
}


