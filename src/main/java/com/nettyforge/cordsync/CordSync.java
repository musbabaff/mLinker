package com.nettyforge.cordsync;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.nettyforge.cordsync.commands.LinkCommand;
import com.nettyforge.cordsync.commands.UnlinkCommand;
import com.nettyforge.cordsync.commands.CordSyncInfoCommand;
import com.nettyforge.cordsync.commands.CordSyncReloadCommand;
import com.nettyforge.cordsync.commands.CordSyncReverifyCommand;
import com.nettyforge.cordsync.discord.DiscordBot;
import com.nettyforge.cordsync.managers.LinkManager;
import com.nettyforge.cordsync.managers.RewardManager;
import com.nettyforge.cordsync.rewards.RewardLogManager;
import com.nettyforge.cordsync.storage.Migratable;
import com.nettyforge.cordsync.storage.MySQLStorage;
import com.nettyforge.cordsync.storage.SQLiteStorage;
import com.nettyforge.cordsync.storage.StorageProvider;
import com.nettyforge.cordsync.storage.YamlStorage;
import com.nettyforge.cordsync.tasks.ReverifyTask;
import com.nettyforge.cordsync.utils.MessageUtil;
import com.nettyforge.cordsync.utils.SchedulerUtil;
import com.nettyforge.cordsync.utils.UpdateChecker;
import com.nettyforge.cordsync.modules.ModuleLoader;

public class CordSync extends JavaPlugin {

    private static CordSync instance;
    private LinkManager linkManager;
    private StorageProvider storageProvider;
    private DiscordBot discordBot;
    private RewardManager rewardManager;
    private RewardLogManager rewardLogManager;
    private ReverifyTask reverifyTask;
    private ModuleLoader moduleLoader;
    private boolean updateAvailable = false;
    private String latestVersion = "";
    private com.nettyforge.cordsync.listeners.LoginVerifyListener loginVerifyListener;
    private net.milkbowl.vault.economy.Economy econ = null;

    public net.milkbowl.vault.economy.Economy getEconomy() {
        return econ;
    }

    public com.nettyforge.cordsync.listeners.LoginVerifyListener getLoginVerifyListener() {
        return loginVerifyListener;
    }

    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public void setUpdateAvailable(boolean updateAvailable) {
        this.updateAvailable = updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }

    private org.bukkit.configuration.file.YamlConfiguration twoFactorConfig;
    private File twoFactorFile;

    public void loadTwoFactorConfig() {
        twoFactorFile = new File(getDataFolder(), "2fa-settings.yml");
        if (!twoFactorFile.exists()) {
            try {
                twoFactorFile.createNewFile();
            } catch (Exception ignored) {
            }
        }
        twoFactorConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(twoFactorFile);
    }

    public boolean getTwoFactorEnabled(UUID uuid) {
        if (twoFactorConfig == null)
            loadTwoFactorConfig();
        return twoFactorConfig.getBoolean(uuid.toString(), false);
    }

    public void setTwoFactorEnabled(UUID uuid, boolean enabled) {
        if (twoFactorConfig == null)
            loadTwoFactorConfig();
        twoFactorConfig.set(uuid.toString(), enabled);
        try {
            twoFactorConfig.save(twoFactorFile);
        } catch (Exception ignored) {
        }
    }

    // Change SLF4J/JDK logging level for JDA explicitly to prevent WebSocketClient
    // spam
    private void suppressJDALogs() {
        try {
            java.util.logging.Logger jdaLogger = java.util.logging.Logger.getLogger("net.dv8tion");
            jdaLogger.setLevel(java.util.logging.Level.WARNING);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        suppressJDALogs();
        instance = this;

        // Konsola havalÄ± ASCII logomuzu yazdÄ±ran metot
        printLogo();

        saveDefaultConfig();

        // Auto-migrate config (add missing keys without overwriting)
        new com.nettyforge.cordsync.utils.ConfigMigrator(this).migrate();
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
                    new com.nettyforge.cordsync.listeners.ChatBridgeListener(this), this);
            getLogger().info("💬 Chat bridge enabled.");
        }

        // Register 2FA login listener (SINGLE INSTANCE for both Bukkit + JDA)
        if (config.getBoolean("security.2fa-login.enabled", false)) {
            loginVerifyListener = new com.nettyforge.cordsync.listeners.LoginVerifyListener(this);
            Bukkit.getPluginManager().registerEvents(loginVerifyListener, this);
            getLogger().info("2FA Login system enabled.");
        }

        // Register join/quit message listener
        if (config.getBoolean("discord.join-quit-messages.enabled", false)) {
            Bukkit.getPluginManager().registerEvents(
                    new com.nettyforge.cordsync.listeners.JoinQuitListener(this), this);
            getLogger().info("Join/Quit messages enabled.");
        }

        // Register GUI listener
        com.nettyforge.cordsync.gui.LinkGUI linkGUI = new com.nettyforge.cordsync.gui.LinkGUI(this);
        Bukkit.getPluginManager().registerEvents(linkGUI, this);

        // Register Update Notifier listener
        Bukkit.getPluginManager().registerEvents(new com.nettyforge.cordsync.listeners.UpdateNotifyListener(this),
                this);

        // Register PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.nettyforge.cordsync.hooks.CordSyncPlaceholders(this).register();
            getLogger().info("PlaceholderAPI integration enabled.");
        }

        // Setup Vault Economy
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") != null) {
            org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = getServer()
                    .getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (rsp != null) {
                econ = rsp.getProvider();
                getLogger().info("Vault Economy integration enabled.");
            }
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

        // Initialize and load dynamic modules
        moduleLoader = new ModuleLoader(this);
        moduleLoader.loadModules();

        getLogger().info(MessageUtil.get("plugin.enabled"));
    }

    @Override
    public void onDisable() {
        // SPARK OPTIMIZATION: Kill ALL remaining Bukkit tasks to prevent zombie threads first!
        // This stops ConsoleBridge timers and other tasks before JDA drops connection.
        SchedulerUtil.cancelAll(this);

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

        if (moduleLoader != null) {
            moduleLoader.unloadModules();
        }

        // Unregister all event listeners owned by this plugin
        org.bukkit.event.HandlerList.unregisterAll(this);

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
        if (config.getBoolean("debug", false)) {
            // ═══ RAW CONFIG MEMORY DUMP (DEBUG) ═══
            getLogger().info("═══════════ CONFIG DEBUG DUMP ═══════════");
            getLogger()
                    .info("📁 Config file path: " + new java.io.File(getDataFolder(), "config.yml").getAbsolutePath());
            getLogger().info("📁 Data folder exists: " + getDataFolder().exists());
            getLogger().info(" Config file exists: " + new java.io.File(getDataFolder(), "config.yml").exists());
            getLogger().info(" Root-level keys: " + config.getKeys(false));
            getLogger().info(" discord section exists: " + config.contains("discord"));
            getLogger().info("🔑 discord.enabled raw value: " + config.get("discord.enabled"));
            getLogger().info("🔑 discord.bot-token raw value: " + (config.getString("discord.bot-token") != null
                    ? "SET (length=" + config.getString("discord.bot-token").length() + ")"
                    : "NULL"));
            if (config.isConfigurationSection("discord")) {
                getLogger().info(" discord section keys: " + config.getConfigurationSection("discord").getKeys(false));
            } else {
                getLogger().info("🔑 discord is NOT a configuration section!");
            }
            getLogger().info("══════════════════════════════════════════");
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
