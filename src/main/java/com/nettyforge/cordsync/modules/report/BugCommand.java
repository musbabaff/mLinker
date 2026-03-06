package com.nettyforge.cordsync.modules.report;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.utils.SchedulerUtil;
import com.nettyforge.cordsync.utils.TPSMonitor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
public class BugCommand implements CommandExecutor {

    private final CordSync plugin;
    private final ReportModule module;

    // Cooldown tracker: UUID -> last usage timestamp (millis)
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public BugCommand(CordSync plugin, ReportModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(module.getConfig().getString("messages.bug-usage",
                    "§cUsage: /bug <description>"));
            return true;
        }

        // Cooldown check
        int cooldownSeconds = module.getConfig().getInt("bug-cooldown-seconds", 300);
        long now = System.currentTimeMillis();
        Long lastUsed = cooldowns.get(player.getUniqueId());
        if (lastUsed != null) {
            long elapsed = (now - lastUsed) / 1000;
            if (elapsed < cooldownSeconds) {
                long remaining = cooldownSeconds - elapsed;
                player.sendMessage(module.getConfig().getString("messages.bug-cooldown",
                        "§cPlease wait {seconds}s before submitting another bug report.")
                        .replace("{seconds}", String.valueOf(remaining)));
                return true;
            }
        }

        cooldowns.put(player.getUniqueId(), now);

        // Build description from args
        StringBuilder description = new StringBuilder();
        for (String arg : args) {
            description.append(arg).append(" ");
        }

        sendBugReport(player, description.toString().trim());

        player.sendMessage(module.getConfig().getString("messages.bug-success",
                "§a🐛 Your bug report has been submitted. Thank you!"));
        return true;
    }

    private void sendBugReport(Player player, String desc) {
        if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
            return;

        String channelId = module.getConfig().getString("bug-reports-channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_BUG_CHANNEL_ID"))
            return;

        // Capture data on main thread before going async
        String playerName = player.getName();
        String worldName = player.getWorld().getName();
        int x = player.getLocation().getBlockX();
        int y = player.getLocation().getBlockY();
        int z = player.getLocation().getBlockZ();
        double tps = TPSMonitor.getTPS();
        long maxMem = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        String ramStr = usedMem + "MB / " + maxMem + "MB";

        // Get the linked Discord ID (if linked)
        String discordId = plugin.getStorageProvider().getDiscordId(player.getUniqueId());
        String discordField = discordId != null ? "<@" + discordId + ">" : "Not Linked";

        SchedulerUtil.runAsync(plugin, () -> {
            TextChannel channel = plugin.getDiscordBot().getJda()
                    .getTextChannelById(Objects.requireNonNull(channelId));
            if (channel == null)
                return;

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("🐛 New Bug Report!");
            eb.setDescription(desc);
            eb.addField("👤 Reporter", playerName != null ? playerName : "Unknown", true);
            eb.addField("💬 Discord", discordField != null ? discordField : "Unknown", true);
            eb.addField("🌍 Location", worldName + " (" + x + ", " + y + ", " + z + ")", true);
            eb.addField("📊 Server TPS", "" + String.format("%.1f", tps), true);
            eb.addField("🧠 RAM Usage", ramStr, true);
            eb.setColor(Color.ORANGE);
            eb.setThumbnail("https://mc-heads.net/avatar/" + playerName + "/100.png");
            eb.setFooter("CordSync Bug Reports • " + playerName);
            eb.setTimestamp(Instant.now());

            channel.sendMessageEmbeds(eb.build()).queue();
        });
    }
}
