package com.nettyforge.cordsync.modules.economy;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.modules.CordModule;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.Bukkit;

import java.util.Arrays;

public class EconomyModule extends CordModule {

    private EconomySlashCommands slashListener;

    public EconomyModule(CordSync plugin) {
        super(plugin, "Economy Module");
    }

    @Override
    protected void setupDefaultConfig() {
        getConfig().set("economy.admin-role-id", "YOUR_ADMIN_ROLE_ID");
        getConfig().set("messages.embed-color", "#2B2D31");

        // PAPI Stats Command
        getConfig().set("messages.stats.title", "\uD83D\uDCCA Player Statistics: {player}");
        getConfig().set("messages.stats.not-linked", "§cThis player has not linked their Discord account.");
        getConfig().set("messages.stats.player-not-found", "§cPlayer not found or has never joined the server.");
        getConfig().set("messages.stats.lines", Arrays.asList(
                "**Kills:** %statistic_player_kills%",
                "**Deaths:** %statistic_deaths%",
                "**Time Played:** %statistic_time_played%"));

        // Vault Bal Command
        getConfig().set("messages.bal.title", "\uD83D\uDCB0 Player Balance: {player}");
        getConfig().set("messages.bal.amount", "**Balance:** ${amount}");
        getConfig().set("messages.bal.error", "§cCould not retrieve balance. Player may not exist.");

        // Vault Eco Give Command
        getConfig().set("messages.eco.success", "§aSuccessfully added ${amount} to {player}'s balance.");
        getConfig().set("messages.eco.no-permission", "§cYou do not have permission to use this command.");

        saveConfig();
        plugin.getLogger().info("⚙ Created default config for EconomyModule!");
    }

    @Override
    public void onEnable() {
        // Safe checks
        boolean hasVault = Bukkit.getPluginManager().getPlugin("Vault") != null;
        boolean hasPAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        if (!hasVault)
            plugin.getLogger().warning("Vault not found! /bal and /eco command integrations will be disabled.");
        if (!hasPAPI)
            plugin.getLogger().warning("PlaceholderAPI not found! /stats command integration will be disabled.");

        this.slashListener = new EconomySlashCommands(plugin, this, hasVault, hasPAPI);

        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null) {
            plugin.getDiscordBot().getJda().addEventListener(slashListener);

            // Register Global Slash Commands
            plugin.getDiscordBot().getJda().updateCommands().addCommands(
                    Commands.slash("stats", "View a player's server statistics.")
                            .addOptions(new OptionData(OptionType.STRING, "player", "Minecraft username", true)),
                    Commands.slash("bal", "View a player's server balance.")
                            .addOptions(new OptionData(OptionType.STRING, "player", "Minecraft username", true)),
                    Commands.slash("eco", "Economy management.")
                            .addSubcommands(
                                    new net.dv8tion.jda.api.interactions.commands.build.SubcommandData("give",
                                            "Add money to a player's balance.")
                                            .addOptions(
                                                    new OptionData(OptionType.STRING, "player", "Minecraft username",
                                                            true),
                                                    new OptionData(OptionType.INTEGER, "amount", "Amount to give",
                                                            true))))
                    .queue();
        }

        plugin.getLogger().info("💰 Economy & Stats Module hooked!");
    }

    @Override
    public void onDisable() {
        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null && slashListener != null) {
            plugin.getDiscordBot().getJda().removeEventListener(slashListener);
            // Optionally we don't clear all global commands here as it wipes other modules'
            // commands
        }
        plugin.getLogger().info("💰 Economy & Stats Module unhooked.");
    }
}
