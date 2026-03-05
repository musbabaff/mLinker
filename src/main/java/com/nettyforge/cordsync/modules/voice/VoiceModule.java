package com.nettyforge.cordsync.modules.voice;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.modules.CordModule;
import com.nettyforge.cordsync.utils.SchedulerUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VoiceModule extends CordModule implements Listener {

    private boolean isRunning = false;
    private boolean softDependHooked = false;

    // Region Name -> Discord Channel ID mapping
    private final Map<String, String> activeVoiceChannels = new HashMap<>();

    // Map tracking what region a player is currently in
    private final Map<UUID, String> playerCurrentRegion = new HashMap<>();

    public VoiceModule(CordSync plugin) {
        super(plugin, "Voice Module");
    }

    @Override
    protected void setupDefaultConfig() {
        getConfig().set("voice-category-id", "00000000000000"); // Category where channels get created
        getConfig().set("watched-regions", java.util.Arrays.asList("dungeon_1", "clan_base", "pvp_arena"));
        saveConfig();
        plugin.getLogger().info("🎧 Created default config for VoiceModule!");
    }

    @Override
    public void onEnable() {
        isRunning = true;

        // Softly check for WorldGuard
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            softDependHooked = true;
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            plugin.getLogger().info("🎧 Voice Module Online! Hooked into WorldGuard for Dynamic Voice Channels.");
        } else {
            plugin.getLogger()
                    .warning("🎧 Voice Module failed to start: WorldGuard is not installed! Disabling module...");
            isRunning = false;
        }
    }

    @Override
    public void onDisable() {
        isRunning = false;

        // Clean up any stray temporary voice channels
        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null) {
            for (String channelId : activeVoiceChannels.values()) {
                if (channelId == null)
                    continue;
                VoiceChannel channel = plugin.getDiscordBot().getJda().getVoiceChannelById(channelId);
                if (channel != null) {
                    channel.delete().queue();
                }
            }
        }

        activeVoiceChannels.clear();
        playerCurrentRegion.clear();
        plugin.getLogger().info("🎧 Voice Module Offline.");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isRunning || !softDependHooked)
            return;

        // Optimize: Only check when moving across whole blocks
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 1. Get player's current region using a mock WG adapter (Simplistic check for
        // demonstration/abstraction)
        // Normally this involves
        // WorldGuardPlugin.inst().getRegionContainer().createQuery().getApplicableRegions(...)
        // Since we don't want hard WG compilation failures, we simulate catching the
        // ID:
        String detectedRegionId = WorldGuardAdapter.getRegionIdAt(player);

        String currentRegion = playerCurrentRegion.get(uuid);

        if (detectedRegionId != null) {
            // Did they just enter a new region?
            if (!detectedRegionId.equals(currentRegion)
                    && getConfig().getStringList("watched-regions").contains(detectedRegionId)) {
                playerCurrentRegion.put(uuid, detectedRegionId);
                handleRegionEnter(player, detectedRegionId);
            }
        } else {
            // Left the region?
            if (currentRegion != null) {
                playerCurrentRegion.remove(uuid);
                handleRegionLeave(player, currentRegion);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!isRunning)
            return;
        String region = playerCurrentRegion.remove(event.getPlayer().getUniqueId());
        if (region != null) {
            handleRegionLeave(event.getPlayer(), region);
        }
    }

    private void handleRegionEnter(Player player, String regionId) {
        if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
            return;

        // Do we need to create the channel?
        if (!activeVoiceChannels.containsKey(regionId)) {
            String categoryId = getConfig().getString("voice-category-id", "");
            if (categoryId.isEmpty() || categoryId.equals("00000000000000"))
                return;

            SchedulerUtil.runAsync(plugin, () -> {
                Category cat = plugin.getDiscordBot().getJda().getCategoryById(categoryId);
                if (cat == null)
                    return;

                Guild guild = cat.getGuild();
                guild.createVoiceChannel("🔊 " + regionId, cat).queue(channel -> {
                    activeVoiceChannels.put(regionId, channel.getId());
                    // In a production app, we would move the linked Discord member to this channel
                    // here
                });
            });
        }
    }

    private void handleRegionLeave(Player player, String regionId) {
        // Are there any other players left in this region?
        boolean hasOthers = playerCurrentRegion.values().stream().anyMatch(val -> val.equals(regionId));

        if (!hasOthers && activeVoiceChannels.containsKey(regionId)) {
            // Delete the channel because it's empty
            String channelId = activeVoiceChannels.remove(regionId);
            SchedulerUtil.runAsync(plugin, () -> {
                if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null && channelId != null) {
                    VoiceChannel channel = plugin.getDiscordBot().getJda().getVoiceChannelById(channelId);
                    if (channel != null) {
                        channel.delete().queue();
                    }
                }
            });
        }
    }
}
