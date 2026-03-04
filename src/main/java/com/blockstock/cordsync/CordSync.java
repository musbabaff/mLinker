package com.blockstock.cordsync;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.blockstock.cordsync.commands.LinkCommand;
import com.blockstock.cordsync.commands.UnlinkCommand;
import com.blockstock.cordsync.commands.CordSyncInfoCommand;
import com.blockstock.cordsync.commands.CordSyncReloadCommand;
import com.blockstock.cordsync.commands.CordSyncReverifyCommand;
import com.blockstock.cordsync.discord.DiscordBot;
import com.blockstock.cordsync.managers.LinkManager;
import com.blockstock.cordsync.managers.RewardManager;
import com.blockstock.cordsync.rewards.RewardLogManager;
import com.blockstock.cordsync.storage.Migratable;
import com.blockstock.cordsync.storage.MySQLStorage;
import com.blockstock.cordsync.storage.SQLiteStorage;
import com.blockstock.cordsync.storage.StorageProvider;
import com.blockstock.cordsync.storage.YamlStorage;
import com.blockstock.cordsync.tasks.ReverifyTask;
import com.blockstock.cordsync.utils.MessageUtil;
import com.blockstock.cordsync.utils.UpdateChecker;

public class CordSync extends JavaPlugin {

    private static CordSync instance;
    private LinkManager linkManager;
    private StorageProvider storageProvider;
    private DiscordBot discordBot;
    private RewardManager rewardManager;
    private RewardLogManager rewardLogManager;
    private ReverifyTask reverifyTask;

    @Override
    public void onEnable() {
        instance = this;

        // Konsola havalÄ± ASCII logomuzu yazdÄ±ran metot
        printLogo();

        saveDefaultConfig();

        // Auto-migrate config (add missing keys without overwriting)
        new com.blockstock.cordsync.utils.ConfigMigrator(this).migrate();
        FileConfiguration config = getConfig();

        MessageUtil.load(this);

        int expire = config.getInt("link.expire-seconds", 300);
        linkManager = new LinkManager(expire);

        String newType = config.getString("storage.type", "YAML").toUpperCase();
        String lastType = config.getString("storage.last-storage", "YAML").toUpperCase();

        storageProvider = createStorage(newType);

        if (!newType.equalsIgnoreCase(lastType)) {
            getLogger().warning(MessageUtil.get("storage.type-changed")
                    .replace("{old}", lastType)
                    .replace("{new}", newType));
            migrateStorage(lastType, newType);
            config.set("storage.last-storage", newType);
            saveConfig();
        }

        rewardLogManager = new RewardLogManager(this);
        rewardManager = new RewardManager(this);
        rewardManager.start();

        registerCommands();

        initializeDiscordBot(config);

        // Register chat bridge listener
        if (config.getBoolean("chat-bridge.enabled", false)) {
            Bukkit.getPluginManager().registerEvents(
                    new com.blockstock.cordsync.listeners.ChatBridgeListener(this), this);
            getLogger().info("💬 Chat bridge enabled.");
        }

        // Register 2FA login listener
        if (config.getBoolean("security.2fa-login.enabled", false)) {
            Bukkit.getPluginManager().registerEvents(
                    new com.blockstock.cordsync.listeners.LoginVerifyListener(this), this);
            getLogger().info("2FA Login system enabled.");
        }

        // Register join/quit message listener
        if (config.getBoolean("discord.join-quit-messages.enabled", false)) {
            Bukkit.getPluginManager().registerEvents(
                    new com.blockstock.cordsync.listeners.JoinQuitListener(this), this);
            getLogger().info("Join/Quit messages enabled.");
        }

        // Register GUI listener
        com.blockstock.cordsync.gui.LinkGUI linkGUI = new com.blockstock.cordsync.gui.LinkGUI(this);
        Bukkit.getPluginManager().registerEvents(linkGUI, this);

        // Register PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.blockstock.cordsync.hooks.CordSyncPlaceholders(this).register();
            getLogger().info("PlaceholderAPI integration enabled.");
        }

        if (config.getBoolean("reverify.enabled", false)) {
            reverifyTask = new ReverifyTask(this);
            reverifyTask.start();
            getLogger().info("♻ Smart Re-Verification system enabled.");
        }

        if (config.getBoolean("update-checker", true)) {
            new UpdateChecker(this).checkForUpdates();
        }

