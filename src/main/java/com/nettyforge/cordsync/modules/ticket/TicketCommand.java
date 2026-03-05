package com.nettyforge.cordsync.modules.ticket;

import com.nettyforge.cordsync.CordSync;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TicketCommand implements CommandExecutor {

    private final TicketModule ticketModule;

    public TicketCommand(CordSync plugin, TicketModule ticketModule) {
        this.ticketModule = ticketModule;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use the ticket system.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("cordsync.ticket")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use tickets.");
            return true;
        }

        if (!ticketModule.isEnabled()) {
            player.sendMessage(ChatColor.RED + "The Ticket Module is currently disabled.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /ticket create <message> OR /ticket close");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("close")) {
            ticketModule.closeTicket(player);
            return true;
        }

        if (subCommand.equals("create")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "You must provide a description: /ticket create <message>");
                return true;
            }

            StringBuilder message = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                message.append(args[i]).append(" ");
            }

            ticketModule.createTicket(player, message.toString().trim());
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown argument. Use 'create' or 'close'.");
        return true;
    }
}
