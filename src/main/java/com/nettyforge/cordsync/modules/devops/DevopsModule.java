package com.nettyforge.cordsync.modules.devops;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.modules.CordModule;
import com.nettyforge.cordsync.utils.SchedulerUtil;
import com.nettyforge.cordsync.utils.TPSMonitor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DevopsModule extends CordModule {

    private boolean isRunning = false;
    private long lastTpsAlarm = 0;

    // Feature 3: Broadcaster
    private AtomicInteger broadcastIndex = new AtomicInteger(0);

    // Feature 2: Smart Filter
    private ConsoleFilter consoleFilter;

    public DevopsModule(CordSync plugin) {
        super(plugin, "DevOps Module");
    }

    @Override
    protected void setupDefaultConfig() {
        // TPS Watchdog
        getConfig().set("tps-watchdog.enabled", true);
        getConfig().set("tps-watchdog.alarm-channel-id", "YOUR_ALARM_CHANNEL_ID");
        getConfig().set("tps-watchdog.ping-role-id", "YOUR_ADMIN_ROLE_ID"); // e.g. @here, or role ID
        getConfig().set("tps-watchdog.min-tps-alarm", 15.0);
        getConfig().set("tps-watchdog.alarm-cooldown-seconds", 300); // 5 minutes default
        getConfig().set("tps-watchdog.message", "⚠️ **CRITICAL TPS ALERT:** Server TPS dropped to `{tps}`!");

        // Smart Console Filter
        getConfig().set("console-filter.enabled", true);
        getConfig().set("console-filter.channel-id", "YOUR_DEV_RESTRICTED_CHANNEL_ID");
        getConfig().set("console-filter.keywords", java.util.Arrays.asList(
                "ERROR", "WARN", "Exception", "Severe"));

        // Auto-Broadcaster
        getConfig().set("broadcaster.enabled", true);
        getConfig().set("broadcaster.interval-seconds", 600);
        getConfig().set("broadcaster.discord-channel-id", "YOUR_ANNOUNCEMENT_CHANNEL_ID");
        getConfig().set("broadcaster.discord-embed-color", "#2B2D31");
        getConfig().set("broadcaster.messages", java.util.Arrays.asList(
                "Welcome to the server! Make sure to link your Discord with /link.",
                "Remember to vote for us daily to receive rewards!"));

        saveConfig();
        plugin.getLogger().info("⚙ Created default config for DevOpsModule!");
    }

    @Override
    public void onEnable() {
        getConfig();
        isRunning = true;

        if (getConfig().getBoolean("tps-watchdog.enabled")) {
            startTPSWatchdog();
        }

        if (getConfig().getBoolean("broadcaster.enabled")) {
            startAutoBroadcaster();
        }

        if (getConfig().getBoolean("console-filter.enabled")) {
            installConsoleFilter();
        }

        plugin.getLogger().info("🛠️ DevOps Module hooked!");
    }

    @Override
    public void onDisable() {
        isRunning = false;
        uninstallConsoleFilter();
        plugin.getLogger().info("🛠️ DevOps Module unhooked.");
    }

    // ==========================================
    // Feature 1: TPS Watchdog & Crash Alarm
    // ==========================================
    private void startTPSWatchdog() {
        SchedulerUtil.runAsyncTimer(plugin, () -> {
            if (!isRunning)
                return;

            double currentTps = TPSMonitor.getTPS();
            double minTps = getConfig().getDouble("tps-watchdog.min-tps-alarm", 15.0);

            if (currentTps < minTps) {
                long now = System.currentTimeMillis();
                long cooldownMs = getConfig().getLong("tps-watchdog.alarm-cooldown-seconds", 300) * 1000L;

                if (now - lastTpsAlarm > cooldownMs) {
                    lastTpsAlarm = now;
                    sendTpsAlarm(currentTps);
                }
            }
        }, 200L, 200L); // check every 10 seconds
    }

    private void sendTpsAlarm(double tps) {
        if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
            return;

        String channelId = getConfig().getString("tps-watchdog.alarm-channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_ALARM_CHANNEL_ID"))
            return;

        TextChannel channel = plugin.getDiscordBot().getJda().getTextChannelById(channelId);
        if (channel == null)
            return;

        String rolePing = getConfig().getString("tps-watchdog.ping-role-id", "");
        String msg = getConfig().getString("tps-watchdog.message",
                "⚠️ **CRITICAL TPS ALERT:** Server TPS dropped to `{tps}`!");
        msg = msg.replace("{tps}", String.valueOf(tps));

        if (!rolePing.isEmpty() && !rolePing.equals("YOUR_ADMIN_ROLE_ID")) {
            if (rolePing.equalsIgnoreCase("@here") || rolePing.equalsIgnoreCase("@everyone")) {
                msg = rolePing + " " + msg;
            } else {
                msg = "<@&" + rolePing + "> " + msg;
            }
        }
        channel.sendMessage(msg + "").queue();
    }

    // ==========================================
    // Feature 3: Auto-Broadcaster
    // ==========================================
    private void startAutoBroadcaster() {
        long intervalSec = getConfig().getLong("broadcaster.interval-seconds", 600);

        SchedulerUtil.runAsyncTimer(plugin, () -> {
            if (!isRunning)
                return;

            List<String> messages = getConfig().getStringList("broadcaster.messages");
            if (messages.isEmpty())
                return;

            int i = broadcastIndex.getAndUpdate(v -> (v + 1) % messages.size());
            String message = messages.get(i);

            // Send to Game
            String gameMessage = org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
            SchedulerUtil.runSync(plugin, () -> Bukkit.broadcastMessage(gameMessage));

            // Send to Discord
            if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null) {
                String channelId = getConfig().getString("broadcaster.discord-channel-id", "");
                if (!channelId.isEmpty() && !channelId.equals("YOUR_ANNOUNCEMENT_CHANNEL_ID")) {
                    TextChannel channel = plugin.getDiscordBot().getJda().getTextChannelById(channelId);
                    if (channel != null) {
                        String color = getConfig().getString("broadcaster.discord-embed-color", "#2B2D31");
                        EmbedBuilder embed = new EmbedBuilder()
                                .setDescription(message.replace("&", ""))
                                .setColor(java.awt.Color.decode(color));
                        channel.sendMessageEmbeds(embed.build()).queue();
                    }
                }
            }
        }, intervalSec * 20L, intervalSec * 20L);
    }

    // ==========================================
    // Feature 2: Smart Console Filter (Log4j)
    // ==========================================
    private void installConsoleFilter() {
        java.util.logging.Logger rootLogger = Bukkit.getLogger();
        consoleFilter = new ConsoleFilter();
        rootLogger.addHandler(new java.util.logging.Handler() {
            @Override
            public void publish(java.util.logging.LogRecord record) {
                consoleFilter.isLoggable(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });
    }

    private void uninstallConsoleFilter() {
        // Safe to leave handler, it does nothing if isRunning = false.
    }

    private class ConsoleFilter implements java.util.logging.Filter {
        @Override
        public boolean isLoggable(java.util.logging.LogRecord record) {
            if (!isRunning)
                return true;
            String msg = record.getMessage();
            if (msg == null)
                return true;

            // PREVENT INFINITE DISCORD LOOP
            if (Thread.currentThread().getName().contains("JDA")
                    || Thread.currentThread().getName().contains("OkHttp")) {
                return true;
            }

            List<String> keywords = getConfig().getStringList("console-filter.keywords");
            for (String kw : keywords) {
                if (msg.contains(kw)) {
                    // Include any stacktrace if present
                    if (record.getThrown() != null) {
                        msg += "\n" + record.getThrown().toString();
                    }
                    broadcastDevError(msg);
                    break;
                }
            }
            return true;
        }
    }

    private void broadcastDevError(String consoleError) {
        if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
            return;

        String channelId = getConfig().getString("console-filter.channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_DEV_RESTRICTED_CHANNEL_ID"))
            return;

        TextChannel channel = plugin.getDiscordBot().getJda().getTextChannelById(channelId);
        if (channel == null)
            return;

        // Ensure not too long for discord (limit ~2000 chars)
        if (consoleError.length() > 1900) {
            consoleError = consoleError.substring(0, 1900) + "...";
        }

        channel.sendMessage("```text\n" + consoleError + "\n```").queue();
    }
}
