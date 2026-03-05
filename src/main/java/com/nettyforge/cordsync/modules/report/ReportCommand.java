package com.nettyforge.cordsync.modules.report;

import com.nettyforge.cordsync.CordSync;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReportCommand implements CommandExecutor {

    private final CordSync plugin;
    private final ReportModule module;

    // Cooldown tracker: UUID -> last usage timestamp (millis)
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public ReportCommand(CordSync plugin, ReportModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player reporter)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }

        if (args.length < 2) {
            reporter.sendMessage(module.getConfig().getString("messages.usage", "§cUsage: /report <player> <reason>"));
            return true;
        }

        // Cooldown check
        int cooldownSeconds = module.getConfig().getInt("report-cooldown-seconds", 60);
        long now = System.currentTimeMillis();
        Long lastUsed = cooldowns.get(reporter.getUniqueId());
        if (lastUsed != null) {
            long elapsed = (now - lastUsed) / 1000;
            if (elapsed < cooldownSeconds) {
                long remaining = cooldownSeconds - elapsed;
                reporter.sendMessage(module.getConfig().getString("messages.report-cooldown",
                        "§cPlease wait {seconds}s before sending another report.")
                        .replace("{seconds}", String.valueOf(remaining)));
                return true;
            }
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            reporter.sendMessage(module.getConfig().getString("messages.offline", "§cThat player is not online."));
            return true;
        }

        cooldowns.put(reporter.getUniqueId(), now);

        StringBuilder reason = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            reason.append(args[i]).append(" ");
        }

        sendDiscordReport(reporter, target, reason.toString().trim());

        reporter.sendMessage(
                module.getConfig().getString("messages.success", "§aYour report has been sent to the admins."));
        return true;
    }

    private void sendDiscordReport(Player reporter, Player target, String reason) {
        if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
            return;

        String channelId = module.getConfig().getString("report-channel-id", "");
        if (channelId.isEmpty() || channelId.equals("BURAYA_KANAL_ID_GELECEK")
                || channelId.equals("YOUR_DISCORD_CHANNEL_ID_HERE")) {
            plugin.getLogger().warning("Please set the report-channel-id in config!");
            return;
        }

        // Capture target location on the main thread
        String targetName = target.getName() != null ? target.getName() : "Unknown";
        String worldName = target.getWorld().getName();
        int x = target.getLocation().getBlockX();
        int y = target.getLocation().getBlockY();
        int z = target.getLocation().getBlockZ();

        TextChannel channel = plugin.getDiscordBot().getJda().getTextChannelById(channelId);
        if (channel == null)
            return;

        EmbedBuilder embed = new EmbedBuilder()
                .setDescription("### 🚨 New Player Report\n\n**" + targetName + "** was reported by **"
                        + reporter.getName() + "**.")
                .addField("📝 Reason", (reason.length() > 0 ? reason : "No reason provided"), false)
                .addField("🌍 Location", worldName + " (" + x + ", " + y + ", " + z + ")", true)
                .addField("🆔 Reporter UUID", "" + reporter.getUniqueId().toString(), true)
                .setColor(java.awt.Color.decode("#2B2D31"))
                .setThumbnail("https://mc-heads.net/avatar/" + (targetName != null ? targetName : "Steve") + "/100.png")
                .setFooter("CordSync Report Module")
                .setTimestamp(Instant.now());

        // Buttons: Moderation + TELEPORT to target
        channel.sendMessageEmbeds(embed.build())
                .addActionRow(
                        Button.primary("mod_mute_" + targetName, "🔇 Mute"),
                        Button.secondary("mod_kick_" + targetName, "🚷 Kick"),
                        Button.danger("mod_ban_" + targetName, "⛔ Ban"),
                        Button.success("report_tp_" + targetName, "📍 Teleport"))
                .queue();
    }
}
