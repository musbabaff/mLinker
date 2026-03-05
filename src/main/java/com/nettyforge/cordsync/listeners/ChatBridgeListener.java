package com.nettyforge.cordsync.listeners;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.nettyforge.cordsync.CordSync;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;

/**
 * Chat Bridge Listener: MC → Discord.
 * Uses Discord Webhooks to show player MC skin as avatar.
 */
public class ChatBridgeListener implements Listener {

    private final CordSync plugin;

    public ChatBridgeListener(CordSync plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat-bridge.enabled", false))
            return;

        Player player = event.getPlayer();
        String message = event.getMessage();

        // Check MC → Discord permission
        String mode = plugin.getConfig().getString("chat-bridge.mc-to-discord", "ALL");
        if ("LINKED_ONLY".equalsIgnoreCase(mode)) {
            if (!plugin.getStorageProvider().isPlayerLinked(player.getUniqueId()))
                return;
        }

        // Check permission node
        if (!player.hasPermission("cordsync.chatbridge"))
            return;

        // Sanitize message (prevent @everyone)
        String sanitized = message
                .replace("@everyone", "@ everyone")
                .replace("@here", "@ here");

        // Send via webhook or regular message
        String webhookUrl = plugin.getConfig().getString("chat-bridge.webhook-url", "");
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            sendWebhookMessage(player, sanitized);
        } else if (plugin.getDiscordBot() != null) {
            String format = plugin.getConfig().getString("chat-bridge.discord-format",
                    "**{player}**: {message}");
            String formatted = format.replace("{player}", player.getName())
                    .replace("{message}", sanitized);
            plugin.getDiscordBot().sendChatBridgeMessage(formatted);
        }
    }

    /**
     * Send via Discord Webhook — player's MC skin as avatar!
     */
    private void sendWebhookMessage(Player player, String message) {
        String webhookUrl = plugin.getConfig().getString("chat-bridge.webhook-url", "");
        if (webhookUrl == null || webhookUrl.isEmpty())
            return;

        try {
            UUID uuid = player.getUniqueId();
            String skinUrl = "https://mc-heads.net/avatar/" + uuid + "/128";

            // Use JDA's webhook support
            if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null) {
                String channelId = plugin.getConfig().getString("chat-bridge.channel-id", "");
                if (channelId == null || channelId.isEmpty())
                    return;

                TextChannel channel = plugin.getDiscordBot().getJda().getTextChannelById(channelId);
                if (channel == null)
                    return;

                // Use the webhook URL directly via HTTP
                try (WebhookClient client = WebhookClient.withUrl(webhookUrl)) {
                    WebhookMessageBuilder builder = new WebhookMessageBuilder()
                            .setUsername(player.getName())
                            .setAvatarUrl(skinUrl)
                            .setContent(message);
                    client.send(builder.build());
                } catch (Exception e) {
                    // Fallback: use regular bot message
                    plugin.getDiscordBot().sendChatBridgeMessage("**" + player.getName() + "**: " + message);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("⚠ Webhook send failed: " + e.getMessage());
        }
    }
}
