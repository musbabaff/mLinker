package com.nettyforge.cordsync.tasks;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.storage.StorageProvider;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

public class ReverifyTask implements Runnable {

    private final CordSync plugin;
    private int taskId = -1;

    public ReverifyTask(CordSync plugin) {
        this.plugin = plugin;
    }

    public void start() {
        long interval = plugin.getConfig().getLong("link.reverify.interval-hours", 6);
        if (interval <= 0)
            interval = 6;
        long ticks = interval * 60 * 60 * 20;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, ticks, ticks);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
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
                plugin.getLogger().warning("âŒ Discord sunucu IDâ€™si bulunamadÄ±, doÄŸrulama atlandÄ±.");
                return;
            }

            if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null) {
                plugin.getLogger().warning("âŒ Discord bot aktif deÄŸil, doÄŸrulama kontrolÃ¼ yapÄ±lmadÄ±.");
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
                    .info("â™» ReVerify tamamlandÄ± â†’ Kontrol edilen: " + checked + ", kaldÄ±rÄ±lan: " + unlinked);
        } catch (Exception e) {
            plugin.getLogger().severe("âŒ ReVerify hatasÄ±: " + e.getMessage());
        }
    }

    public void executeNow() {
        try {
            runReverifyCheck();
        } catch (Exception e) {
            plugin.getLogger().severe("âŒ Manuel yeniden doÄŸrulama hatasÄ±: " + e.getMessage());
        }
    }
}
