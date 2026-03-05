package com.nettyforge.cordsync.hooks;

import java.util.UUID;

import org.bukkit.OfflinePlayer;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.storage.StorageProvider;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

/**
 * PlaceholderAPI expansion for CordSync.
 *
 * Placeholders:
 * %cordsync_is_linked% → true/false
 * %cordsync_discord_name% → Discord username
 * %cordsync_discord_tag% → Discord display name
 * %cordsync_discord_id% → Discord user ID
 * %cordsync_discord_avatar% → Avatar URL
 * %cordsync_discord_role% → Highest Discord role
 * %cordsync_linked_count% → Total linked accounts
 * %cordsync_online_linked% → Online linked players count
 */
@SuppressWarnings("null")
public class CordSyncPlaceholders extends PlaceholderExpansion {

    private final CordSync plugin;

    public CordSyncPlaceholders(CordSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "cordsync";
    }

    @Override
    public String getAuthor() {
        return "musbabaff";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null)
            return "";

        StorageProvider storage = plugin.getStorageProvider();
        UUID uuid = player.getUniqueId();

        switch (params.toLowerCase()) {

            case "is_linked":
                return String.valueOf(storage.isPlayerLinked(uuid));

            case "discord_name": {
                String discordId = storage.getDiscordId(uuid);
                if (discordId == null)
                    return "Not Linked";
                User user = getDiscordUser(discordId);
                return user != null ? user.getName() : "Unknown";
            }

            case "discord_tag": {
                String discordId = storage.getDiscordId(uuid);
                if (discordId == null)
                    return "Not Linked";
                User user = getDiscordUser(discordId);
                return user != null ? user.getEffectiveName() : "Unknown";
            }

            case "discord_id": {
                String discordId = storage.getDiscordId(uuid);
                return discordId != null ? discordId : "None";
            }

            case "discord_avatar": {
                String discordId = storage.getDiscordId(uuid);
                if (discordId == null)
                    return "";
                User user = getDiscordUser(discordId);
                return user != null ? user.getEffectiveAvatarUrl() : "";
            }

            case "discord_role": {
                String discordId = storage.getDiscordId(uuid);
                if (discordId == null)
                    return "None";
                Member member = getDiscordMember(discordId);
                if (member == null || member.getRoles().isEmpty())
                    return "None";
                return member.getRoles().get(0).getName();
            }

            case "linked_count":
                return String.valueOf(storage.getAllLinkedPlayers().size());

            case "online_linked": {
                long count = org.bukkit.Bukkit.getOnlinePlayers().stream()
                        .filter(p -> storage.isPlayerLinked(p.getUniqueId()))
                        .count();
                return String.valueOf(count);
            }

            default:
                return null;
        }
    }

    private User getDiscordUser(String discordId) {
        try {
            if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
                return null;
            return plugin.getDiscordBot().getJda().retrieveUserById(discordId).complete();
        } catch (Exception e) {
            return null;
        }
    }

    private Member getDiscordMember(String discordId) {
        try {
            if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
                return null;
            JDA jda = plugin.getDiscordBot().getJda();
            String guildId = plugin.getConfig().getString("discord.guild-id", "");
            Guild guild = jda.getGuildById(guildId != null ? guildId : "");
            if (guild == null)
                return null;
            return guild.retrieveMemberById(discordId).complete();
        } catch (Exception e) {
            return null;
        }
    }
}
