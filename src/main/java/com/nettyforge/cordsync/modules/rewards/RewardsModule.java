package com.nettyforge.cordsync.modules.rewards;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.modules.CordModule;
import com.nettyforge.cordsync.utils.SchedulerUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RewardsModule extends CordModule {

    private boolean isRunning = false;
    // Tracks consecutive minutes a user has spent in a valid voice state
    private final Map<Long, Integer> activeVoiceMinutes = new HashMap<>();

    public RewardsModule(CordSync plugin) {
        super(plugin, "Rewards Module");
    }

    @Override
    protected void setupDefaultConfig() {
        getConfig().set("reward-interval-minutes", 30);
        getConfig().set("reward-commands", java.util.Arrays.asList(
                "eco give %player% 500",
                "give %player% diamond 1"));
        getConfig().set("reward-message",
                "🎉 Congratulations! You received in-game rewards for hanging out in our Discord Voice Channels!");
        saveConfig();
        plugin.getLogger().info("⚙ Created default config for RewardsModule!");
    }

    @Override
    public void onEnable() {
        isRunning = true;
        startVoiceTracker();
        plugin.getLogger().info("🎁 Rewards Module hooked! Tracking Voice XP.");
    }

    @Override
    public void onDisable() {
        isRunning = false;
        activeVoiceMinutes.clear();
        plugin.getLogger().info("🎁 Rewards Module unhooked.");
    }

    private void startVoiceTracker() {
        // Runs every 1 minute (1200 ticks = 60 seconds)
        SchedulerUtil.runTimerAsync(plugin, () -> {
            if (!isRunning)
                return;
            if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
                return;

            int requiredMinutes = getConfig().getInt("reward-interval-minutes", 30);

            for (Guild guild : plugin.getDiscordBot().getJda().getGuilds()) {
                for (AudioChannel channel : guild.getVoiceChannels()) {
                    // Ignore AFK channels
                    if (channel.getName().toLowerCase().contains("afk"))
                        continue;
                    net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel afkChannel = guild.getAfkChannel();
                    if (afkChannel != null && channel.getId().equals(afkChannel.getId()))
                        continue;

                    for (Member member : channel.getMembers()) {
                        // Security: Ignore bots
                        if (member.getUser().isBot())
                            continue;

                        // Security: Ignore Self-Muted, Self-Deafened, Guild-Muted, Guild-Deafened
                        net.dv8tion.jda.api.entities.GuildVoiceState voiceState = member.getVoiceState();
                        if (voiceState == null)
                            continue;
                        if (voiceState.isMuted() || voiceState.isDeafened() ||
                                voiceState.isGuildMuted() || voiceState.isGuildDeafened()) {
                            // Reset their streak if they mute/deafen
                            activeVoiceMinutes.remove(member.getIdLong());
                            continue;
                        }

                        // Check if player is linked
                        UUID mappedUUID = plugin.getStorageProvider().getPlayerUUID(String.valueOf(member.getIdLong()));
                        if (mappedUUID == null)
                            continue;

                        // Increment minute counter
                        int currentMinutes = activeVoiceMinutes.getOrDefault(member.getIdLong(), 0) + 1;

                        if (currentMinutes >= requiredMinutes) {
                            // Reward time!
                            activeVoiceMinutes.put(member.getIdLong(), 0); // Reset after rewarding
                            grantReward(mappedUUID, member);
                        } else {
                            activeVoiceMinutes.put(member.getIdLong(), currentMinutes);
                        }
                    }
                }
            }
        }, 1200L, 1200L);
    }

    private void grantReward(UUID uuid, Member member) {
        String playerName = Bukkit.getOfflinePlayer(uuid).getName();
        if (playerName == null)
            return; // Player never joined the server, edge case

        List<String> commands = getConfig().getStringList("reward-commands");
        String message = getConfig().getString("reward-message",
                "🎉 Congratulations! You received in-game rewards for hanging out in our Discord Voice Channels!");

        // Run commands on primary Bukkit thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String cmd : commands) {
                String finalCmd = cmd.replace("%player%", playerName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
            }
        });

        // Try to DM the user
        member.getUser().openPrivateChannel().queue(
                pc -> pc.sendMessage(message + "").queue(
                        success -> {
                        },
                        error -> {
                            // DM failed (e.g. DMs turned off), we fail silently as they still got the game
                            // reward.
                        }),
                error -> {
                });
    }
}
