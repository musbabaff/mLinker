package com.nettyforge.cordsync.modules.report;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.modules.CordModule;
import com.nettyforge.cordsync.utils.SchedulerUtil;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

public class ReportModule extends CordModule {

    private ReportCommand reportCommand;
    private BugCommand bugCommand;
    private ReportTeleportListener jdaListener;

    public ReportModule(CordSync plugin) {
        super(plugin, "Report Module");
    }

    @Override
    protected void setupDefaultConfig() {
        getConfig().set("report-channel-id", "BURAYA_KANAL_ID_GELECEK");
        getConfig().set("bug-reports-channel-id", "YOUR_BUG_CHANNEL_ID");
        getConfig().set("report-cooldown-seconds", 60);
        getConfig().set("bug-cooldown-seconds", 300);
        getConfig().set("messages.usage", "§cUsage: /report <player> <reason>");
        getConfig().set("messages.success", "§aYour report has been sent to the admins.");
        getConfig().set("messages.offline", "§cThat player is not online.");
        getConfig().set("messages.bug-usage", "§cUsage: /bug <description>");
        getConfig().set("messages.bug-success", "§a🐛 Your bug report has been submitted. Thank you!");
        getConfig().set("messages.bug-cooldown", "§cPlease wait {seconds}s before submitting another bug report.");
        getConfig().set("messages.report-cooldown", "§cPlease wait {seconds}s before sending another report.");
        saveConfig();
        plugin.getLogger().info("⚙ Created default config for ReportModule!");
    }

    @Override
    public void onEnable() {
        getConfig();

        reportCommand = new ReportCommand(plugin, this);
        bugCommand = new BugCommand(plugin, this);

        Objects.requireNonNull(plugin.getCommand("report")).setExecutor(reportCommand);
        Objects.requireNonNull(plugin.getCommand("bug")).setExecutor(bugCommand);

        // Register JDA Listener for TELEPORT button
        jdaListener = new ReportTeleportListener();
        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null) {
            plugin.getDiscordBot().getJda().addEventListener(jdaListener);
        }

        plugin.getLogger().info("🚨 Report System hooked! /report and /bug are now active.");
    }

    @Override
    public void onDisable() {
        if (plugin.getCommand("report") != null) {
            Objects.requireNonNull(plugin.getCommand("report")).setExecutor(null);
        }
        if (plugin.getCommand("bug") != null) {
            Objects.requireNonNull(plugin.getCommand("bug")).setExecutor(null);
        }
        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null && jdaListener != null) {
            plugin.getDiscordBot().getJda().removeEventListener(jdaListener);
        }

        plugin.getLogger().info("🚨 Report System unhooked.");
    }

    // ═══════════════════════════════════════════════════════════
    // JDA Listener: [TELEPORT] Button → tp staff to reported player
    // ═══════════════════════════════════════════════════════════
    private class ReportTeleportListener extends ListenerAdapter {
        @Override
        public void onButtonInteraction(@javax.annotation.Nonnull ButtonInteractionEvent event) {
            String componentId = event.getComponentId();
            if (!componentId.startsWith("report_tp_"))
                return;

            String targetName = componentId.substring("report_tp_".length());

            // Find whoever clicked → get their linked MC account
            String discordUserId = event.getUser().getId();
            java.util.UUID staffUuid = plugin.getStorageProvider().getPlayerUUID(discordUserId);

            if (staffUuid == null) {
                event.reply("❌ Your Discord account is not linked to any Minecraft account. Please link first!")
                        .setEphemeral(true).queue();
                return;
            }

            // Execute teleport on Bukkit main thread
            SchedulerUtil.runSync(plugin, () -> {
                Player staff = Bukkit.getPlayer(staffUuid);
                Player target = Bukkit.getPlayer(targetName);

                if (staff == null) {
                    // Staff is offline — reply from async context
                    event.reply("❌ You must be online on the Minecraft server to teleport!")
                            .setEphemeral(true).queue();
                    return;
                }

                if (target == null || !target.isOnline()) {
                    event.reply("⚠️ Target player **" + targetName + "** is no longer online.")
                            .setEphemeral(true).queue();
                    return;
                }

                staff.teleport(target.getLocation());
                event.reply("✅ Teleported **" + staff.getName() + "** → **" + targetName + "**!")
                        .setEphemeral(true).queue();
            });
        }
    }
}
