package com.nettyforge.cordsync.listeners;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.storage.StorageProvider;
import com.nettyforge.cordsync.utils.MessageUtil;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import javax.annotation.Nonnull;
import java.time.Instant;

/**
 * 2FA Login System: IP verification + Discord approval on server join.
 * When a linked player joins from an unknown IP, they must approve via Discord.
 */
@SuppressWarnings("null")
public class LoginVerifyListener extends ListenerAdapter implements Listener {

    private final CordSync plugin;

    // Players waiting for Discord approval: UUID -> login data (STATIC to guarantee
    // single source of truth)
    private static final ConcurrentHashMap<UUID, PendingLogin> pendingLogins = new ConcurrentHashMap<>();

    // Approved session cache: UUID -> approved IP (STATIC for same reason)
    private static final ConcurrentHashMap<UUID, String> approvedSessions = new ConcurrentHashMap<>();

    public LoginVerifyListener(CordSync plugin) {
        this.plugin = plugin;
    }

    // ===================================================================
    // BUKKIT: Player Login Event
    // ===================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!plugin.getConfig().getBoolean("security.2fa-login.enabled", false))
            return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        StorageProvider storage = plugin.getStorageProvider();

        // Only for linked players
        if (!storage.isPlayerLinked(uuid))
            return;

        // Check if player opted-in into 2FA!
        if (!plugin.getTwoFactorEnabled(uuid))
            return;

        String currentIp = getIpAddress(event.getAddress());
        String discordId = storage.getDiscordId(uuid);
        if (discordId == null)
            return;

        // Check if session is already approved
        String approvedIp = approvedSessions.get(uuid);
        if (approvedIp != null && approvedIp.equals(currentIp)) {
            return; // Already approved for this IP
        }

        // Send Discord 2FA request
        pendingLogins.put(uuid, new PendingLogin(player.getName(), discordId, currentIp));
        sendDiscordApproval(uuid, player.getName(), discordId, currentIp);

        // Kick with message — player must approve via Discord first
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                MessageUtil.get("security.2fa-login-pending"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("security.2fa-login.enabled", false))
            return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        StorageProvider storage = plugin.getStorageProvider();

        if (!storage.isPlayerLinked(uuid))
            return;

        String currentIp = "";
        if (player.getAddress() != null) {
            currentIp = player.getAddress().getAddress().getHostAddress();
        }

