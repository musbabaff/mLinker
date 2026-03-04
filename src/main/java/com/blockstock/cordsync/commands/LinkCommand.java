package com.blockstock.cordsync.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.blockstock.cordsync.CordSync;
import com.blockstock.cordsync.managers.LinkManager;
import com.blockstock.cordsync.storage.StorageProvider;
import com.blockstock.cordsync.utils.MessageUtil;

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
