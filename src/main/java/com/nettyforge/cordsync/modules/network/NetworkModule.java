package com.nettyforge.cordsync.modules.network;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.modules.CordModule;
import com.nettyforge.cordsync.utils.SchedulerUtil;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class NetworkModule extends CordModule {

    private boolean isRunning = false;
    private NetworkDiscordListener jdaListener;
    private StaffChatCommand staffChatCommand;

    public NetworkModule(CordSync plugin) {
        super(plugin, "Network Module");
    }

    @Override
    protected void setupDefaultConfig() {
        getConfig().set("server-name", "Skyblock-1");
        getConfig().set("discord-staff-channel-id", "000000000000000000");
        getConfig().set("format.discord-to-game", "&8[&cStaffChat&8] &7[&eDiscord&7] &f{author}&8: &a{message}");
        getConfig().set("format.game-to-discord", "**[{server}]** `{player}`: {message}");
        getConfig().set("format.game-console-view", "&8[&cStaffChat&8] &7[&b{server}&7] &f{player}&8: &a{message}");
        saveConfig();
        plugin.getLogger().info("⚙ Created default config for NetworkModule!");
    }

    @Override
    public void onEnable() {
        isRunning = true;

        // Register Discord Listener
        jdaListener = new NetworkDiscordListener();
        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null) {
            plugin.getDiscordBot().getJda().addEventListener(jdaListener);
        }

        // Register Command
        staffChatCommand = new StaffChatCommand(plugin, this);
        if (plugin.getCommand("staffchat") != null) {
            plugin.getCommand("staffchat").setExecutor(staffChatCommand);
        }

        plugin.getLogger().info("🌐 Network Module Online! Cross-Server Sync is active.");
    }

    @Override
    public void onDisable() {
        isRunning = false;

        // Unregister Discord Listener
        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null && jdaListener != null) {
            plugin.getDiscordBot().getJda().removeEventListener(jdaListener);
        }

        plugin.getLogger().info("🌐 Network Module Offline.");
    }

    // Called by the Bukkit Command when a staff member types /sc
    public void sendStaffChatToDiscord(Player sender, String message) {
        if (!isRunning)
            return;

        String channelId = getConfig().getString("discord-staff-channel-id", "");
        if (channelId.isEmpty() || channelId.equals("000000000000000000"))
            return;

        String serverName = getConfig().getString("server-name", "Unknown");
        String finalMsg = getConfig().getString("format.game-to-discord", "**[{server}]** `{player}`: {message}")
                .replace("{server}", serverName)
                .replace("{player}", sender.getName())
                .replace("{message}", message);

        String consoleView = getConfig()
                .getString("format.game-console-view", "&8[&cStaffChat&8] &7[&b{server}&7] &f{player}&8: &a{message}")
                .replace("{server}", serverName)
                .replace("{player}", sender.getName())
                .replace("{message}", message);

        // Broadcast to this local server's online staff first (preventing self-echo
        // issues)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("cordsync.staffchat")) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', consoleView));
            }
        }
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', consoleView));

        // Send Async to Discord
        com.nettyforge.cordsync.utils.SchedulerUtil.runAsync(plugin, () -> {
            if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
                return;
            TextChannel channel = plugin.getDiscordBot().getJda().getTextChannelById(channelId);
            if (channel != null) {
                // By sending it, OTHER servers reading the channel will pick it up via
                // MessageReceivedEvent
                channel.sendMessage(finalMsg + "").queue();
            }
        });
    }

    // Listens for cross-server messages from Discord
    private class NetworkDiscordListener extends ListenerAdapter {
        @Override
        public void onMessageReceived(@javax.annotation.Nonnull MessageReceivedEvent event) {
            if (!isRunning)
                return;

            // SECURITY: Anti-Loop! NEVER read messages from ANY Bot (including ourselves)
            if (event.getAuthor().isBot())
                return;

            String channelId = getConfig().getString("discord-staff-channel-id", "");
            if (!event.getChannel().getId().equals(channelId))
                return;

            String message = event.getMessage().getContentDisplay();
            net.dv8tion.jda.api.entities.Member member = event.getMember();
            String authorName = member != null ? member.getEffectiveName()
                    : event.getAuthor().getName();

            String format = getConfig()
                    .getString("format.discord-to-game", "&8[&cStaffChat&8] &7[&eDiscord&7] &f{author}&8: &a{message}")
                    .replace("{author}", authorName)
                    .replace("{message}", message);

            String colored = ChatColor.translateAlternateColorCodes('&', format);

            // Dispatch to local online staff concurrently
            SchedulerUtil.runSync(plugin, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("cordsync.staffchat")) {
                        p.sendMessage(colored);
                    }
                }
                Bukkit.getConsoleSender().sendMessage(colored);
            });
        }
    }
}
