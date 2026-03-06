package com.nettyforge.cordsync.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.managers.LinkManager;
import com.nettyforge.cordsync.storage.StorageProvider;
import com.nettyforge.cordsync.utils.MessageUtil;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class LinkCommand implements CommandExecutor {

    private final LinkManager linkManager;
    private final CordSync plugin;
    private final StorageProvider storage;

    public LinkCommand(LinkManager linkManager) {
        this.linkManager = linkManager;
        this.plugin = CordSync.getInstance();
        this.storage = plugin.getStorageProvider();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Konsol kontrolÃ¼
        if (!(sender instanceof Player)) {
            // MessageUtil zaten renkleri Ã§eviriyorsa ekstra ChatColor kullanmaya gerek yok
            sender.sendMessage(MessageUtil.get("link.not-player"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("yardim"))) {
            player.sendMessage(MessageUtil.get("commands.help-title"));
            player.sendMessage(MessageUtil.get("commands.help-link"));
            player.sendMessage(MessageUtil.get("commands.help-unlink"));
            
            for (com.nettyforge.cordsync.modules.CordModule mod : plugin.getModuleLoader().getActiveModules()) {
                String modName = mod.getName().toLowerCase();
                if (modName.contains("report")) {
                    player.sendMessage(MessageUtil.get("commands.help-report"));
                    player.sendMessage(MessageUtil.get("commands.help-bug"));
                } else if (modName.contains("ticket")) {
                    player.sendMessage(MessageUtil.get("commands.help-ticket"));
                } else if (modName.contains("network")) {
                    if (player.hasPermission("cordsync.staffchat")) {
                        player.sendMessage(MessageUtil.get("commands.help-staffchat"));
                    }
                }
            }
            
            if (player.hasPermission("cordsync.admin")) {
                player.sendMessage(MessageUtil.get("commands.help-admin-info"));
                player.sendMessage(MessageUtil.get("commands.help-admin-reload"));
                player.sendMessage(MessageUtil.get("commands.help-admin-reverify"));
            }
            return true;
        }

        // Oyuncu zaten eÅŸleÅŸtirilmiÅŸ mi?
        if (storage.isPlayerLinked(player.getUniqueId())) {
            player.sendMessage(MessageUtil.get("link.already-linked"));
            return true;
        }

        // Oyuncunun halihazÄ±rda sÃ¼resi dolmamÄ±ÅŸ aktif bir kodu var mÄ±?
        String existing = linkManager.getCode(player);
        if (existing != null) {
            sendStyledCodeMessage(player, existing, true);
            return true;
        }

        // Yeni kod oluÅŸtur
        String newCode = linkManager.generateCode(player);
        sendStyledCodeMessage(player, newCode, false);

        return true;
    }

    private void sendStyledCodeMessage(Player player, String code, boolean isExisting) {

        player.sendMessage("");
        player.sendMessage(MessageUtil.get("link.header"));
        player.sendMessage(MessageUtil.get("link.title"));

        if (isExisting) {
            player.sendMessage(MessageUtil.get("link.code-active"));
        } else {
            player.sendMessage(MessageUtil.get("link.code-new"));
        }

        // TÄ±klanabilir ÅŸÄ±k metin tasarÄ±mÄ±
        TextComponent clickableCode = new TextComponent("✦ " + code + " ✦");
        clickableCode.setColor(ChatColor.AQUA);
        clickableCode.setBold(true);

        // Ã–NEMLÄ° DEÄÄ°ÅÄ°KLÄ°K: 1.15+ iÃ§in COPY_TO_CLIPBOARD kullanarak oyuncunun
        // kod
        // kopyalamasÄ±nÄ± kolaylaÅŸtÄ±rdÄ±k.
        clickableCode.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code));

        clickableCode.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(MessageUtil.get("link.copied-hover"))));

        // Spigot API ile Bungee chat mesajÄ±nÄ± gÃ¶nder
        player.spigot().sendMessage(clickableCode);

        // KullanÄ±m talimatÄ±nÄ± gÃ¶nder
        player.sendMessage(MessageUtil.get("link.usage").replace("<kod>", code));
        player.sendMessage(MessageUtil.get("link.header"));
        player.sendMessage("");
    }
}
