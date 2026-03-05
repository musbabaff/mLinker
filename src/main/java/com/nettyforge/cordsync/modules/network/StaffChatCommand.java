package com.nettyforge.cordsync.modules.network;

import com.nettyforge.cordsync.CordSync;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffChatCommand implements CommandExecutor {

    private final NetworkModule networkModule;

    public StaffChatCommand(CordSync plugin, NetworkModule networkModule) {
        this.networkModule = networkModule;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command is meant for players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("cordsync.staffchat")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use the cross-server staff chat.");
            return true;
        }

        if (!networkModule.isEnabled()) {
            player.sendMessage(ChatColor.RED + "The Network Module is currently disabled by the server administrator.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /staffchat <message>");
            return true;
        }

        StringBuilder message = new StringBuilder();
        for (String arg : args) {
            message.append(arg).append(" ");
        }

        networkModule.sendStaffChatToDiscord(player, message.toString().trim());
        return true;
    }
}
