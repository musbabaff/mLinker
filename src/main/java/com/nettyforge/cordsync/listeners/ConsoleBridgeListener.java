package com.nettyforge.cordsync.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import org.bukkit.Bukkit;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.utils.MessageUtil;
import com.nettyforge.cordsync.utils.SchedulerUtil;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;

@SuppressWarnings("null")
public class ConsoleBridgeListener extends ListenerAdapter {

    private final CordSync plugin;
    private final LinkedBlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private boolean running = false;
    private AbstractFilter log4jFilter;

    public ConsoleBridgeListener(CordSync plugin) {
        this.plugin = plugin;
    }

    public void startCapture() {
        if (running)
            return;
        running = true;

        Logger coreLogger = (Logger) LogManager.getRootLogger();

        log4jFilter = new AbstractFilter() {
            @Override
            public Result filter(LogEvent event) {
                if (!running) return Result.NEUTRAL;
                
                String loggerName = event.getLoggerName();
                if (loggerName != null && (loggerName.startsWith("net.dv8tion") || loggerName.startsWith("okhttp3"))) {
                    return Result.NEUTRAL;
                }

                String msg = event.getMessage().getFormattedMessage();
                if (msg == null || msg.trim().isEmpty()) return Result.NEUTRAL;
                
                // Infinite loop protection
                if (msg.contains("CordSync \u2022 Console Bridge") 
                    || msg.contains("Console Bridge active")
                    || msg.contains("rate limit")
                    || msg.contains("Unknown error while executing")) {
                    return Result.NEUTRAL; 
                }

                String formatted = "[" + event.getLevel().name() + "] " + msg;
                formatted = formatted.replaceAll("\u001B\\[[;\\d]*m", "");
                
                if (formatted.length() > 1900) {
                    formatted = formatted.substring(0, 1900) + "...";
                }
                
                messageQueue.offer(formatted);
                return Result.NEUTRAL;
            }
        };

        log4jFilter.start();
        coreLogger.addFilter(log4jFilter);

        SchedulerUtil.runAsyncTimer(plugin, () -> {
            try {
                if (!running || messageQueue.isEmpty())
                    return;

                StringBuilder batch = new StringBuilder();
                String line;
                while ((line = messageQueue.poll()) != null) {
                    if (batch.length() + line.length() + 1 > 1900) {
                        sendConsoleMessage(batch.toString());
                        batch = new StringBuilder();
                    }
                    batch.append(line).append("\n");
                }
                if (batch.length() > 0) {
                    sendConsoleMessage(batch.toString());
                }
            } catch (Throwable t) {
                // Ignore errors here to prevent infinite loop logging.
            }
        }, 30L, 30L); // 1.5 seconds
    }

    public void stop() {
        running = false;
        if (log4jFilter != null) {
            Logger coreLogger = (Logger) LogManager.getRootLogger();
            try {
                coreLogger.getClass().getMethod("removeFilter", Filter.class).invoke(coreLogger, log4jFilter);
            } catch (Exception ignored) { }
            log4jFilter.stop();
            log4jFilter = null;
        }
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (!plugin.getConfig().getBoolean("console-bridge.enabled", false))
            return;
        if (event.getAuthor().isBot())
            return;

        String channelId = plugin.getConfig().getString("console-bridge.channel-id", "");
        if (channelId == null || channelId.isEmpty())
            return;
        if (!event.getChannel().getId().equals(channelId))
            return;

        String allowedRoleId = plugin.getConfig().getString("console-bridge.admin-role-id", "");
        if (allowedRoleId != null && !allowedRoleId.isEmpty()) {
            if (event.getMember() == null)
                return;
            boolean hasRole = event.getMember().getRoles().stream()
                    .anyMatch(r -> r.getId().equals(allowedRoleId));
            if (!hasRole) {
                event.getMessage().reply(MessageUtil.getRaw("discord.console-bridge-no-permission")).queue();
                return;
            }
        }

        String command = event.getMessage().getContentRaw().trim();
        if (command.isEmpty())
            return;

        String blockedStr = plugin.getConfig().getString("console-bridge.blocked-commands", "stop,restart,reload");
        if (blockedStr != null) {
            String[] blocked = blockedStr.split(",");
            for (String b : blocked) {
                if (command.toLowerCase().startsWith(b.trim().toLowerCase())) {
                    event.getMessage().reply(MessageUtil.getRaw("discord.console-bridge-blocked")).queue();
                    return;
                }
            }
        }

        SchedulerUtil.runSync(plugin, () -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

                EmbedBuilder embed = new EmbedBuilder()
                        .setDescription("```\n" + command + "\n```")
                        .addField(MessageUtil.getRaw("discord.console-bridge-field"), event.getAuthor().getAsMention(),
                                true)
                        .setColor(java.awt.Color.decode("#2B2D31"))
                        .setFooter(MessageUtil.getRaw("discord.console-bridge-footer"))
                        .setTimestamp(Instant.now());

                event.getChannel().sendMessageEmbeds(embed.build()).queue();
            } catch (Exception e) {
                event.getMessage().reply(MessageUtil.getRaw("discord.console-bridge-error").replace("{error}", e.getMessage())).queue();
            }
        });
    }

    private void sendConsoleMessage(String text) {
        if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
            return;
        String channelId = plugin.getConfig().getString("console-bridge.channel-id", "");
        if (channelId == null || channelId.isEmpty())
            return;
        try {
            TextChannel channel = plugin.getDiscordBot().getJda().getTextChannelById(channelId);
            if (channel == null)
                return;
            channel.sendMessage("```\n" + text + "\n```").queue();
        } catch (Exception ignored) {
        }
    }
}
