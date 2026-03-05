package com.nettyforge.cordsync.modules.economy;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.utils.SchedulerUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.List;

public class EconomySlashCommands extends ListenerAdapter {

    private final CordSync plugin;
    private final EconomyModule module;
    private final boolean hasVault;
    private final boolean hasPAPI;

    private Economy econ = null;

    public EconomySlashCommands(CordSync plugin, EconomyModule module, boolean hasVault, boolean hasPAPI) {
        this.plugin = plugin;
        this.module = module;
        this.hasVault = hasVault;
        this.hasPAPI = hasPAPI;

        if (hasVault) {
            setupEconomy();
        }
    }

    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null)
            return;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null)
            return;
        econ = rsp.getProvider();
    }

    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        if (event.getName().equals("stats")) {
            handleStatsCommand(event);
        } else if (event.getName().equals("bal")) {
            handleBalCommand(event);
        } else if (event.getName().equals("eco")) {
            handleEcoCommand(event);
        }
    }

    private void handleStatsCommand(SlashCommandInteractionEvent event) {
        if (!hasPAPI) {
            event.reply("PlaceholderAPI is not installed on the server.").setEphemeral(true).queue();
            return;
        }

        String targetName = java.util.Objects.requireNonNull(event.getOption("player")).getAsString();
        event.deferReply().queue(); // Because Bukkit operations can take time async

        SchedulerUtil.runTask(plugin, () -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                String error = module.getConfig().getString("messages.stats.player-not-found", "Player not found.");
                event.getHook().sendMessage(error.replace("&", "§") + "").queue();
                return;
            }

            List<String> lines = module.getConfig().getStringList("messages.stats.lines");
            StringBuilder desc = new StringBuilder();
            for (String line : lines) {
                // Apply PAPI
                String parsed = PlaceholderAPI.setPlaceholders(target, line);
                desc.append(parsed).append("\n");
            }

            String title = module.getConfig().getString("messages.stats.title", "Statistics: {player}")
                    .replace("{player}", target.getName() != null ? target.getName() : "Unknown");
            String color = module.getConfig().getString("messages.embed-color", "#2B2D31");

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(desc.toString())
                    .setColor(java.awt.Color.decode(color))
                    .setTimestamp(Instant.now());

            event.getHook().sendMessageEmbeds(embed.build()).queue();
        });
    }

    private void handleBalCommand(SlashCommandInteractionEvent event) {
        if (!hasVault || econ == null) {
            event.reply("Vault is not installed on the server.").setEphemeral(true).queue();
            return;
        }

        String targetName = java.util.Objects.requireNonNull(event.getOption("player")).getAsString();
        event.deferReply().queue();

        SchedulerUtil.runTask(plugin, () -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                String error = module.getConfig().getString("messages.bal.error", "Player not found.");
                event.getHook().sendMessage(error.replace("&", "§") + "").queue();
                return;
            }

            double balance = econ.getBalance(target);
            String title = module.getConfig().getString("messages.bal.title", "Balance: {player}").replace("{player}",
                    target.getName() != null ? target.getName() : "Unknown");
            String amtLine = module.getConfig().getString("messages.bal.amount", "**Balance:** ${amount}")
                    .replace("{amount}", String.valueOf(balance));
            String color = module.getConfig().getString("messages.embed-color", "#2B2D31");

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(amtLine)
                    .setColor(java.awt.Color.decode(color))
                    .setTimestamp(Instant.now());

            event.getHook().sendMessageEmbeds(embed.build()).queue();
        });
    }

    private void handleEcoCommand(SlashCommandInteractionEvent event) {
        if (!hasVault || econ == null) {
            event.reply("Vault is not installed on the server.").setEphemeral(true).queue();
            return;
        }

        // Check Permissions via admin-role-id
        String adminRoleId = module.getConfig().getString("economy.admin-role-id", "");
        net.dv8tion.jda.api.entities.Member member = event.getMember();
        if (member == null || member.getRoles().stream().noneMatch(r -> r.getId().equals(adminRoleId))) {
            String error = module.getConfig().getString("messages.eco.no-permission", "No permission.");
            event.reply(error.replace("&", "§") + "").setEphemeral(true).queue();
            return;
        }

        String subName = event.getSubcommandName();
        if (subName == null || !subName.equals("give")) {
            event.reply("Unknown subcommand.").setEphemeral(true).queue();
            return;
        }

        String targetName = java.util.Objects.requireNonNull(event.getOption("player")).getAsString();
        int amount = java.util.Objects.requireNonNull(event.getOption("amount")).getAsInt();
        event.deferReply().queue();

        SchedulerUtil.runTask(plugin, () -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                String error = module.getConfig().getString("messages.bal.error", "Player not found.");
                event.getHook().sendMessage(error.replace("&", "§") + "").queue();
                return;
            }

            econ.depositPlayer(target, amount);

            String success = module.getConfig().getString("messages.eco.success", "Success!")
                    .replace("{player}", target.getName() != null ? target.getName() : "Unknown")
                    .replace("{amount}", String.valueOf(amount));

            event.getHook().sendMessage(success.replace("&", "§") + "").queue();
        });
    }
}
