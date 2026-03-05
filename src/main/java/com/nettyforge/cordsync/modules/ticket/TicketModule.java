package com.nettyforge.cordsync.modules.ticket;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.modules.CordModule;
import com.nettyforge.cordsync.utils.SchedulerUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TicketModule extends CordModule {

    private boolean isRunning = false;
    private TicketDiscordListener jdaListener;
    private TicketCommand ticketCommand;

    // Maps to keep track of Active Tickets
    private final Map<UUID, String> activePlayerTickets = new HashMap<>();
    private final Map<String, UUID> activeChannelTickets = new HashMap<>();

    public TicketModule(CordSync plugin) {
        super(plugin, "Ticket Module");
    }

    @Override
    protected void setupDefaultConfig() {
        getConfig().set("ticket-category-id", "000000000000000000");
        getConfig().set("messages.ticket-created",
                "&a[SyncTicket] Your ticket has been created! Please wait for staff.");
        getConfig().set("messages.ticket-already-open",
                "&c[SyncTicket] You already have an open ticket! Use /ticket close first.");
        getConfig().set("messages.ticket-closed", "&e[SyncTicket] Your ticket has been closed by Staff.");
        getConfig().set("messages.discord-reply", "&8[&bSupport&8] &7[&eStaff&7] &c{staff}&8: &f{message}");
        saveConfig();
        plugin.getLogger().info("🎫 Created default config for TicketModule!");
    }

    @Override
    public void onEnable() {
        isRunning = true;

        // Register Discord Listener
        jdaListener = new TicketDiscordListener();
        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null) {
            plugin.getDiscordBot().getJda().addEventListener(jdaListener);
        }

        // Register Command
        ticketCommand = new TicketCommand(plugin, this);
        if (plugin.getCommand("ticket") != null) {
            plugin.getCommand("ticket").setExecutor(ticketCommand);
        }

        plugin.getLogger().info("🎫 Ticket Module Online! 2-Way system active.");
    }

    @Override
    public void onDisable() {
        isRunning = false;

        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null && jdaListener != null) {
            plugin.getDiscordBot().getJda().removeEventListener(jdaListener);
        }

        plugin.getLogger().info("🎫 Ticket Module Offline.");
    }

    public void createTicket(Player player, String message) {
        if (!isRunning)
            return;

        if (activePlayerTickets.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    getConfig().getString("messages.ticket-already-open", "&cYou already have an open ticket!")));
            return;
        }

        String categoryId = getConfig().getString("ticket-category-id", "");
        if (categoryId.isEmpty() || categoryId.equals("000000000000000000")) {
            player.sendMessage(ChatColor.RED + "Ticket system is not fully configured (Missing Discord Category).");
            return;
        }

        double tps = com.nettyforge.cordsync.utils.TPSMonitor.getTPS();
        int ping = player.getPing();

        SchedulerUtil.runAsync(plugin, () -> {
            if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
                return;

            Category category = plugin.getDiscordBot().getJda().getCategoryById(categoryId);
            if (category == null)
                return;

            Guild guild = category.getGuild();
            guild.createTextChannel("ticket-" + player.getName().toLowerCase(), category).queue(channel -> {
                // Map the Ticket securely
                activePlayerTickets.put(player.getUniqueId(), channel.getId());
                activeChannelTickets.put(channel.getId(), player.getUniqueId());

                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("🎫 New Ticket: " + player.getName());
                eb.setDescription("**Message:** " + message + "");
                eb.addField("Player", java.util.Objects.requireNonNull(player.getName()), true);
                eb.addField("Ping", ping + "ms", true);
                eb.addField("Server TPS", java.util.Objects.requireNonNull(String.format("%.2f", tps)), true);
                eb.setColor(Color.ORANGE);

                channel.sendMessageEmbeds(eb.build())
                        .addActionRow(
                                Button.danger("close_ticket_" + player.getUniqueId().toString(), "🔒 Close Ticket"))
                        .queue();

                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        getConfig().getString("messages.ticket-created", "&aTicket created!") + ""));
            });
        });
    }

    public void closeTicket(Player player) {
        if (!isRunning)
            return;

        if (!activePlayerTickets.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You do not have an open ticket.");
            return;
        }

        String channelId = activePlayerTickets.get(player.getUniqueId());

        SchedulerUtil.runAsync(plugin, () -> {
            if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null && channelId != null) {
                TextChannel channel = plugin.getDiscordBot().getJda()
                        .getTextChannelById(java.util.Objects.requireNonNull(channelId));
                if (channel != null) {
                    channel.delete().queue();
                }
            }
        });

        activePlayerTickets.remove(player.getUniqueId());
        activeChannelTickets.remove(channelId);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.ticket-closed", "&eYour ticket was closed.") + ""));
    }

    private void closeTicketFromDiscord(String channelId) {
        if (!activeChannelTickets.containsKey(channelId))
            return;

        UUID playerUUID = activeChannelTickets.get(channelId);
        activeChannelTickets.remove(channelId);
        activePlayerTickets.remove(playerUUID);

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    getConfig().getString("messages.ticket-closed", "&eYour ticket was closed by Staff.") + ""));
        }

        SchedulerUtil.runAsync(plugin, () -> {
            if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null && channelId != null) {
                TextChannel channel = plugin.getDiscordBot().getJda()
                        .getTextChannelById(java.util.Objects.requireNonNull(channelId));
                if (channel != null) {
                    channel.delete().queue();
                }
            }
        });
    }

    // 2-Way Discord Relayer
    private class TicketDiscordListener extends ListenerAdapter {
        @Override
        public void onMessageReceived(@javax.annotation.Nonnull MessageReceivedEvent event) {
            if (!isRunning)
                return;
            if (event.getAuthor().isBot())
                return; // Anti-Loop

            String channelId = event.getChannel().getId();

            // If the message is typed in an active Ticket Channel
            if (activeChannelTickets.containsKey(channelId)) {
                UUID playerUUID = activeChannelTickets.get(channelId);
                Player player = Bukkit.getPlayer(playerUUID);

                if (player != null && player.isOnline()) {
                    String message = event.getMessage().getContentDisplay();
                    String format = getConfig()
                            .getString("messages.discord-reply",
                                    "&8[&bSupport&8] &7[&eStaff&7] &c{staff}&8: &f{message}")
                            .replace("{staff}", event.getAuthor().getName())
                            .replace("{message}", message);

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', format));
                }
            }
        }

        @Override
        public void onButtonInteraction(@javax.annotation.Nonnull ButtonInteractionEvent event) {
            if (!isRunning)
                return;

            String componentId = event.getComponentId();
            if (componentId.startsWith("close_ticket_")) {
                event.deferEdit().queue(); // Acknowledge button
                String channelId = event.getChannel().getId();
                closeTicketFromDiscord(channelId);
            }
        }
    }
}