        if (config.getBoolean("metrics", true)) {
            int pluginId = 29899; // bStats Plugin ID
            org.bstats.bukkit.Metrics metrics = new org.bstats.bukkit.Metrics(this, pluginId);

            // Custom bStats Chart: Track which storage method servers prefer
            metrics.addCustomChart(new org.bstats.charts.SimplePie("storage_type", () -> {
                return getConfig().getString("storage.type", "YAML").toUpperCase();
            }));
        }

        getLogger().info(MessageUtil.get("plugin.enabled"));
    }

    @Override
    public void onDisable() {
        if (discordBot != null) {
            discordBot.shutdown();
            getLogger().info(MessageUtil.get("discord.bot-stopped"));
        }

        if (rewardManager != null) {
            rewardManager.stop();
            getLogger().info(MessageUtil.get("rewards.system-stopped"));
        }

        if (rewardLogManager != null) {
            rewardLogManager.close();
            getLogger().info(MessageUtil.get("rewards.log-stopped"));
        }

        if (reverifyTask != null) {
            reverifyTask.stop();
            getLogger().info("â™» AkÄ±llÄ± Yeniden DoÄŸrulama sistemi durduruldu.");
        }

        if (storageProvider != null) {
            storageProvider.close();
            getLogger().info(MessageUtil.get("storage.closed"));
        }

        getLogger().info(MessageUtil.get("plugin.disabled"));
    }

    // --- ASCII LOGO ---
    private void printLogo() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        String version = getDescription().getVersion();

        console.sendMessage("");
        console.sendMessage("\u00A7b  ____              _\u00A7f____                  ");
        console.sendMessage("\u00A7b / ___|___  _ __ __| |\u00A7f/ ___|_   _ _ __   ___ ");
        console.sendMessage("\u00A7b| |   / _ \\| '__/ _ |\u00A7f\\___ \\| | | | '_ \\ / __|");
        console.sendMessage("\u00A7b| |__| (_) | | | (_| |\u00A7f ___) || |_| | | | | (__ ");
        console.sendMessage("\u00A7b \\____\\___/|_|  \\__,_|\u00A7f|____/ \\__, |_| |_|\\___|");
        console.sendMessage("\u00A7b                           \u00A7f|___/          ");
        console.sendMessage("");
        console.sendMessage("      \u00A77Version: \u00A7fv" + version);
        console.sendMessage("      \u00A77Developer: \u00A7fmusbabaff");
        console.sendMessage("");
    }

    private StorageProvider createStorage(String type) {
        switch (type.toUpperCase()) {
            case "MYSQL":
                getLogger().info(MessageUtil.get("storage.mysql"));
                return new MySQLStorage(this);
            case "SQLITE":
                getLogger().info(MessageUtil.get("storage.sqlite"));
                return new SQLiteStorage(this);
            default:
                getLogger().info(MessageUtil.get("storage.yaml"));
                return new YamlStorage(this);
        }
    }

    private void migrateStorage(String oldType, String newType) {
        try {
            StorageProvider oldStorage = createStorage(oldType);

            if (!(oldStorage instanceof Migratable) || !(storageProvider instanceof Migratable)) {
                getLogger().warning(MessageUtil.get("storage.migrate-unsupported"));
                return;
            }

            Migratable from = (Migratable) oldStorage;
            Migratable to = (Migratable) storageProvider;

            Map<UUID, Migratable.LinkedData> data = from.loadAllLinkedAccounts();
            if (data.isEmpty()) {
                getLogger().info(MessageUtil.get("storage.migrate-empty").replace("{type}", oldType));
                oldStorage.close();
                return;
            }

            to.importLinkedAccounts(data);
            oldStorage.close();

            File yamlFile = new File(getDataFolder(), "linked-accounts.yml");
            if (yamlFile.exists()) {
                File backup = new File(getDataFolder(), "backup-" + oldType.toLowerCase() + ".bak");
                yamlFile.renameTo(backup);
                getLogger().info(MessageUtil.get("storage.backup-created").replace("{file}", backup.getName()));
            }

            getLogger().info(MessageUtil.get("storage.migrate-success")
                    .replace("{count}", String.valueOf(data.size()))
                    .replace("{old}", oldType)
                    .replace("{new}", newType));

        } catch (Exception e) {
            getLogger().severe(MessageUtil.get("storage.migrate-fail").replace("{error}", e.getMessage()));
            e.printStackTrace();
        }
    }

    private void registerCommands() {
        if (getCommand("link") != null) {
            getCommand("link").setExecutor(new LinkCommand(linkManager));
        } else {
            getLogger().severe(MessageUtil.get("command.missing").replace("{cmd}", "link"));
        }

        if (getCommand("unlink") != null) {
            getCommand("unlink").setExecutor(new UnlinkCommand());
        } else {
            getLogger().warning(MessageUtil.get("command.undefined").replace("{cmd}", "unlink"));
        }

        if (getCommand("csreload") != null) {
            getCommand("csreload").setExecutor(new CordSyncReloadCommand());
        } else {
            getLogger().warning(MessageUtil.get("command.undefined").replace("{cmd}", "csreload"));
        }

        if (getCommand("csreverify") != null) {
            getCommand("csreverify").setExecutor(new CordSyncReverifyCommand());
        } else {
            getLogger().warning(MessageUtil.get("command.undefined").replace("{cmd}", "csreverify"));
        }

        if (getCommand("csinfo") != null) {
            getCommand("csinfo").setExecutor(new CordSyncInfoCommand());
        } else {
            getLogger().warning(MessageUtil.get("command.undefined").replace("{cmd}", "csinfo"));
        }
    }

    private void initializeDiscordBot(FileConfiguration config) {
        // Anti-Format-Breaking Check: Did the user strip the indentation spaces?
        if (!config.contains("discord.bot-token") && config.contains("bot-token")) {
            getLogger().severe("──────────────────────────────────────────────────");
            getLogger().severe("🚨 CRITICAL YAZILIM/BİÇİM HATASI (YAML FORMAT ERROR) 🚨");
            getLogger().severe("🚨 'bot-token' ve 'enabled' ayarlarının başındaki BOŞLUKLARI silmişsiniz!");
            getLogger().severe("🚨 Bu ayarların discord'un 'içinde' sayılması için başlarında 2 adet boşluk olmalı.");
            getLogger().severe("🚨 Lütfen config.yml dosyanızı şu şekilde DÜZELTİN:");
            getLogger().severe("discord:");
            getLogger().severe("  enabled: true");
            getLogger().severe("  bot-token: \"SİZİN_TOKENİNİZ\"");
            getLogger().severe("🚨 Boşlukları (SPACE tuşu ile) ekleyip kaydedin ve /csreload yazın.");
            getLogger().severe("──────────────────────────────────────────────────");
            return;
        }

        String token = config.getString("discord.bot-token");
        String status = config.getString("discord.status", "Minecraft ↔ Discord Linker");

        boolean hasValidToken = token != null && !token.isEmpty()
                && !token.equalsIgnoreCase("BOT_TOKEN_BURAYA")
                && !token.equalsIgnoreCase("BOT_TOKEN_HERE");

        boolean isEnabled = config.getBoolean("discord.enabled", false);

        // Smart Auto-Enabler: If they entered a token but forgot to enable it
        if (!isEnabled && hasValidToken) {
            getLogger().info("⚠️ Token detected! Auto-enabling Discord integration in config.yml...");
            config.set("discord.enabled", true);
            saveConfig();
            isEnabled = true;
        }

        // Diagnostic debugging injected for user visibility:
        if (token == null && !isEnabled) {
            if (config.getKeys(false).isEmpty()) {
                getLogger().severe("🚨 CRITICAL: Your config.yml is COMPLETELY EMPTY in server memory!");
                getLogger().severe("🚨 1) You might have a YAML syntax error (e.g., using TABs instead of spaces).");
                getLogger().severe("🚨 2) You might be editing the file in the wrong server folder.");
            } else {
                getLogger().severe("🚨 WARNING: 'discord.bot-token' could not be found inside config.yml.");
                getLogger().severe(
                        "🚨 WARNING: Please make sure you saved the file and there are no YAML space/indentation errors.");
            }
        }

        if (!isEnabled) {
            getLogger().info(MessageUtil.get("discord.disabled"));
            return;
        }

        if (!hasValidToken) {
            getLogger().warning(MessageUtil.get("discord.no-token"));
            return;
        }

        try {
            discordBot = new DiscordBot(this, token, status);
            getLogger().info(MessageUtil.get("discord.started"));
        } catch (Exception e) {
            getLogger().severe(MessageUtil.get("discord.failed").replace("{error}", e.getMessage()));
        }
    }

    public void reloadDiscordBot() {
        if (discordBot != null) {
            discordBot.shutdown();
            discordBot = null;
        }
        initializeDiscordBot(getConfig());
    }

    public static CordSync getInstance() {
        return instance;
    }

    public void debug(String message) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public LinkManager getLinkManager() {
        return linkManager;
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public RewardLogManager getRewardLogManager() {
        return rewardLogManager;
    }

    public ReverifyTask getReverifyTask() {
        return reverifyTask;
    }
}
