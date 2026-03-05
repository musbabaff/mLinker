package com.nettyforge.cordsync.modules.security;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.modules.CordModule;
import com.nettyforge.cordsync.utils.SchedulerUtil;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import javax.annotation.Nonnull;
import java.util.UUID;

public class SecurityModule extends CordModule implements Listener {

    private final JDAListener jdaListener;

    public SecurityModule(CordSync plugin) {
        super(plugin, "Security Module");
        this.jdaListener = new JDAListener();
    }

    @Override
    protected void setupDefaultConfig() {
        getConfig().set("forced-linking.enabled", true);
        getConfig().set("forced-linking.message",
                "§cYou must link your Discord account to play on this server!\n§eJoin our Discord: discord.gg/link");

        getConfig().set("alt-account-protection.enabled", true);
        getConfig().set("alt-account-protection.min-discord-account-age-days", 7);
        getConfig().set("alt-account-protection.message",
                "§cYour Discord account is too new to be linked. Please try again later.");

        getConfig().set("kick-on-discord-leave.enabled", true);
        getConfig().set("kick-on-discord-leave.message",
                "§cYour Discord link was broken because you left our Discord server.");

        saveConfig();
        plugin.getLogger().info("⚙ Created default config for SecurityModule!");
    }

    @Override
    public void onEnable() {
        getConfig(); // Setup or load config

        // Register Bukkit events (Forced Linking, Kick syncs)
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Register JDA events (Leave sync)
        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null) {
            plugin.getDiscordBot().getJda().addEventListener(jdaListener);
        }

        plugin.getLogger().info("🛡️ Security Module hooked!");
    }

    @Override
    public void onDisable() {
        AsyncPlayerPreLoginEvent.getHandlerList().unregister((Listener) this);

        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null) {
            plugin.getDiscordBot().getJda().removeEventListener(jdaListener);
        }

        plugin.getLogger().info("🛡️ Security Module unhooked.");
    }

    // ==========================================
    // Feature 1: Forced Linking (Discord Whitelist)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!getConfig().getBoolean("forced-linking.enabled", false))
            return;

        UUID playerUUID = event.getUniqueId();

        // If they are not linked in the storage, deny login
        try {
            if (plugin.getStorageProvider().getDiscordId(playerUUID) == null) {
                String kickMsg = getConfig().getString("forced-linking.message",
                        "§cYou must link your Discord account to play!");
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, kickMsg.replace("&", "§"));
            }
        } catch (Exception e) {
            plugin.getLogger().warning(
                    "Failed to check link status for " + event.getName() + " during pre-login: " + e.getMessage());
        }
    }

    // ==========================================
    // Feature 3: Discord Leave Sync (Kick on Leave)
    // ==========================================
    private class JDAListener extends ListenerAdapter {
        @Override
        public void onGuildMemberRemove(@Nonnull GuildMemberRemoveEvent event) {
            if (!getConfig().getBoolean("kick-on-discord-leave.enabled", false))
                return;

            String discordId = event.getUser().getId();
            UUID playerUUID = plugin.getStorageProvider().getPlayerUUID(discordId);

            if (playerUUID != null) {
                // They were linked, so we unlink them
                plugin.getLogger().info("🛡️ Player left Discord, unlinking account for discord ID: " + discordId);

                // Using Scheduler to safely access Bukkit API and perform Storage DB call if
                // needed
                SchedulerUtil.runTask(plugin, () -> {
                    plugin.getStorageProvider().removeLinkedAccount(playerUUID);

                    // If player is online, kick them
                    Player onlinePlayer = Bukkit.getPlayer(playerUUID);
                    if (onlinePlayer != null && onlinePlayer.isOnline()) {
                        String kickMsg = getConfig().getString("kick-on-discord-leave.message",
                                "§cYour Discord link was broken because you left our Discord server.");
                        onlinePlayer.kickPlayer(kickMsg.replace("&", "§"));
                    }
                });
            }
        }
    }
}
