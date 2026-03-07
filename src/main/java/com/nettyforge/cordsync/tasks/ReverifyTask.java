package com.nettyforge.cordsync.tasks;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.storage.StorageProvider;
import com.nettyforge.cordsync.utils.SchedulerUtil;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

public class ReverifyTask implements Runnable {

    private final CordSync plugin;
    private boolean running = false;

    public ReverifyTask(CordSync plugin) {
        this.plugin = plugin;
    }

    public void start() {
        long interval = plugin.getConfig().getLong("link.reverify.interval-hours", 6);
        if (interval <= 0)
            interval = 6;
        long ticks = interval * 60 * 60 * 20;
        running = true;
        SchedulerUtil.runSyncTimer(plugin, this, ticks, ticks);
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        runReverifyCheck();
    }

    private void runReverifyCheck() {
        try {
            String action = plugin.getConfig().getString("link.reverify.action", "unlink");
            String guildId = plugin.getConfig().getString("discord.guild-id");
            String verifiedRoleId = plugin.getConfig().getString("discord.role-id-verified");

            if (guildId == null || guildId.isEmpty()) {
                plugin.getLogger().warning("⚠ Discord guild ID not found, verification check skipped.");
                return;
            }

            if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null) {
                plugin.getLogger().warning("⚠ Discord bot is inactive, verification check skipped.");
                return;
            }

            Guild guild = plugin.getDiscordBot().getJda().getGuildById(guildId);
            if (guild == null) {
                plugin.getLogger().warning("⚠ Specified Discord guild not found: " + guildId);
                return;
            }

            StorageProvider storage = plugin.getStorageProvider();
            Set<UUID> players = storage.getAllLinkedPlayers();
            int checked = 0;
            int unlinked = 0;

            for (UUID uuid : players) {
                String discordId = storage.getDiscordId(uuid);
                if (discordId == null)
                    continue;

                Member member = guild.retrieveMemberById(discordId).onErrorMap(err -> null).complete();
                if (member == null || (verifiedRoleId != null && !verifiedRoleId.isEmpty()
                        && member.getRoles().stream().noneMatch(r -> r.getId().equals(verifiedRoleId)))) {
                    if (action.equalsIgnoreCase("unlink")) {
                        storage.removeLinkedAccount(uuid);
                        unlinked++;
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.sendMessage(
                                    "§cYour Discord link was automatically removed (Lost role or left guild).");
                        }
                    } else if (action.equalsIgnoreCase("notify")) {
                        plugin.getLogger().warning("⚠ Player " + storage.getPlayerName(uuid)
                                + " lost the verification role or left the guild.");
                    }
                }
                checked++;
            }

            plugin.getLogger()
                    .info("♻ ReVerify complete -> Checked: " + checked + ", Unlinked: " + unlinked);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ ReVerify error: " + e.getMessage());
        }
    }

    public void executeNow() {
        try {
            runReverifyCheck();
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Manual ReVerify error: " + e.getMessage());
        }
    }
}
