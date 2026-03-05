package com.nettyforge.cordsync.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.nettyforge.cordsync.CordSync;

public class UpdateNotifyListener implements Listener {

    private final CordSync plugin;

    public UpdateNotifyListener(CordSync plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.isUpdateAvailable() && (player.isOp() || player.hasPermission("cordsync.admin"))) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&e----------------------------------------------------"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&6CordSync Update"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&cA new version is available! &f(" + plugin.getLatestVersion() + ")"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&7Current version: &e" + plugin.getDescription().getVersion()));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&aDownload: &ehttps://www.spigotmc.org/resources/133118/"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&e----------------------------------------------------"));
            }, 60L); // Delay by 3 seconds so it doesn't get buried in other login messages
        }
    }
}
