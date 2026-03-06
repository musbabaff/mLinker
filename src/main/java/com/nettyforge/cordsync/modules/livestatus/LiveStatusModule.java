package com.nettyforge.cordsync.modules.livestatus;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.modules.CordModule;
import com.nettyforge.cordsync.utils.SchedulerUtil;
import com.nettyforge.cordsync.utils.TPSMonitor;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import org.bukkit.Bukkit;

import java.awt.Color;
import java.time.Instant;

/**
 * Live Status Module — Spark-Optimized.
 * 
 * Performance Design:
 * - Config values are cached on enable (not re-read every cycle).
 * - Chunks/Entities come from TPSMonitor's cached volatile fields (zero
 * iteration).
 * - EmbedBuilder is reused conceptually (no extra objects beyond JDA's own).
 * - Data capture on main thread is a single volatile read per field.
 */
@SuppressWarnings("null")
public class LiveStatusModule extends CordModule {

    private boolean isRunning = false;
    private TPSMonitor monitorInstance;

    // ── Cached Config Values (read once on enable) ──
    private String cachedChannelId;
    private String cachedMessageId;
    private String cachedTitle;
    private Color cachedColor;
    private long cachedIntervalTicks;

    public LiveStatusModule(CordSync plugin) {
        super(plugin, "Live Status");
    }

    @Override
    protected void setupDefaultConfig() {
        getConfig().set("discord.status-channel-id", "YOUR_CHANNEL_ID_HERE");
        getConfig().set("discord.message-id", "YOUR_MESSAGE_ID_HERE");
        getConfig().set("discord.update-interval", 30);
        getConfig().set("messages.embed-title", "📊 Live Server Status");
        getConfig().set("messages.embed-color", "#2B2D31");
        saveConfig();
        plugin.getLogger().info("⚙ Created default config for LiveStatusModule!");
    }

    @Override
    public void onEnable() {
        getConfig();

        // SPARK: Cache all config values once — no per-cycle getString calls
        cachedChannelId = getConfig().getString("discord.status-channel-id", "");
        cachedMessageId = getConfig().getString("discord.message-id", "");
        cachedTitle = getConfig().getString("messages.embed-title", "📊 Live Server Status");
        long intervalSec = getConfig().getLong("discord.update-interval", 30);
        cachedIntervalTicks = intervalSec * 20L;

        String colorHex = getConfig().getString("messages.embed-color", "#2B2D31");
        try {
            cachedColor = Color.decode(colorHex);
        } catch (Exception e) {
            cachedColor = Color.decode("#2B2D31");
        }

        // Validate early — don't start timers if config is invalid
        if (cachedChannelId.isEmpty() || cachedChannelId.equals("YOUR_CHANNEL_ID_HERE") ||
                cachedMessageId.isEmpty() || cachedMessageId.equals("YOUR_MESSAGE_ID_HERE")) {
            plugin.getLogger().warning("📊 Live Status: channel-id or message-id not configured. Skipping.");
            return;
        }

        // Start TPS & MSPT Monitor
        if (monitorInstance == null) {
            monitorInstance = new TPSMonitor(plugin);
        }

        isRunning = true;
        updateLoop();

        plugin.getLogger().info("📊 Live Status Module hooked! Updates every " + intervalSec + "s with MSPT data.");
    }

    @Override
    public void onDisable() {
        isRunning = false;
        plugin.getLogger().info("📊 Live Status Module unhooked.");
    }

    private void updateLoop() {
        if (!isRunning)
            return;

        // SPARK: Read cached volatile values from TPSMonitor — zero iteration, zero
        // allocation
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        int chunks = TPSMonitor.getTotalLoadedChunks(); // Cached volatile
        int entities = TPSMonitor.getTotalEntities(); // Cached volatile

        SchedulerUtil.runAsync(plugin, () -> {
            updateStatusEmbed(online, max, chunks, entities);

            if (isRunning) {
                SchedulerUtil.runTaskLater(plugin, this::updateLoop, cachedIntervalTicks);
            }
        });
    }

    private void updateStatusEmbed(int online, int max, int chunks, int entities) {
        if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
            return;

        TextChannel channel = plugin.getDiscordBot().getJda()
                .getTextChannelById(cachedChannelId != null ? cachedChannelId : "");
        if (channel == null)
            return;

        // ── Gather Performance Data (volatile reads, no computation) ──
        double tps = TPSMonitor.getTPS();
        String ram = TPSMonitor.getRamUsage();
        String uptime = TPSMonitor.getUptime();
        double mspt1m = TPSMonitor.getMspt1m();
        double mspt5m = TPSMonitor.getMspt5m();
        double mspt15m = TPSMonitor.getMspt15m();

        String tpsEmoji = tps >= 18.0 ? "🟢" : (tps >= 14.0 ? "🟡" : "🔴");
        String healthEmoji = TPSMonitor.getMsptHealthEmoji(mspt1m);

        // Build embed — JDA handles its own object lifecycle
        EmbedBuilder embed = new EmbedBuilder()
                .setDescription("### " + cachedTitle)
                .addField("🌍 Online Players", "`" + online + " / " + max + "`", true)
                .addField("⚡ Server TPS", tpsEmoji + " `" + String.format("%.2f", tps) + "`", true)
                .addField("💾 RAM Usage", "`" + ram + "`", true)
                .addField(healthEmoji + " Performance Health",
                        "```\nMSPT  1m:  " + String.format("%6.2f", mspt1m) + " ms\n" +
                                "MSPT  5m:  " + String.format("%6.2f", mspt5m) + " ms\n" +
                                "MSPT 15m:  " + String.format("%6.2f", mspt15m) + " ms\n```",
                        false)
                .addField("🧩 Loaded Chunks", "`" + chunks + "`", true)
                .addField("👾 Entities", "`" + entities + "`", true)
                .addField("⏱️ Uptime", "`" + uptime + "`", true)
                .setColor(cachedColor)
                .setFooter("CordSync Performance Monitor • Auto-Refresh")
                .setTimestamp(Instant.now());

        if (Bukkit.getPluginManager().getPlugin("spark") != null) {
            embed.setAuthor("⚡ Powered by Spark", null, null);
        }

        channel.retrieveMessageById(cachedMessageId != null ? cachedMessageId : "").queue(
                message -> message.editMessageEmbeds(embed.build()).queue(),
                error -> plugin.getLogger().warning("Live Status: message-id not found. Update config."));
    }
}
