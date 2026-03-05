package com.nettyforge.cordsync.modules.leaderboard;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.modules.CordModule;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.awt.Color;
import java.time.Instant;
import java.util.*;

/**
 * Dynamic Leaderboard Module — supports unlimited boards via config.
 * Integrates with ajLeaderboards API (soft-depend) or PlaceholderAPI fallback.
 * Each board is independently scheduled and edits its own Discord message.
 */
public class LeaderboardModule extends CordModule {

    private boolean isRunning = false;
    private final List<Integer> activeTaskIds = new ArrayList<>();

    // Cached availability flags
    private static Boolean ajlbAvailable = null;
    private static Boolean papiAvailable = null;

    public LeaderboardModule(CordSync plugin) {
        super(plugin, "Leaderboard Module");
    }

    @Override
    protected void setupDefaultConfig() {
        // Example board: top kills
        getConfig().set("boards.top_kills.channel-id", "YOUR_CHANNEL_ID");
        getConfig().set("boards.top_kills.message-id", "");
        getConfig().set("boards.top_kills.title", "⚔️ Top 10 Killers");
        getConfig().set("boards.top_kills.ajlb-board", "statistic_player_kills");
        getConfig().set("boards.top_kills.update-interval", 600);
        getConfig().set("boards.top_kills.embed-color", "#FFD700");

        // Example board: top money
        getConfig().set("boards.top_money.channel-id", "YOUR_CHANNEL_ID");
        getConfig().set("boards.top_money.message-id", "");
        getConfig().set("boards.top_money.title", "💸 Top 10 Richest Players");
        getConfig().set("boards.top_money.ajlb-board", "vault_eco_balance");
        getConfig().set("boards.top_money.update-interval", 600);
        getConfig().set("boards.top_money.embed-color", "#2ECC71");

        saveConfig();
        plugin.getLogger().info("🏆 Created default config for LeaderboardModule!");
    }

    @Override
    public void onEnable() {
        isRunning = true;

        // Check soft-dependencies once
        ajlbAvailable = Bukkit.getPluginManager().getPlugin("ajLeaderboards") != null;
        papiAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        if (!ajlbAvailable && !papiAvailable) {
            plugin.getLogger().warning(
                    "🏆 Neither ajLeaderboards nor PlaceholderAPI found! Leaderboard Module requires at least one.");
            isRunning = false;
            return;
        }

        plugin.getLogger().info("🏆 Leaderboard Module Online! " +
                (ajlbAvailable ? "[ajLeaderboards ✓]" : "[ajLeaderboards ✗]") + " " +
                (papiAvailable ? "[PlaceholderAPI ✓]" : "[PlaceholderAPI ✗]"));

        // Start a separate async timer for each configured board
        ConfigurationSection boardsSection = getConfig().getConfigurationSection("boards");
        if (boardsSection == null)
            return;

        for (String boardKey : boardsSection.getKeys(false)) {
            ConfigurationSection board = boardsSection.getConfigurationSection(boardKey);
            if (board == null)
                continue;

            long intervalSeconds = board.getLong("update-interval", 600);
            long intervalTicks = intervalSeconds * 20L;

            int taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                if (!isRunning)
                    return;
                updateBoard(boardKey, board);
            }, 100L, intervalTicks).getTaskId();