        // Log the join
        sendJoinLog(player.getName(), storage.getDiscordId(uuid), currentIp, true);
    }

    // ===================================================================
    // DISCORD: Button Interaction for 2FA
    // ===================================================================

    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        // Approve login
        if (buttonId.startsWith("cordsync_2fa_approve_")) {
            String uuidStr = buttonId.replace("cordsync_2fa_approve_", "");
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (Exception e) {
                return;
            }

            PendingLogin pending = pendingLogins.remove(uuid);
            if (pending == null) {
                event.reply("⏰ This request has expired.").setEphemeral(true).queue();
                return;
            }

            // Verify it's the correct Discord user
            if (!event.getUser().getId().equals(pending.discordId)) {
                event.reply("❌ This request is not for you!").setEphemeral(true).queue();
                pendingLogins.put(uuid, pending);
                return;
            }

            // Approve the session
            approvedSessions.put(uuid, pending.ipAddress);

            // Auto-expire session after configured time
            long sessionMinutes = plugin.getConfig().getLong("security.2fa-login.session-duration", 60);
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,
                    () -> approvedSessions.remove(uuid),
                    20L * 60 * sessionMinutes);

            EmbedBuilder embed = new EmbedBuilder()

                    .setDescription(
                            "You approved the login for **" + pending.playerName + "**.\nThey can now join the server.")
                    .addField("🌐 IP Address", censorIp(pending.ipAddress), true)
                    .addField("⏰ Session", sessionMinutes + " minutes", true)
                    .setColor(java.awt.Color.decode("#2B2D31"))
                    .setFooter("CordSync • 2FA Login")
                    .setTimestamp(Instant.now());

            event.replyEmbeds(embed.build()).setEphemeral(true).queue();

            plugin.getLogger()
                    .info("✅ 2FA approved for " + pending.playerName + " from " + censorIp(pending.ipAddress));
            return;
        }

        // Deny login
        if (buttonId.startsWith("cordsync_2fa_deny_")) {
            String uuidStr = buttonId.replace("cordsync_2fa_deny_", "");
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (Exception e) {
                return;
            }

            PendingLogin pending = pendingLogins.remove(uuid);
            if (pending == null) {
                event.reply("⏰ This request has expired.").setEphemeral(true).queue();
                return;
            }

            if (!event.getUser().getId().equals(pending.discordId)) {
                event.reply("❌ This request is not for you!").setEphemeral(true).queue();
                pendingLogins.put(uuid, pending);
                return;
            }

            EmbedBuilder embed = new EmbedBuilder()

                    .setDescription("### 🚫 Login Denied\n\nLogin attempt for **" + pending.playerName
                            + "** was denied.\n⚠️ If this wasn't you, your account may be compromised!")
                    .addField("🌐 IP Address", censorIp(pending.ipAddress), true)
                    .setColor(java.awt.Color.decode("#2B2D31"))
                    .setFooter("CordSync • 2FA Login")
                    .setTimestamp(Instant.now());

            event.replyEmbeds(embed.build()).setEphemeral(true).queue();

            plugin.getLogger()
                    .warning("🚫 2FA denied for " + pending.playerName + " from " + censorIp(pending.ipAddress));
        }
    }

    // ===================================================================
    // DISCORD: Send Approval Request
    // ===================================================================

    private void sendDiscordApproval(UUID uuid, String playerName, String discordId, String ip) {
        if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
            return;

        // ASYNC: Use .queue() instead of blocking .complete() to prevent main thread
        // freeze
        plugin.getDiscordBot().getJda().retrieveUserById(discordId).queue(user -> {
            if (user == null)
                return;

            String serverName = plugin.getConfig().getString("server-name", "Minecraft Server");

            EmbedBuilder embed = new EmbedBuilder()

                    .setDescription("### 🔐 2FA Login Request\n\nSomeone is trying to join **" + serverName
                            + "** with your linked account.\n\nIs this you?")
                    .addField("👤 Username", playerName, true)
                    .addField("🌐 IP Address", censorIp(ip), true)
                    .addField("📅 Time", "<t:" + Instant.now().getEpochSecond() + ":f>", true)
                    .setColor(java.awt.Color.decode("#2B2D31"))
                    .setFooter("CordSync • 2FA Verification")
                    .setTimestamp(Instant.now());

            user.openPrivateChannel().queue(channel -> {
                channel.sendMessageEmbeds(embed.build())
                        .addActionRow(
                                Button.success("cordsync_2fa_approve_" + uuid, "✅ Approve"),
                                Button.danger("cordsync_2fa_deny_" + uuid, "🚫 Deny"))
                        .queue(
                                success -> plugin.getLogger().info("📨 2FA request sent to " + user.getName()),
                                failure -> plugin.getLogger().warning("⚠ Could not DM user: " + failure.getMessage()));
            });
        }, throwable -> {
            plugin.getLogger().warning("⚠ 2FA request failed: " + throwable.getMessage());
        });

        // Auto-expire after 5 minutes (300 seconds = 6000 ticks)
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,
                () -> pendingLogins.remove(uuid), 20L * 300);
    }

    // ===================================================================
    // LOG: Join/Leave embeds
    // ===================================================================

    public void sendJoinLog(String playerName, String discordId, String ip, boolean joined) {
        if (plugin.getDiscordBot() == null)
            return;

        boolean is2fa = plugin.getConfig().getBoolean("security.2fa-login.enabled", false);
        String title = joined ? "🟢 A player has joined the server" : "🔴 A player has left the server";

        EmbedBuilder embed = new EmbedBuilder()
                .setDescription("### " + title)
                .setColor(java.awt.Color.decode("#2B2D31"))
                .addField("👤 Username", playerName, true)
                .addField("🔐 2FA", is2fa ? "✅ Enabled" : "❌ Disabled", true)
                .addField("💬 Discord", discordId != null ? "<@" + discordId + ">" : "Not Linked", true)
                .addField("🌐 IP Address", censorIp(ip), true)
                .addField("📅 Date", "<t:" + Instant.now().getEpochSecond() + ":F>", true)
                .setFooter("CordSync • Server Logs")
                .setTimestamp(Instant.now());

        plugin.getDiscordBot().sendLogEmbed(null, null, null, embed);
    }

    // ===================================================================
    // HELPERS
    // ===================================================================

    private String getIpAddress(InetAddress address) {
        return address != null ? address.getHostAddress() : "0.0.0.0";
    }

    private String censorIp(String ip) {
        if (ip == null || ip.isEmpty())
            return "Unknown";
        if (!plugin.getConfig().getBoolean("security.2fa-login.censor-ip", true))
            return ip;
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".*.*";
        }
        return ip;
    }

    // ===================================================================
    // DATA CLASS
    // ===================================================================

    private static class PendingLogin {
        final String playerName;
        final String discordId;
        final String ipAddress;

        PendingLogin(String playerName, String discordId, String ipAddress) {
            this.playerName = playerName;
            this.discordId = discordId;
            this.ipAddress = ipAddress;
        }
    }
}
