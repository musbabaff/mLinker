package com.nettyforge.cordsync.commands;

import java.awt.Color;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.storage.StorageProvider;
import com.nettyforge.cordsync.utils.MessageUtil;
import com.nettyforge.cordsync.utils.SchedulerUtil;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;

public class UnlinkCommand implements CommandExecutor {

    private final CordSync plugin;
    private final StorageProvider storage;

    public UnlinkCommand() {
        this.plugin = CordSync.getInstance();
        this.storage = plugin.getStorageProvider();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.get("link.not-player"));
            return true;
        }

        Player player = (Player) sender;

        // Yetki kontrolÃ¼
        if (!player.hasPermission("CordSync.use")) {
            player.sendMessage(MessageUtil.get("system.no-permission"));
            return true;
        }

        if (!storage.isPlayerLinked(player.getUniqueId())) {
            player.sendMessage(MessageUtil.get("unlink.not-linked"));
            return true;
        }

        String discordId = storage.getDiscordId(player.getUniqueId());
        if (discordId == null || discordId.isEmpty()) {
            player.sendMessage(MessageUtil.get("unlink.error"));
            return true;
        }

        // VeritabanÄ± ve Discord API iÅŸlemlerini tamamen Asenkron yapÄ±yoruz.
        SchedulerUtil.runAsync(plugin, () -> {

            // 1. Unlink timestamp kaydet (suistimal korumasÄ±)
            storage.setUnlinkTimestamp(player.getUniqueId(), System.currentTimeMillis());

            // 2. VeritabanÄ±ndan silme iÅŸlemi
            storage.removeLinkedAccount(player.getUniqueId());

            // 2. Discord rolÃ¼nÃ¼ alma iÅŸlemi
            removeVerifiedRole(discordId);

            // 3. LuckPerms eÅŸleme rollerini kaldÄ±r
            removeLuckPermsRoles(discordId, player.getUniqueId());

            // 4. Log kanalÄ±na bilgilendirme gÃ¶nder
            sendUnlinkLog(player.getName());

            // 5. Oyuncuya ve konsola mesaj gÃ¶nderme
            SchedulerUtil.runSync(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(MessageUtil.get("unlink.success"));
                }
                plugin.getLogger()
                        .info(MessageUtil.format("unlink.console-success", Map.of("player", player.getName())));
            });

        });

        return true;
    }

    private void removeVerifiedRole(String discordId) {
        try {
            if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null) {
                plugin.getLogger().warning(MessageUtil.get("discord.bot-disabled"));
                return;
            }

            String guildId = plugin.getConfig().getString("discord.guild-id");
            String roleId = plugin.getConfig().getString("discord.role-id-verified");

            if (guildId == null || roleId == null || guildId.isEmpty() || roleId.isEmpty()) {
                plugin.getLogger().warning(MessageUtil.get("discord.role-missing"));
                return;
            }

            Guild guild = plugin.getDiscordBot().getJda().getGuildById(guildId);
            if (guild == null) {
                plugin.getLogger().warning(MessageUtil.get("discord.guild-missing"));
                return;
            }

            Role role = guild.getRoleById(roleId);
            if (role == null) {
                plugin.getLogger().warning(MessageUtil.get("discord.role-missing"));
                return;
            }

            if (discordId == null || discordId.isEmpty())
                return;

            Member member = guild.retrieveMemberById(discordId).complete();
            if (member == null) {
                plugin.getLogger().warning(MessageUtil.format("discord.user-not-found", Map.of("id", discordId)));
                return;
            }

            guild.removeRoleFromMember(member, role).queue(
                    success -> plugin.getLogger().info(MessageUtil.get("discord.unlinked")),
                    error -> plugin.getLogger().warning(MessageUtil.format("discord.role-fail",
                            Map.of("error", error.getMessage()))));

        } catch (Exception e) {
            plugin.getLogger().severe(MessageUtil.format("discord.role-remove-error", Map.of("error", e.getMessage())));
        }
    }

    /**
     * LuckPerms grupâ†’Discord rol eÅŸlemelerindeki rolleri kaldÄ±rÄ±r.
     */
    private void removeLuckPermsRoles(String discordId, UUID playerUUID) {
        try {
            if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
                return;

            if (!plugin.getConfig().getBoolean("discord.luckperms-roles.enabled", false))
                return;

            ConfigurationSection mappings = plugin.getConfig()
                    .getConfigurationSection("discord.luckperms-roles.mappings");
            if (mappings == null || mappings.getKeys(false).isEmpty())
                return;

            // Guild bilgisi
            String guildId = plugin.getConfig().getString("discord.guild-id");
            if (guildId == null || guildId.isEmpty())
                return;

            Guild guild = plugin.getDiscordBot().getJda().getGuildById(guildId);
            if (guild == null)
                return;
            if (discordId == null || discordId.isEmpty())
                return;

            Member member = guild.retrieveMemberById(discordId).complete();
            if (member == null)
                return;

            // LuckPerms API'sine eriÅŸim (oyuncunun gruplarÄ±nÄ± Ã¶ÄŸrenmek iÃ§in)
            LuckPerms luckPerms = null;
            Set<String> groups = new java.util.HashSet<>();
            try {
                var provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
                if (provider != null) {
                    luckPerms = provider.getProvider();
                    User lpUser = luckPerms.getUserManager().getUser(playerUUID);
                    if (lpUser == null) {
                        lpUser = luckPerms.getUserManager().loadUser(playerUUID).join();
                    }
                    if (lpUser != null) {
                        String primaryGroup = lpUser.getPrimaryGroup();
                        if (primaryGroup != null)
                            groups.add(primaryGroup.toLowerCase());
                        for (var node : lpUser.getNodes()) {
                            if (node.getKey().startsWith("group.")) {
                                groups.add(node.getKey().substring(6).toLowerCase());
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            // EÅŸlemedeki tÃ¼m rolleri kaldÄ±r (grup kontrolÃ¼nden baÄŸÄ±msÄ±z - hesap
            // kaldÄ±rÄ±lÄ±yor)
            for (String groupName : mappings.getKeys(false)) {
                String discordRoleId = mappings.getString(groupName);
                if (discordRoleId == null || discordRoleId.isEmpty()
                        || discordRoleId.equals("DISCORD_ROL_ID_BURAYA"))
                    continue;

                Role role = guild.getRoleById(discordRoleId);
                if (role == null)
                    continue;

                // Ãœyenin bu rolÃ¼ var mÄ± kontrol et
                if (member.getRoles().contains(role)) {
                    guild.removeRoleFromMember(member, role).queue(
                            success -> plugin.getLogger().info(
                                    MessageUtil.format("discord.luckperms-role-removed",
                                            Map.of("player", member.getEffectiveName(), "role", groupName))),
                            error -> plugin.getLogger().warning(
                                    MessageUtil.format("discord.role-fail",
                                            Map.of("error", error.getMessage()))));
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("⚠ LuckPerms role removal error: " + e.getMessage());
        }
    }

    /**
     * Log kanalÄ±na hesap kaldÄ±rma bilgisi gÃ¶nderir.
     */
    private void sendUnlinkLog(String playerName) {
        if (plugin.getDiscordBot() == null)
            return;

        String description = MessageUtil.format("discord.log-unlinked",
                Map.of("player", playerName));

        plugin.getDiscordBot().sendLogEmbed("🔓 Discord Account Unlinked", description, new Color(244, 67, 54));
    }
}
