package com.blockstock.cordsync.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;

import com.blockstock.cordsync.CordSync;
import com.blockstock.cordsync.storage.StorageProvider;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

/**
 * Reverse Sync: Discord roles → Minecraft (LuckPerms) groups.
 * When a user gains/loses a Discord role, the corresponding
 * LuckPerms group is added/removed automatically.
 */
public class ReverseSyncListener extends ListenerAdapter {

    private final CordSync plugin;

    public ReverseSyncListener(CordSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onGuildMemberRoleAdd(@Nonnull GuildMemberRoleAddEvent event) {
        if (!plugin.getConfig().getBoolean("discord.reverse-sync.enabled", false))
            return;

        String memberId = event.getMember().getId();
        StorageProvider storage = plugin.getStorageProvider();
        UUID uuid = storage.getPlayerUUID(memberId);
        if (uuid == null)
            return; // Not linked

        for (Role role : event.getRoles()) {
            String luckPermsGroup = getMappedGroup(role.getId());
            if (luckPermsGroup != null) {
                addLuckPermsGroup(uuid, luckPermsGroup);
                plugin.getLogger().info("🔄 Reverse sync: Added group '" + luckPermsGroup
                        + "' to " + storage.getPlayerName(uuid) + " (Discord role: " + role.getName() + ")");
            }
        }
    }

    @Override
    public void onGuildMemberRoleRemove(@Nonnull GuildMemberRoleRemoveEvent event) {
        if (!plugin.getConfig().getBoolean("discord.reverse-sync.enabled", false))
            return;

        String memberId = event.getMember().getId();
        StorageProvider storage = plugin.getStorageProvider();
        UUID uuid = storage.getPlayerUUID(memberId);
        if (uuid == null)
            return;

        for (Role role : event.getRoles()) {
            String luckPermsGroup = getMappedGroup(role.getId());
            if (luckPermsGroup != null) {
                removeLuckPermsGroup(uuid, luckPermsGroup);
                plugin.getLogger().info("🔄 Reverse sync: Removed group '" + luckPermsGroup
                        + "' from " + storage.getPlayerName(uuid) + " (Discord role: " + role.getName() + ")");
            }
        }
    }

    /**
     * Finds the LuckPerms group mapped to a Discord role ID.
     * Uses the reverse-sync.mappings section: discord_role_id -> luckperms_group
     */
    private String getMappedGroup(String discordRoleId) {
        var section = plugin.getConfig().getConfigurationSection("discord.reverse-sync.mappings");
        if (section == null)
            return null;

        for (String roleId : section.getKeys(false)) {
            if (roleId.equals(discordRoleId)) {
                return section.getString(roleId);
            }
        }
        return null;
    }

    private void addLuckPermsGroup(UUID uuid, String group) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "lp user " + uuid + " parent add " + group);
            } catch (Exception e) {
                plugin.getLogger().warning("⚠ Reverse sync LuckPerms add failed: " + e.getMessage());
            }
        });
    }

    private void removeLuckPermsGroup(UUID uuid, String group) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "lp user " + uuid + " parent remove " + group);
            } catch (Exception e) {
                plugin.getLogger().warning("⚠ Reverse sync LuckPerms remove failed: " + e.getMessage());
            }
        });
    }
}
