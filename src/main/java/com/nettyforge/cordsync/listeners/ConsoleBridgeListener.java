package com.nettyforge.cordsync.listeners;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.bukkit.Bukkit;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.utils.MessageUtil;

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
    private Handler logHandler;

    public ConsoleBridgeListener(CordSync plugin) {
        this.plugin = plugin;
    }

    public void startCapture() {
        if (running)
            return;
        running = true;

        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record != null && record.getMessage() != null) {
                    String msg = "[" + record.getLevel().getName() + "] " + record.getMessage();
                    if (msg.length() > 1900)
                        msg = msg.substring(0, 1900) + "...";
                    messageQueue.offer(msg);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        };

        Bukkit.getLogger().addHandler(logHandler);

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
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
                plugin.getLogger().warning("Error in ConsoleBridge task: " + t.getMessage());
            }
        }, 60L, 60L); // 3 seconds
    }

    public void stop() {
        running = false;
        if (logHandler != null) {
            Bukkit.getLogger().removeHandler(logHandler);
            logHandler = null;
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

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

                EmbedBuilder embed = new EmbedBuilder()
                        .setDescription("```\n" + command + "\n```")
                        .addField(MessageUtil.getRaw("discord.console-bridge-field"), event.getAuthor().getAsMention(),
                                true)
                        .setColor(java.awt.Color.decode("#2B2D31"))
                        .setFooter("CordSync • Console Bridge")
                        .setTimestamp(Instant.now());

                event.getChannel().sendMessageEmbeds(embed.build()).queue();
            } catch (Exception e) {
                event.getMessage().reply("❌ Error: " + e.getMessage()).queue();
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
