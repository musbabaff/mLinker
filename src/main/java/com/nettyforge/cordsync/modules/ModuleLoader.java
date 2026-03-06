package com.nettyforge.cordsync.modules;

import com.nettyforge.cordsync.CordSync;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ModuleLoader {

    private final CordSync plugin;
    private final Set<CordModule> activeModules = new HashSet<>();

    public Set<CordModule> getActiveModules() {
        return activeModules;
    }

    private File modulesFile;
    private FileConfiguration modulesConfig;

    public ModuleLoader(CordSync plugin) {
        this.plugin = plugin;
    }

    private void loadConfig() {
        modulesFile = new File(plugin.getDataFolder(), "modules.yml");
        if (!modulesFile.exists()) {
            plugin.saveResource("modules.yml", false);
        }
        modulesConfig = YamlConfiguration.loadConfiguration(modulesFile);
    }

    public void registerModule(CordModule module) {
        if (modulesConfig == null)
            loadConfig();

        String configPath = "modules." + module.getName().toLowerCase().replace(" ", "-") + ".enabled";
        // Fallback for new modules if not present in the user's modules.yml
        if (!modulesConfig.contains(configPath)) {
            modulesConfig.set(configPath, false);
            try {
                modulesConfig.save(modulesFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save modules.yml: " + e.getMessage());
            }
        }

        boolean shouldEnable = modulesConfig.getBoolean(configPath, false);

        if (shouldEnable) {
            module.setEnabled(true);
            activeModules.add(module);
            plugin.getLogger().info(com.nettyforge.cordsync.utils.MessageUtil.getRaw("modules.enabled").replace("{module}", module.getName()));
        } else {
            plugin.debug(com.nettyforge.cordsync.utils.MessageUtil.getRaw("modules.skipped").replace("{module}", module.getName()));
            hideCommandsForModule(module);
        }
    }

    private void hideCommandsForModule(CordModule module) {
        String name = module.getName().toLowerCase();
        if (name.contains("report")) {
            hideCommand("report");
            hideCommand("bug");
        } else if (name.contains("ticket")) {
            hideCommand("ticket");
        } else if (name.contains("network")) {
            hideCommand("staffchat");
        }
    }

    @SuppressWarnings("deprecation")
    private void hideCommand(String cmdName) {
        org.bukkit.command.PluginCommand cmd = plugin.getCommand(cmdName);
        if (cmd != null) {
            cmd.setPermission("cordsync.module.disabled");
            cmd.setPermissionMessage(com.nettyforge.cordsync.utils.MessageUtil.get("modules.unknown-command"));
            cmd.setTabCompleter((sender, command, alias, args) -> Collections.emptyList());
            cmd.setExecutor((sender, command, label, args) -> {
                sender.sendMessage(com.nettyforge.cordsync.utils.MessageUtil.get("modules.unknown-command"));
                return true;
            });
        }
    }

    public CordModule getModule(String name) {
        for (CordModule module : activeModules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public void loadModules() {
        plugin.getLogger().info("📦 Loading CordSync Modules...");

        // Register all available modules
        registerModule(new com.nettyforge.cordsync.modules.report.ReportModule(plugin));
        registerModule(new com.nettyforge.cordsync.modules.livestatus.LiveStatusModule(plugin));
        registerModule(new com.nettyforge.cordsync.modules.security.SecurityModule(plugin));
        registerModule(new com.nettyforge.cordsync.modules.economy.EconomyModule(plugin));
        registerModule(new com.nettyforge.cordsync.modules.devops.DevopsModule(plugin));
        registerModule(new com.nettyforge.cordsync.modules.rewards.RewardsModule(plugin));
        registerModule(new com.nettyforge.cordsync.modules.network.NetworkModule(plugin));
        registerModule(new com.nettyforge.cordsync.modules.ticket.TicketModule(plugin));

        // 🏆 The Grand Finale
        registerModule(new com.nettyforge.cordsync.modules.moderation.ModerationModule(plugin));
        registerModule(new com.nettyforge.cordsync.modules.leaderboard.LeaderboardModule(plugin));
        registerModule(new com.nettyforge.cordsync.modules.voice.VoiceModule(plugin));
    }

    public void unloadModules() {
        plugin.getLogger().info("📦 Unloading CordSync Modules...");
        for (CordModule module : activeModules) {
            if (module.isEnabled()) {
                module.setEnabled(false);
            }
        }
        activeModules.clear();
    }
}
