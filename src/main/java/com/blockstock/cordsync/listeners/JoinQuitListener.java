package com.blockstock.cordsync.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.blockstock.cordsync.CordSync;
import com.blockstock.cordsync.storage.StorageProvider;
import com.blockstock.cordsync.utils.MessageUtil;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.awt.Color;
import java.time.Instant;

@SuppressWarnings("null")
public class JoinQuitListener implements Listener {

    private final CordSync plugin;

    public JoinQuitListener(CordSync plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("discord.join-quit-messages.enabled", false))
            return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        StorageProvider storage = plugin.getStorageProvider();

        if (!storage.isPlayerLinked(uuid))
            return;

        String discordId = storage.getDiscordId(uuid);
        if (discordId == null)
            return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String skinUrl = "https://mc-heads.net/avatar/" + uuid + "/128";
            String roleName = getHighestRole(discordId);
            String rolePrefix = roleName != null ? "[" + roleName + "] " : "";
            int online = Bukkit.getOnlinePlayers().size();

            String authorText = MessageUtil.getRaw("join-quit.join-author")
                    .replace("{role}", rolePrefix)
                    .replace("{player}", player.getName());

            EmbedBuilder embed = new EmbedBuilder()
                    .setAuthor(authorText, null, skinUrl)
                    .setColor(new Color(0, 200, 83))
                    .setThumbnail(skinUrl)
                    .addField(MessageUtil.getRaw("join-quit.field-player"), player.getName(), true)
                    .addField(MessageUtil.getRaw("join-quit.field-discord"), "<@" + discordId + ">", true)
                    .addField(MessageUtil.getRaw("join-quit.field-online"), String.valueOf(online), true)
                    .setFooter("CordSync • Join/Quit")
                    .setTimestamp(Instant.now());

            if (plugin.getDiscordBot() != null) {
                plugin.getDiscordBot().sendLogEmbed(null, null, null, embed);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("discord.join-quit-messages.enabled", false))
            return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        StorageProvider storage = plugin.getStorageProvider();

        if (!storage.isPlayerLinked(uuid))
            return;

        String discordId = storage.getDiscordId(uuid);
        if (discordId == null)
            return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String skinUrl = "https://mc-heads.net/avatar/" + uuid + "/128";
            String roleName = getHighestRole(discordId);
            String rolePrefix = roleName != null ? "[" + roleName + "] " : "";
            int online = Bukkit.getOnlinePlayers().size() - 1;

            String authorText = MessageUtil.getRaw("join-quit.quit-author")
                    .replace("{role}", rolePrefix)
                    .replace("{player}", player.getName());

            EmbedBuilder embed = new EmbedBuilder()
                    .setAuthor(authorText, null, skinUrl)
                    .setColor(new Color(255, 71, 87))
                    .setThumbnail(skinUrl)
                    .addField(MessageUtil.getRaw("join-quit.field-player"), player.getName(), true)
                    .addField(MessageUtil.getRaw("join-quit.field-discord"), "<@" + discordId + ">", true)
                    .addField(MessageUtil.getRaw("join-quit.field-online"), String.valueOf(Math.max(0, online)), true)
                    .setFooter("CordSync • Join/Quit")
                    .setTimestamp(Instant.now());

            if (plugin.getDiscordBot() != null) {
                plugin.getDiscordBot().sendLogEmbed(null, null, null, embed);
            }
        });
    }

    private String getHighestRole(String discordId) {
        try {
            if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
                return null;
            String guildId = plugin.getConfig().getString("discord.guild-id", "");
            Guild guild = plugin.getDiscordBot().getJda().getGuildById(guildId != null ? guildId : "");
            if (guild == null)
                return null;
            Member member = guild.retrieveMemberById(discordId).complete();
            if (member == null || member.getRoles().isEmpty())
                return null;
            return member.getRoles().get(0).getName();
        } catch (Exception e) {
            return null;
        }
    }
}
