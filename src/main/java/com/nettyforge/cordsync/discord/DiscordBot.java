package com.nettyforge.cordsync.discord;

import java.awt.Color;
import java.util.List;

import java.util.concurrent.atomic.AtomicInteger;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.commands.LinkVerifyCommand;
import com.nettyforge.cordsync.listeners.ConsoleBridgeListener;
import com.nettyforge.cordsync.listeners.ReverseSyncListener;
import com.nettyforge.cordsync.utils.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import javax.annotation.Nonnull;
import java.util.EnumSet;

public class DiscordBot extends ListenerAdapter {

    private final CordSync plugin;
    private JDA jda;
    private BukkitTask statusTask;
    private ConsoleBridgeListener consoleBridgeListener;

    @SuppressWarnings("null")
    public DiscordBot(CordSync plugin, String token, String status) {
        this.plugin = plugin;
        try {
            EnumSet<GatewayIntent> intents = EnumSet.of(
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS,
                    GatewayIntent.DIRECT_MESSAGES);

            consoleBridgeListener = new ConsoleBridgeListener(plugin);

            jda = JDABuilder.createDefault(token != null ? token : "")
                    .enableIntents(java.util.Collections.unmodifiableCollection(intents))
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setActivity(Activity.playing(status != null ? status : "CordSync"))
                    .addEventListeners(new LinkVerifyCommand(plugin))
                    .addEventListeners(consoleBridgeListener)
                    .addEventListeners(new ReverseSyncListener(plugin))
                    .addEventListeners(this) // for chat bridge
                    .build();

            // Register the SAME 2FA listener instance created by CordSync (single-instance
            // fix)
            if (plugin.getLoginVerifyListener() != null) {
                jda.addEventListener(plugin.getLoginVerifyListener());
            }

            jda.awaitReady();

            // Register slash command (configurable name)
            String slashName = plugin.getConfig().getString("commands.discord-slash-command", "link");
            String slashDesc = plugin.getConfig().getString("commands.discord-slash-description",
                    "Link your Minecraft account with Discord.");
            jda.updateCommands().addCommands(
                    net.dv8tion.jda.api.interactions.commands.build.Commands.slash(slashName, slashDesc)
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
                                    "code", "The linking code from Minecraft", false))
                    .queue(
                            success -> plugin.getLogger().info("\u2705 /" + slashName + " slash command registered!"),
                            failure -> plugin.getLogger()
                                    .severe("\u274C Slash command registration failed: " + failure.getMessage()));

            plugin.getLogger().info(MessageUtil.get("discord.started"));

            if (plugin.getConfig().getBoolean("console-bridge.enabled", false)) {
                consoleBridgeListener.startCapture();
                plugin.getLogger().info("🖥️ Console Bridge active!");
            }

            // Auto messages
            sendAutoMessage();
            sendBoosterInfoMessage();

            // Rotating status
            startStatusUpdater();

        } catch (InterruptedException e) {
            plugin.getLogger().severe("Discord bot interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            plugin.getLogger().severe("Discord bot failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (consoleBridgeListener != null) {
            consoleBridgeListener.stop();
        }
        if (statusTask != null) {
            statusTask.cancel();
            statusTask = null;
        }
        if (jda != null) {
            jda.shutdownNow();
            plugin.getLogger().info(MessageUtil.get("discord.bot-stopped"));
        }
    }

    public JDA getJda() {
        return jda;
    }

    // ===================================================================
    // CHAT BRIDGE: Discord → Minecraft
    // ===================================================================

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (!plugin.getConfig().getBoolean("chat-bridge.enabled", false))
            return;
        if (event.getAuthor().isBot())
            return;

        String bridgeChannelId = plugin.getConfig().getString("chat-bridge.channel-id", "");
        if (bridgeChannelId == null || bridgeChannelId.isEmpty())
            return;
        if (!event.getChannel().getId().equals(bridgeChannelId))
            return;

        String rawMessage = event.getMessage().getContentDisplay();
        if (rawMessage.isEmpty())
            return;

        // Check ignored prefixes
        List<String> ignoredPrefixes = plugin.getConfig().getStringList("chat-bridge.ignored-prefixes");
        for (String prefix : ignoredPrefixes) {
            if (rawMessage.startsWith(prefix))
                return;
        }

        // Check Discord → MC permission setting
        String d2mc = plugin.getConfig().getString("chat-bridge.discord-to-mc", "ALL");
        if ("LINKED_ONLY".equalsIgnoreCase(d2mc)) {
            if (!plugin.getStorageProvider().isDiscordLinked(event.getAuthor().getId()))
                return;
        }

        // Format and send to MC
        String format = plugin.getConfig().getString("chat-bridge.minecraft-format",
                "&7[&bDiscord&7] &f{player}&7: &f{message}");
        net.dv8tion.jda.api.entities.Member member = event.getMember();
        String playerName = member != null ? member.getEffectiveName()
                : event.getAuthor().getName();
        String formatted = ChatColor.translateAlternateColorCodes('&',
                format.replace("{player}", playerName).replace("{message}", rawMessage));

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(formatted);
            }
            Bukkit.getConsoleSender().sendMessage(formatted);
        });
    }

    /**
     * Called by ChatBridgeListener to send MC messages to Discord.
     */
    public void sendChatBridgeMessage(String message) {
        if (jda == null || message == null)
            return;
        String channelId = plugin.getConfig().getString("chat-bridge.channel-id", "");
        if (channelId == null || channelId.isEmpty())
            return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null)
            return;

        channel.sendMessage(message).queue();
    }

    // ===================================================================
    // ROTATING STATUS
    // ===================================================================

    private void startStatusUpdater() {
        if (jda == null)
            return;

        boolean rotate = plugin.getConfig().getBoolean("discord.status.rotate", true);
        int intervalSec = plugin.getConfig().getInt("discord.status.interval", 15);
        List<String> messages = plugin.getConfig().getStringList("discord.status.messages");

        if (messages.isEmpty()) {
            messages = java.util.List.of("PLAYING:{online} Players Online | CordSync");
        }

        final List<String> finalMessages = messages;
        final AtomicInteger index = new AtomicInteger(0);

        statusTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (jda == null || jda.getStatus() != JDA.Status.CONNECTED)
                return;

            int i = rotate ? index.getAndUpdate(v -> (v + 1) % finalMessages.size()) : 0;
            String entry = finalMessages.get(i);

            String type = "PLAYING";
            String text = entry;
            if (entry.contains(":")) {
                String[] parts = entry.split(":", 2);
                type = parts[0].toUpperCase();
                text = parts[1];
            }

            int online = Bukkit.getOnlinePlayers().size();
            int max = Bukkit.getMaxPlayers();
            int linked = plugin.getStorageProvider().getAllLinkedPlayers().size();
            text = text.replace("{online}", String.valueOf(online))
                    .replace("{max}", String.valueOf(max))
                    .replace("{linked}", String.valueOf(linked));

            if (text == null)
                text = "";

            Activity activity;
            switch (type) {
                case "WATCHING":
                    activity = Activity.watching(text);
                    break;
                case "LISTENING":
                    activity = Activity.listening(text);
                    break;
                default:
                    activity = Activity.playing(text);
                    break;
            }
            jda.getPresence().setActivity(activity);

        }, 20L * 10, 20L * intervalSec);

        plugin.getLogger()
                .info("\uD83C\uDFAE Rotating status started (" + messages.size() + " messages, " + intervalSec + "s).");
    }

    // ===================================================================
    // HELPER METHODS
    // ===================================================================

    public boolean isMemberInGuild(String discordId) {
        if (jda == null)
            return false;
        try {
            String guildId = plugin.getConfig().getString("discord.guild-id", "");
            Guild guild = jda.getGuildById(guildId != null ? guildId : "");
            if (guild == null)
                return false;
            return guild.retrieveMemberById(discordId != null ? discordId : "").complete() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasVerifiedRole(String discordId) {
        if (jda == null)
            return false;
        try {
            String guildId = plugin.getConfig().getString("discord.guild-id", "");
            Guild guild = jda.getGuildById(guildId != null ? guildId : "");
            if (guild == null)
                return false;
            Member member = guild.retrieveMemberById(discordId != null ? discordId : "").complete();
            if (member == null)
                return false;
            String roleId = plugin.getConfig().getString("discord.role-id-verified");
            return member.getRoles().stream().anyMatch(r -> r.getId().equals(roleId));
        } catch (Exception e) {
            return false;
        }
    }

    // ===================================================================
    // LOG CHANNEL
    // ===================================================================

    public void sendLogEmbed(String title, String description, Color color) {
        if (jda == null)
            return;
        String logChannelId = plugin.getConfig().getString("discord.log-channel-id", "");
        if (logChannelId == null || logChannelId.isEmpty())
            return;

        try {
            TextChannel channel = jda.getTextChannelById(logChannelId);
            if (channel == null)
                return;

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title).setDescription(description).setColor(color)
                    .setFooter("CordSync Log").setTimestamp(java.time.Instant.now());

            channel.sendMessageEmbeds(embed.build()).queue();
        } catch (Exception e) {
            plugin.getLogger().warning("Log embed failed: " + e.getMessage());
        }
    }

    /**
     * Overload: send a pre-built embed to log channel.
     */
    public void sendLogEmbed(String ignoreTitle, String ignoreDesc, Color ignoreColor, EmbedBuilder embed) {
        if (jda == null)
            return;
        String logChannelId = plugin.getConfig().getString("discord.log-channel-id", "");
        if (logChannelId == null || logChannelId.isEmpty())
            return;

        try {
            TextChannel channel = jda.getTextChannelById(logChannelId);
            if (channel == null)
                return;
            channel.sendMessageEmbeds(embed.build()).queue();
        } catch (Exception e) {
            plugin.getLogger().warning("Log embed failed: " + e.getMessage());
        }
    }

    // ===================================================================
    // AUTO MESSAGE - Premium embed with buttons
    // ===================================================================

    private void sendAutoMessage() {
        if (jda == null)
            return;
        if (!plugin.getConfig().getBoolean("discord.auto-message.enabled", false))
            return;

        String channelId = plugin.getConfig().getString("discord.auto-message.channel-id", "");
        if (channelId == null || channelId.isEmpty())
            return;

        plugin.debug("Discord: Attempting to process Auto-Message in channel " + channelId);
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null)
                return;

            // Check if bot message already exists
            List<Message> messages = channel.getHistory().retrievePast(50).complete();
            for (Message msg : messages) {
                if (msg.getAuthor().getId().equals(jda.getSelfUser().getId())) {
                    plugin.getLogger().info(MessageUtil.get("discord.auto-message-exists"));
                    return;
                }
            }

            String title = plugin.getConfig().getString("discord.auto-message.embed-title",
                    "\uD83D\uDD17 Account Linking");
            String description = plugin.getConfig().getString("discord.auto-message.embed-description",
                    "Link your Minecraft account with Discord!");
            String image = plugin.getConfig().getString("discord.auto-message.embed-image", "");
            String thumbnail = plugin.getConfig().getString("discord.auto-message.embed-thumbnail", "");

            Color embedColor = Color.decode("#2B2D31");

            String fieldTitle = MessageUtil.getRaw("discord-embeds.auto-message.field-title");
            String fieldDesc = MessageUtil.getRaw("discord-embeds.auto-message.field-desc");
            String footerText = MessageUtil.getRaw("discord-embeds.auto-message.footer");

            String markdownDesc = "### " + title + "\n\n" + description;

            EmbedBuilder embed = new EmbedBuilder()
                    .setDescription(markdownDesc)
                    .setColor(embedColor)
                    .addField(fieldTitle != null ? fieldTitle : "How It Works",
                            fieldDesc != null ? fieldDesc : "Click the button to link.",
                            false)
                    .setFooter(footerText != null ? footerText : "CordSync")
                    .setTimestamp(java.time.Instant.now());

            if (image != null && !image.isEmpty()) {
                try {
                    embed.setImage(image);
                } catch (Exception ignored) {
                }
            }
            if (thumbnail != null && !thumbnail.isEmpty()) {
                try {
                    embed.setThumbnail(thumbnail);
                } catch (Exception ignored) {
                }
            }

            String btnLink = MessageUtil.getRaw("discord-embeds.buttons.link");
            String btnHowTo = MessageUtil.getRaw("discord-embeds.buttons.how-to");
            String btnStatus = MessageUtil.getRaw("discord-embeds.buttons.status");

            // Premium button layout with modal trigger, info, and status
            channel.sendMessageEmbeds(embed.build())
                    .addActionRow(
                            Button.success("cordsync_link_modal", btnLink != null ? btnLink : "Link"),
                            Button.primary("cordsync_howto_info", btnHowTo != null ? btnHowTo : "How To"),
                            Button.secondary("cordsync_status", btnStatus != null ? btnStatus : "Status"),
                            Button.danger("cordsync_toggle_2fa", "Toggle 2FA"))
                    .queue(
                            success -> plugin.getLogger().info(MessageUtil.get("discord.auto-message-sent")),
                            failure -> plugin.getLogger().warning("Auto message failed: " + failure.getMessage()));

        } catch (Exception e) {
            plugin.getLogger().warning("Auto message error: " + e.getMessage());
        }
    }

    // ===================================================================
    // BOOSTER INFO MESSAGE
    // ===================================================================

    public void sendJoinQuitEmbed(EmbedBuilder embed) {
        if (jda == null)
            return;
        try {
            String channelId = plugin.getConfig().getString("discord.join-quit-messages.channel-id", "");
            if (channelId == null || channelId.isEmpty())
                return;
            net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessageEmbeds(embed.build()).queue();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("JoinQuit message failed: " + e.getMessage());
        }
    }

    private void sendBoosterInfoMessage() {
        if (jda == null)
            return;
        if (!plugin.getConfig().getBoolean("discord.booster-message.enabled", false))
            return;

        String channelId = plugin.getConfig().getString("discord.booster-message.channel-id", "");
        if (channelId == null || channelId.isEmpty())
            return;

        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null)
                return;

            List<Message> messages = channel.getHistory().retrievePast(50).complete();
            for (Message msg : messages) {
                if (msg.getAuthor().getId().equals(jda.getSelfUser().getId()))
                    return;
            }

            String title = plugin.getConfig().getString("discord.booster-message.embed-title",
                    "\uD83D\uDE80 Server Booster Rewards");
            String description = plugin.getConfig().getString("discord.booster-message.embed-description",
                    "Boost our server to earn exclusive in-game rewards!");
            String colorHex = plugin.getConfig().getString("discord.booster-message.embed-color", "#FF73FA");
            String thumbnail = plugin.getConfig().getString("discord.booster-message.embed-thumbnail", "");

            Color embedColor;
            try {
                embedColor = Color.decode(colorHex);
            } catch (Exception e) {
                embedColor = new Color(255, 115, 250);
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(description)
                    .setColor(embedColor)
                    .setFooter(MessageUtil.getRaw("discord-embeds.booster-message.footer"))
                    .setTimestamp(java.time.Instant.now());

            if (thumbnail != null && !thumbnail.isEmpty()) {
                try {
                    embed.setThumbnail(thumbnail);
                } catch (Exception ignored) {
                }
            }

            String btnViewRewards = MessageUtil.getRaw("discord-embeds.buttons.view-rewards");
            channel.sendMessageEmbeds(embed.build())
                    .addActionRow(Button.secondary("cordsync_booster_info",
                            btnViewRewards != null ? btnViewRewards : "🎁 View Rewards"))
                    .queue(
                            success -> plugin.getLogger().info("\u2705 Booster info message sent."),
                            failure -> plugin.getLogger().warning("Booster message failed: " + failure.getMessage()));

        } catch (Exception e) {
            plugin.getLogger().warning("Booster message error: " + e.getMessage());
        }
    }
}