            activeTaskIds.add(taskId);
            plugin.getLogger().info("  📋 Board '" + boardKey + "' scheduled every " + intervalSeconds + "s");
        }
    }

    @Override
    public void onDisable() {
        isRunning = false;
        for (int taskId : activeTaskIds) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        activeTaskIds.clear();
        plugin.getLogger().info("🏆 Leaderboard Module Offline.");
    }

    // ═══════════════════════════════════════════════════════════
    // Core: Update a single board
    // ═══════════════════════════════════════════════════════════

    private void updateBoard(String boardKey, ConfigurationSection board) {
        if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
            return;

        String channelId = board.getString("channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_CHANNEL_ID"))
            return;

        TextChannel channel = plugin.getDiscordBot().getJda().getTextChannelById(channelId);
        if (channel == null)
            return;

        String title = board.getString("title", "🏆 Leaderboard");
        String ajlbBoard = board.getString("ajlb-board", "");
        String colorHex = board.getString("embed-color", "#FFD700");

        // ── Fetch top 10 data ──
        List<Map.Entry<String, String>> topEntries = fetchLeaderboardData(ajlbBoard, 10);

        // ── Build the embed description ──
        StringBuilder desc = new StringBuilder();
        if (topEntries.isEmpty()) {
            desc.append("*No data available yet. Waiting for players...*\n");
        } else {
            for (int i = 0; i < topEntries.size(); i++) {
                Map.Entry<String, String> entry = topEntries.get(i);
                String medal = getMedalEmoji(i);
                desc.append(medal).append(" **").append(entry.getKey())
                        .append("** — ").append(entry.getValue()).append("\n");
            }
        }

        Color embedColor;
        try {
            embedColor = Color.decode(colorHex);
        } catch (Exception e) {
            embedColor = Color.YELLOW;
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setDescription("### " + title + "\n\n" + desc)
                .setColor(embedColor)
                .setFooter("CordSync Dynamic Leaderboards • Auto-Refresh")
                .setTimestamp(Instant.now());

        if (ajlbAvailable) {
            eb.setAuthor("📊 Powered by ajLeaderboards", null, null);
        }

        // ── Edit or Send ──
        String messageId = board.getString("message-id", "");
        if (messageId != null && !messageId.isEmpty()) {
            channel.retrieveMessageById(messageId).queue(
                    msg -> msg.editMessageEmbeds(eb.build()).queue(),
                    error -> {
                        // Message gone — send a new one and save the ID
                        sendNewMessage(channel, eb, boardKey);
                    });
        } else {
            sendNewMessage(channel, eb, boardKey);
        }
    }

    private void sendNewMessage(TextChannel channel, EmbedBuilder eb, String boardKey) {
        channel.sendMessageEmbeds(eb.build()).queue(newMsg -> {
            getConfig().set("boards." + boardKey + ".message-id", newMsg.getId());
            saveConfig();
            plugin.getLogger()
                    .info("🏆 New leaderboard message created for board '" + boardKey + "'. ID saved to config.");
        });
    }

    // ═══════════════════════════════════════════════════════════
    // Data Fetching: ajLeaderboards API → PAPI fallback
    // ═══════════════════════════════════════════════════════════

    private List<Map.Entry<String, String>> fetchLeaderboardData(String boardName, int limit) {
        List<Map.Entry<String, String>> results = new ArrayList<>();

        // Priority 1: ajLeaderboards API via reflection
        if (ajlbAvailable && !boardName.isEmpty()) {
            results = fetchFromAjLeaderboards(boardName, limit);
            if (!results.isEmpty())
                return results;
        }

        // Priority 2: PlaceholderAPI — parse
        // %ajlb_board_<boardName>_<position>_name/value%
        if (papiAvailable && !boardName.isEmpty()) {
            results = fetchFromPAPI(boardName, limit);
            if (!results.isEmpty())
                return results;
        }

        return results;
    }

    /**
     * Fetch leaderboard data directly from ajLeaderboards API via reflection.
     * No compile-time dependency needed.
     */
    private List<Map.Entry<String, String>> fetchFromAjLeaderboards(String boardName, int limit) {
        List<Map.Entry<String, String>> results = new ArrayList<>();
        try {
            Object plugin_inst = Bukkit.getPluginManager().getPlugin("ajLeaderboards");
            if (plugin_inst == null)
                return results;

            // getTopManager()
            Object topManager = plugin_inst.getClass().getMethod("getTopManager").invoke(plugin_inst);

            // getTopManager().getTop(boardName)
            // This returns List<StatEntry> with getName() and getScoreFormatted()
            List<?> topList = (List<?>) topManager.getClass()
                    .getMethod("getTop", String.class)
                    .invoke(topManager, boardName);

            if (topList == null)
                return results;

            int count = Math.min(topList.size(), limit);
            for (int i = 0; i < count; i++) {
                Object entry = topList.get(i);
                String name = (String) entry.getClass().getMethod("getName").invoke(entry);
                String value = (String) entry.getClass().getMethod("getScoreFormatted").invoke(entry);
                if (name != null && !name.isEmpty()) {
                    results.add(new AbstractMap.SimpleEntry<>(name, value != null ? value : "0"));
                }
            }
        } catch (Throwable t) {
            // API signature might differ between versions — graceful fallback
            plugin.getLogger().fine("ajLeaderboards API reflection error (falling back to PAPI): " + t.getMessage());
        }
        return results;
    }

    /**
     * Fetch leaderboard data via PlaceholderAPI placeholders.
     * Standard ajLeaderboards PAPI format:
     * %ajlb_lb_<board>_<position>_<name|value>%
     */
    private List<Map.Entry<String, String>> fetchFromPAPI(String boardName, int limit) {
        List<Map.Entry<String, String>> results = new ArrayList<>();
        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            java.lang.reflect.Method setPlaceholders = papiClass.getMethod("setPlaceholders",
                    OfflinePlayer.class, String.class);

            // We need a dummy player to resolve — use any online player or first offline
            OfflinePlayer dummy = null;
            if (!Bukkit.getOnlinePlayers().isEmpty()) {
                dummy = Bukkit.getOnlinePlayers().iterator().next();
            } else {
                OfflinePlayer[] offlines = Bukkit.getOfflinePlayers();
                if (offlines.length > 0)
                    dummy = offlines[0];
            }
            if (dummy == null)
                return results;

            for (int pos = 1; pos <= limit; pos++) {
                String namePlaceholder = "%ajlb_lb_" + boardName + "_" + pos + "_name%";
                String valuePlaceholder = "%ajlb_lb_" + boardName + "_" + pos + "_value%";

                String name = (String) setPlaceholders.invoke(null, dummy, namePlaceholder);
                String value = (String) setPlaceholders.invoke(null, dummy, valuePlaceholder);

                // If PAPI couldn't parse, the raw placeholder string is returned
                if (name != null && !name.startsWith("%") && !name.isEmpty()) {
                    results.add(new AbstractMap.SimpleEntry<>(name, value != null ? value : "0"));
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().fine("PAPI leaderboard fetch error: " + t.getMessage());
        }
        return results;
    }

    // ═══════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════

    private String getMedalEmoji(int index) {
        return switch (index) {
            case 0 -> "🥇";
            case 1 -> "🥈";
            case 2 -> "🥉";
            default -> "🏅";
        };
    }
}
