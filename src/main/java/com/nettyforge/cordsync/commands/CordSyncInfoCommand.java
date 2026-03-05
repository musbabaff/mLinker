package com.nettyforge.cordsync.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.nettyforge.cordsync.CordSync;

public class CordSyncInfoCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        CordSync plugin = CordSync.getInstance();

        sender.sendMessage("Â§8Â§mâ”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”");
        sender.sendMessage("Â§bÂ§lCordSync Â§7v" + plugin.getDescription().getVersion());
        sender.sendMessage("Â§7GeliÅŸtirici: Â§fmusbabaff");
        sender.sendMessage("");
        sender.sendMessage("Â§7Depolama: Â§f" + plugin.getConfig().getString("storage.type", "Bilinmiyor"));
        sender.sendMessage("Â§7Dil: Â§f" + plugin.getConfig().getString("language", "TR"));
        sender.sendMessage("Â§7ReVerify: Â§f" + (plugin.getReverifyTask() != null ? "Â§aAktif" : "Â§cPasif"));
        sender.sendMessage("Â§7Discord Botu: Â§f" + (plugin.getDiscordBot() != null ? "Â§aBaÄŸlÄ±" : "Â§cKapalÄ±"));
        sender.sendMessage("Â§8Â§mâ”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”");

        return true;
    }
}

