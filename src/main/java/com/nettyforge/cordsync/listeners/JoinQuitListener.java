package com.nettyforge.cordsync.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.storage.StorageProvider;
import com.nettyforge.cordsync.utils.MessageUtil;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

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

        boolean isLinked = storage.isPlayerLinked(uuid);
        String discordId = isLinked ? storage.getDiscordId(uuid) : null;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String skinUrl = "https://mc-heads.net/avatar/" + uuid + "/128";
            
            String roleName = null;
            if (isLinked && discordId != null) {
                roleName = getHighestRole(discordId);
            }
            String rolePrefix = roleName != null ? "[" + roleName + "] " : "";
            int online = Bukkit.getOnlinePlayers().size();

            String authorText = MessageUtil.getRaw("join-quit.join-author")
                    .replace("{role}", rolePrefix)
                    .replace("{player}", player.getName());

            EmbedBuilder embed = new EmbedBuilder()
                    .setAuthor(authorText, null, skinUrl)
                    .setColor(java.awt.Color.decode("#2B2D31"))
                    .setThumbnail(skinUrl)
                    .addField(MessageUtil.getRaw("join-quit.field-player"), player.getName(), true);
                    
            if (isLinked && discordId != null) {
                embed.addField(MessageUtil.getRaw("join-quit.field-discord"), "<@" + discordId + ">", true);
            } else {
                embed.addField(MessageUtil.getRaw("join-quit.field-discord"), "Not Linked", true);
            }
            
            embed.addField(MessageUtil.getRaw("join-quit.field-online"), String.valueOf(online), true)
                    .setFooter("CordSync • Join/Quit")
                    .setTimestamp(Instant.now());

            if (plugin.getDiscordBot() != null) {
                String chId = plugin.getConfig().getString("discord.join-quit-messages.channel-id", "");
                if (chId != null && !chId.isEmpty()) {
                    try {
                        TextChannel tc = plugin.getDiscordBot().getJda().getTextChannelById(chId);
                        if (tc != null)
                            tc.sendMessageEmbeds(embed.build()).queue();
                    } catch (Exception ignored) {
                    }
                }
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

        boolean isLinked = storage.isPlayerLinked(uuid);
        String discordId = isLinked ? storage.getDiscordId(uuid) : null;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String skinUrl = "https://mc-heads.net/avatar/" + uuid + "/128";
            
            String roleName = null;
            if (isLinked && discordId != null) {
                roleName = getHighestRole(discordId);
            }
            String rolePrefix = roleName != null ? "[" + roleName + "] " : "";
            int online = Bukkit.getOnlinePlayers().size() - 1;

            String authorText = MessageUtil.getRaw("join-quit.quit-author")
                    .replace("{role}", rolePrefix)
                    .replace("{player}", player.getName());

            EmbedBuilder embed = new EmbedBuilder()
                    .setAuthor(authorText, null, skinUrl)
                    .setColor(java.awt.Color.decode("#2B2D31"))
                    .setThumbnail(skinUrl)
                    .addField(MessageUtil.getRaw("join-quit.field-player"), player.getName(), true);
                    
            if (isLinked && discordId != null) {
                embed.addField(MessageUtil.getRaw("join-quit.field-discord"), "<@" + discordId + ">", true);
            } else {
                embed.addField(MessageUtil.getRaw("join-quit.field-discord"), "Not Linked", true);
            }
            
            embed.addField(MessageUtil.getRaw("join-quit.field-online"), String.valueOf(Math.max(0, online)), true)
                    .setFooter("CordSync • Join/Quit")
                    .setTimestamp(Instant.now());

            if (plugin.getDiscordBot() != null) {
                String chId = plugin.getConfig().getString("discord.join-quit-messages.channel-id", "");
                if (chId != null && !chId.isEmpty()) {
                    try {
                        TextChannel tc = plugin.getDiscordBot().getJda().getTextChannelById(chId);
                        if (tc != null)
                            tc.sendMessageEmbeds(embed.build()).queue();
                    } catch (Exception ignored) {
                    }
                }
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
