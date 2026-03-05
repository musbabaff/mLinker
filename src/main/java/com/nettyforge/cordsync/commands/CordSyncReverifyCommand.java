package com.nettyforge.cordsync.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.tasks.ReverifyTask;
import com.nettyforge.cordsync.utils.MessageUtil;

public class CordSyncReverifyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Yetki kontrolÃ¼ (plugin.yml dosyasÄ±nda tanÄ±mladÄ±ÄŸÄ±mÄ±z yetki)
        if (!sender.hasPermission("CordSync.reverify") && !sender.hasPermission("CordSync.admin")) {
            sender.sendMessage(MessageUtil.get("system.no-permission"));
            return true;
        }

        try {
            ReverifyTask task = CordSync.getInstance().getReverifyTask();

            // tr.yml / en.yml dosyasÄ±ndan baÅŸlangÄ±Ã§ mesajÄ±nÄ± gÃ¶nderiyoruz
            sender.sendMessage(MessageUtil.get("reverify.start"));

            if (task != null) {
                // GÃ¶rev zaten aktifse hemen Ã§alÄ±ÅŸtÄ±r
                task.executeNow();
            } else {
                // GÃ¶rev config Ã¼zerinden kapalÄ±ysa (null ise), geÃ§ici bir tane oluÅŸturup sadece 1 kez Ã§alÄ±ÅŸtÄ±r
                ReverifyTask newTask = new ReverifyTask(CordSync.getInstance());
                newTask.executeNow();
                sender.sendMessage("Â§aâ™» Yeniden doÄŸrulama sÃ¼reci tek seferlik oluÅŸturuldu ve baÅŸlatÄ±ldÄ±.");
            }

        } catch (Exception e) {
            // OlasÄ± bir API veya veritabanÄ± hatasÄ±nda konsola/oyuncuya bilgi ver
            sender.sendMessage("Â§câŒ Yeniden doÄŸrulama baÅŸlatÄ±lamadÄ±: " + e.getMessage());
            CordSync.getInstance().getLogger().severe("ReVerify Command Error: " + e.getMessage());
        }

        return true;
    }
}

