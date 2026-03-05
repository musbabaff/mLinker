package com.nettyforge.cordsync.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.nettyforge.cordsync.CordSync;

public class CordSyncInfoCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        CordSync plugin = CordSync.getInstance();

        sender.sendMessage("§8§m--------------------------------");
        sender.sendMessage("§b§lCordSync §7v" + plugin.getDescription().getVersion());
        sender.sendMessage("§7Developer: §fmusbabaff");
        sender.sendMessage("");
        sender.sendMessage("§7Storage: §f" + plugin.getConfig().getString("storage.type", "Unknown"));
        sender.sendMessage("§7Language: §f" + plugin.getConfig().getString("language", "en"));
        sender.sendMessage("§7ReVerify: §f" + (plugin.getReverifyTask() != null ? "§aActive" : "§cPassive"));
        sender.sendMessage("§7Discord Bot: §f" + (plugin.getDiscordBot() != null ? "§aConnected" : "§cOffline"));
        sender.sendMessage("§8§m--------------------------------");

        return true;
    }
}
