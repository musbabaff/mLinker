package com.nettyforge.cordsync.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.utils.MessageUtil;

public class CordSyncReloadCommand implements CommandExecutor {

    private final CordSync plugin;

    public CordSyncReloadCommand() {
        this.plugin = CordSync.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Yetki kontrolÃ¼ (plugin.yml dosyasÄ±ndaki yeni yetkiye gÃ¶re uyarlandÄ±)
        if (!sender.hasPermission("CordSync.admin")) {
            sender.sendMessage(MessageUtil.get("system.no-permission"));
            return true;
        }

        long start = System.currentTimeMillis();

        // YapÄ±landÄ±rma ve mesaj dosyalarÄ±nÄ± yeniden yÃ¼kle
        plugin.saveDefaultConfig(); // Restore config.yml if it was deleted
        plugin.reloadConfig();
        MessageUtil.load(plugin);

        // CanlÄ± Discord botunu config gÃ¼ncellemeleriyle beraber yeniden baÅŸlat
        plugin.reloadDiscordBot();

        long took = System.currentTimeMillis() - start;

        // Konsol veya oyuncu sohbetinde renk kodunun bozulmamasÄ± iÃ§in Â§ kullanÄ±ldÄ±
        sender.sendMessage(MessageUtil.get("system.reload") + " Â§7(" + took + "ms)");

        plugin.getLogger().info(MessageUtil.get("system.reload-detailed").replace("{time}", took + "ms"));

        return true;
    }
}
