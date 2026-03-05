package com.nettyforge.cordsync.commands;

import java.awt.Color;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.managers.LinkManager;
import com.nettyforge.cordsync.managers.RewardManager;
import com.nettyforge.cordsync.rewards.RewardLogManager;
import com.nettyforge.cordsync.storage.StorageProvider;
import com.nettyforge.cordsync.utils.MessageUtil;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;

@SuppressWarnings("null")
public class LinkVerifyCommand extends ListenerAdapter {

    private final CordSync plugin;

    // Pending confirmations: buttonId -> PendingLink
    private final ConcurrentHashMap<String, PendingLink> pendingConfirmations = new ConcurrentHashMap<>();

    public LinkVerifyCommand(CordSync plugin) {
        this.plugin = plugin;
    }

    // ===================================================================
    // BUTTON INTERACTIONS
    // ===================================================================

    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        // ---- LINK ACCOUNT BUTTON (opens modal form) ----
        if (buttonId.equals("cordsync_link_modal")) {

            // Check alt-account protection
            if (plugin.getModuleLoader() != null) {
                com.nettyforge.cordsync.modules.security.SecurityModule securityModule = (com.nettyforge.cordsync.modules.security.SecurityModule) plugin
                        .getModuleLoader().getModule("Security Module");

                if (securityModule != null
                        && securityModule.getConfig().getBoolean("alt-account-protection.enabled", false)) {
                    int minDays = securityModule.getConfig()
                            .getInt("alt-account-protection.min-discord-account-age-days", 7);
                    long daysCreated = java.time.temporal.ChronoUnit.DAYS.between(event.getUser().getTimeCreated(),
                            java.time.OffsetDateTime.now());

                    if (daysCreated < minDays) {
                        String msg = securityModule.getConfig().getString("alt-account-protection.message",
                                "&cYour Discord account is too new to be linked.");
                        event.reply(msg.replace("&", "§")).setEphemeral(true).queue();
                        return;
                    }
                }
            }

            // Check if already linked
            StorageProvider storage = plugin.getStorageProvider();
            if (storage.isDiscordLinked(event.getUser().getId())) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setDescription("### " + MessageUtil.get("discord.already-linked-title") + "\n\n"
                                + MessageUtil.get("discord.already-linked-desc"))
                        .setColor(java.awt.Color.decode("#2B2D31"))
                        .setFooter("CordSync");
                event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                return;
            }

            TextInput codeInput = TextInput
                    .create("cordsync_code", MessageUtil.getRaw("discord-embeds.commands.link-modal-label"),
                            TextInputStyle.SHORT)
                    .setPlaceholder(MessageUtil.getRaw("discord-embeds.commands.link-modal-placeholder"))
                    .setRequired(true)
                    .setMinLength(4)
                    .setMaxLength(10)
                    .build();

            Modal modal = Modal
                    .create("cordsync_link_form", MessageUtil.getRaw("discord-embeds.commands.link-modal-title"))
                    .addComponents(ActionRow.of(codeInput))
                    .build();

            event.replyModal(modal).queue();
            return;
        }

        // ---- HOW TO LINK INFO BUTTON ----
        if (buttonId.equals("cordsync_howto_info")) {
            String details = plugin.getConfig().getString("discord.auto-message.button-details",
                    "Details not configured.");
            EmbedBuilder embed = new EmbedBuilder()
                    .setDescription("### \uD83D\uDCD6 How to Link Your Account\n\n" + details)
                    .setColor(java.awt.Color.decode("#2B2D31"))
                    .setFooter("CordSync");
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }

        // ---- BOOSTER INFO BUTTON ----
        if (buttonId.equals("cordsync_booster_info")) {
            String details = plugin.getConfig().getString("discord.booster-message.button-details",
                    "Details not configured.");
            EmbedBuilder embed = new EmbedBuilder()
                    .setDescription("### " + MessageUtil.getRaw("booster.info-title") + "\n\n"
                            + MessageUtil.getRaw("booster.info-desc") + "\n\n" + details)
                    .setColor(java.awt.Color.decode("#2B2D31"))
                    .setFooter("CordSync");
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }

        // ---- ACCOUNT STATUS BUTTON ----
        if (buttonId.equals("cordsync_status")) {
            handleStatusButton(event);
            return;
        }

        // ---- CONFIRM BUTTON ----
        if (buttonId.startsWith("cordsync_confirm_")) {
            PendingLink pending = pendingConfirmations.remove(buttonId);
            if (pending == null) {
                String msg = MessageUtil.get("discord.confirm-expired");
                event.reply(msg != null ? msg : "Confirmation expired!").setEphemeral(true).queue();
                return;
            }
            if (!event.getUser().getId().equals(pending.discordId)) {
                event.reply("\u274C This confirmation is not yours!").setEphemeral(true).queue();
                pendingConfirmations.put(buttonId, pending);
                return;
            }
            performLink(event, pending);
            return;
        }

        // ---- CANCEL BUTTON ----
        if (buttonId.startsWith("cordsync_cancel_")) {
            String confirmId = buttonId.replace("cancel", "confirm");
            pendingConfirmations.remove(confirmId);
            EmbedBuilder embed = new EmbedBuilder()
                    .setDescription("### " + MessageUtil.getRaw("discord-embeds.ui.confirm-title-cancelled") + "\n\n"
                            + MessageUtil.getRaw("discord-embeds.ui.confirm-cancelled"))
                    .setColor(java.awt.Color.decode("#2B2D31"))
                    .setFooter("CordSync");
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }

        // ---- TOGGLE 2FA BUTTON ----
        if (buttonId.equals("cordsync_toggle_2fa")) {
            StorageProvider storage = plugin.getStorageProvider();
            String discordId = event.getUser().getId();
            if (!storage.isDiscordLinked(discordId)) {
                event.reply("\u274C You must link your account first!").setEphemeral(true).queue();
                return;
            }
            UUID uuid = storage.getPlayerUUID(discordId);
            if (uuid == null)
                return;

            boolean isEnabled = plugin.getTwoFactorEnabled(uuid);
            plugin.setTwoFactorEnabled(uuid, !isEnabled);
            String state = !isEnabled ? "\u2705 **Enabled**" : "\u274C **Disabled**";
            event.reply("Your 2FA login security has been " + state + ". (Effective immediately)").setEphemeral(true)
                    .queue();
            return;
        }
    }

    // ===================================================================
    // MODAL FORM SUBMIT (Code entry)
    // ===================================================================

    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {
        if (!event.getModalId().equals("cordsync_link_form"))
            return;

        String code = event.getValue("cordsync_code").getAsString().trim();
        if (code.isEmpty()) {
            sendEmbed(event, MessageUtil.get("discord.code-missing-title"),
                    MessageUtil.get("discord.code-missing-desc"), Color.RED);
            return;
        }

        LinkManager linkManager = plugin.getLinkManager();
        StorageProvider storage = plugin.getStorageProvider();

        // Already linked?
        if (storage.isDiscordLinked(event.getUser().getId())) {
            sendEmbed(event, MessageUtil.get("discord.already-linked-title"),
                    MessageUtil.get("discord.already-linked-desc"), Color.RED);
            return;
        }

        // Valid code?
        if (!linkManager.isValidCode(code)) {
            sendEmbed(event, MessageUtil.get("discord.invalid-title"),
                    MessageUtil.get("discord.invalid-desc"), Color.RED);
            return;
        }

        UUID uuid = linkManager.getPlayerByCode(code);

        // MC account already linked?
        if (storage.isPlayerLinked(uuid)) {
            sendEmbed(event, MessageUtil.get("discord.player-already-linked-title"),
                    MessageUtil.get("discord.player-already-linked-desc"), Color.RED);
            linkManager.removeCodeByUUID(uuid);
            return;
        }

        // Cooldown check
        long unlinkTime = storage.getUnlinkTimestamp(uuid);
        if (unlinkTime > 0) {
            long cooldownMs = parseDuration(plugin.getConfig().getString("security.unlink-cooldown", "24h"));
            long elapsed = System.currentTimeMillis() - unlinkTime;
            if (elapsed < cooldownMs) {
                String remaining = formatDuration(cooldownMs - elapsed);
                sendEmbed(event, MessageUtil.get("discord.cooldown-title"),
                        MessageUtil.format("discord.cooldown-desc", Map.of("time", remaining)), new Color(255, 165, 0));
                return;
            }
        }

        // Relink limit check
        int maxRelink = plugin.getConfig().getInt("security.max-relink-count", 3);
        if (maxRelink > 0 && storage.getRelinkCount(uuid) >= maxRelink) {
            sendEmbed(event, MessageUtil.get("discord.relink-limit-title"),
                    MessageUtil.format("discord.relink-limit-desc", Map.of("max", String.valueOf(maxRelink))),
                    Color.RED);
            return;
        }

        // Show 2FA confirmation with buttons
        String playerName = Bukkit.getOfflinePlayer(uuid).getName();
        if (playerName == null)
            playerName = "Unknown";

        String confirmId = "cordsync_confirm_" + uuid;
        String cancelId = "cordsync_cancel_" + uuid;

        pendingConfirmations.values().removeIf(p -> p.uuid.equals(uuid));
        pendingConfirmations.put(confirmId, new PendingLink(uuid, playerName, code, event.getUser().getId()));

        EmbedBuilder embed = new EmbedBuilder()
                .setDescription("### " + MessageUtil.getRaw("discord-embeds.ui.confirm-title") + "\n\n"
                        + MessageUtil.getRaw("discord-embeds.ui.confirm-desc").replace("{player}", playerName))
                .setColor(java.awt.Color.decode("#2B2D31"))
                .addField(MessageUtil.getRaw("discord-embeds.ui.status-field-mc"), playerName, true)
                .addField(MessageUtil.getRaw("discord-embeds.ui.status-field-dc"), event.getUser().getName(), true)
                .setFooter("CordSync \u2022 2FA")
                .setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build())
                .addActionRow(
                        Button.success(confirmId, MessageUtil.getRaw("discord-embeds.buttons.confirm")),
                        Button.danger(cancelId, MessageUtil.getRaw("discord-embeds.buttons.cancel")))
                .setEphemeral(true)
                .queue();

        // Auto-expire after 60 seconds
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            pendingConfirmations.remove(confirmId);
        }, 20L * 60);
    }

    // ===================================================================
    // ACCOUNT STATUS CHECK
    // ===================================================================

    private void handleStatusButton(ButtonInteractionEvent event) {
        StorageProvider storage = plugin.getStorageProvider();
        String discordId = event.getUser().getId();

        if (!storage.isDiscordLinked(discordId)) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setDescription("### " + MessageUtil.getRaw("discord-embeds.ui.status-title") + "\n\n"
                            + MessageUtil.getRaw("discord-embeds.ui.status-not-linked-desc"))
                    .addField(MessageUtil.getRaw("discord-embeds.ui.status-field-status"),
                            MessageUtil.getRaw("discord-embeds.ui.status-val-not-linked"), false)
                    .setColor(java.awt.Color.decode("#2B2D31"))
                    .setFooter("CordSync");
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }

        UUID uuid = storage.getPlayerUUID(discordId);
        String playerName = uuid != null ? Bukkit.getOfflinePlayer(uuid).getName() : "Unknown";
        boolean isBoosting = event.getMember() != null && event.getMember().isBoosting();
        int relinkCount = uuid != null ? storage.getRelinkCount(uuid) : 0;

        EmbedBuilder embed = new EmbedBuilder()
                .setDescription("### " + MessageUtil.getRaw("discord-embeds.ui.status-title") + "\n\n"
                        + MessageUtil.getRaw("discord-embeds.ui.status-linked-desc"))
                .addField(MessageUtil.getRaw("discord-embeds.ui.status-field-mc"),
                        playerName != null ? playerName : "Unknown", true)
                .addField(MessageUtil.getRaw("discord-embeds.ui.status-field-dc"), event.getUser().getName(), true)
                .addField(MessageUtil.getRaw("discord-embeds.ui.status-field-relink"), String.valueOf(relinkCount),
                        true)
                .addField(MessageUtil.getRaw("discord-embeds.ui.status-field-booster"),
                        isBoosting ? MessageUtil.getRaw("discord-embeds.ui.status-val-yes")
                                : MessageUtil.getRaw("discord-embeds.ui.status-val-no"),
                        true)
                .addField(MessageUtil.getRaw("discord-embeds.ui.status-field-status"),
                        MessageUtil.getRaw("discord-embeds.ui.status-val-linked"), true)
                .setColor(java.awt.Color.decode("#2B2D31"))
                .setFooter("CordSync")
                .setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    // ===================================================================
    // PERFORM LINK (after confirmation)
    // ===================================================================

    private void performLink(ButtonInteractionEvent event, PendingLink pending) {
        StorageProvider storage = plugin.getStorageProvider();
        LinkManager linkManager = plugin.getLinkManager();

        if (storage.isDiscordLinked(pending.discordId) || storage.isPlayerLinked(pending.uuid)) {
            event.reply(MessageUtil.get("discord.already-linked-title")).setEphemeral(true).queue();
            return;
        }

        if (!linkManager.isValidCode(pending.code)) {
            event.reply(MessageUtil.get("discord.invalid-title")).setEphemeral(true).queue();
            return;
        }

        storage.setLinkedAccount(pending.uuid, pending.playerName, pending.discordId);
        linkManager.removeCodeByUUID(pending.uuid);

        // Track relink
        long unlinkTime = storage.getUnlinkTimestamp(pending.uuid);
        if (unlinkTime > 0) {
            storage.incrementRelinkCount(pending.uuid);
        }

        // Apply verified role
        applyVerifiedRole(event);
        applyLuckPermsRoles(event, pending.uuid);

        // Nickname sync
        applyNicknameSync(event, pending.playerName);

        // Success embed
        EmbedBuilder embed = new EmbedBuilder()
                .setDescription("### " + MessageUtil.getRaw("discord-embeds.ui.success-title") + "\n\n"
                        + MessageUtil.getRaw("discord-embeds.ui.success-desc").replace("{player}", pending.playerName))
                .setColor(java.awt.Color.decode("#2B2D31"))
                .addField(MessageUtil.getRaw("discord-embeds.ui.status-field-mc"), pending.playerName, true)
                .addField(MessageUtil.getRaw("discord-embeds.ui.status-field-dc"), event.getUser().getName(), true)
                .setFooter("CordSync \u2022 Account Linked \u2705")
                .setTimestamp(java.time.Instant.now());
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();

        plugin.getLogger().info(MessageUtil.format("discord.verified-console", Map.of("player", pending.playerName)));
        sendLinkLog(pending.playerName, event.getUser().getName());

        // Bukkit main thread tasks
        final String playerName = pending.playerName;
        final UUID uuid = pending.uuid;
        final boolean isBoosting = event.getMember() != null && event.getMember().isBoosting();

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(MessageUtil.format("link.success", Map.of("player", playerName)));

                RewardManager rewardManager = plugin.getRewardManager();
                RewardLogManager logManager = plugin.getRewardLogManager();

                boolean firstRewardOnce = plugin.getConfig().getBoolean("security.first-reward-once", true);
                boolean alreadyReceived = storage.hasReceivedFirstReward(uuid);

                if (rewardManager != null && (!firstRewardOnce || !alreadyReceived)) {
                    rewardManager.grantFirstLink(player);
                    storage.setFirstRewardReceived(uuid);
                    if (logManager != null) {
                        logManager.logReward(uuid, "first-link", "First link reward granted to " + playerName);
                    }
                }

                if (isBoosting && rewardManager != null) {
                    grantBoosterReward(player, rewardManager, logManager);
                }
            }
        });
    }

    // ===================================================================
    // NICKNAME SYNC
    // ===================================================================

    private void applyNicknameSync(ButtonInteractionEvent event, String mcName) {
        if (!plugin.getConfig().getBoolean("discord.nickname-sync.enabled", false))
            return;

        String format = plugin.getConfig().getString("discord.nickname-sync.format", "[{mcname}] {discordname}");
        Member member = event.getMember();
        if (member == null)
            return;

        String discordName = member.getUser().getName();
        String newNick = format.replace("{mcname}", mcName).replace("{discordname}", discordName);

        try {
            member.modifyNickname(newNick).queue(
                    s -> plugin.getLogger().info("\uD83D\uDC64 Nickname updated: " + newNick),
                    e -> plugin.getLogger().warning("\u26A0 Nickname update failed: " + e.getMessage()));
        } catch (Exception e) {
            plugin.getLogger().warning("\u26A0 Cannot modify nickname: " + e.getMessage());
        }
    }

    // ===================================================================
    // BOOSTER REWARD
    // ===================================================================

    private void grantBoosterReward(Player player, RewardManager rewardManager, RewardLogManager logManager) {
        if (!plugin.getConfig().getBoolean("rewards.booster.enabled", false))
            return;

        String message = plugin.getConfig().getString("rewards.booster.message", "");
        if (message != null && !message.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message)
                    .replace("{player}", player.getName()));
        }

        java.util.List<String> commands = plugin.getConfig().getStringList("rewards.booster.commands");
        for (String cmd : commands) {
            String finalCmd = cmd.replace("{player}", player.getName())
                    .replace("{uuid}", player.getUniqueId().toString());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        }

        java.util.List<?> items = plugin.getConfig().getList("rewards.booster.items");
        if (items != null) {
            rewardManager.giveItemsFromConfig(player, items);
        }

        if (logManager != null) {
            logManager.logReward(player.getUniqueId(), "booster", "Booster reward granted to " + player.getName());
        }
        plugin.getLogger().info("\uD83D\uDE80 " + player.getName() + " received booster reward.");
    }

    // ===================================================================
    // ROLE OPERATIONS
    // ===================================================================

    private void applyVerifiedRole(ButtonInteractionEvent event) {
        String guildId = plugin.getConfig().getString("discord.guild-id");
        String roleId = plugin.getConfig().getString("discord.role-id-verified");
        if (guildId == null || roleId == null || guildId.isEmpty() || roleId.isEmpty())
            return;

        Guild guild = event.getJDA().getGuildById(guildId);
        if (guild == null)
            return;

        Role role = guild.getRoleById(roleId);
        if (role == null)
            return;

        Member member = event.getMember();
        if (member == null)
            member = guild.retrieveMemberById(event.getUser().getId()).complete();
        if (member == null)
            return;

        final Member fm = member;
        guild.addRoleToMember(fm, role).queue(
                s -> plugin.getLogger().info("\u2705 Verified role given to " + fm.getEffectiveName()),
                e -> plugin.getLogger().warning("\u274C Role assignment failed: " + e.getMessage()));
    }

    private void applyLuckPermsRoles(ButtonInteractionEvent event, UUID playerUUID) {
        if (!plugin.getConfig().getBoolean("discord.luckperms-roles.enabled", false))
            return;
        ConfigurationSection mappings = plugin.getConfig().getConfigurationSection("discord.luckperms-roles.mappings");
        if (mappings == null || mappings.getKeys(false).isEmpty())
            return;

        LuckPerms luckPerms;
        try {
            var provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider == null)
                return;
            luckPerms = provider.getProvider();
        } catch (Exception e) {
            return;
        }

        User lpUser = luckPerms.getUserManager().getUser(playerUUID);
        if (lpUser == null) {
            try {
                lpUser = luckPerms.getUserManager().loadUser(playerUUID).join();
            } catch (Exception e) {
                return;
            }
        }
        if (lpUser == null)
            return;

        Set<String> groups = new java.util.HashSet<>();
        String pg = lpUser.getPrimaryGroup();
        if (pg != null)
            groups.add(pg.toLowerCase());
        for (var node : lpUser.getNodes()) {
            if (node.getKey().startsWith("group."))
                groups.add(node.getKey().substring(6).toLowerCase());
        }

        String guildId = plugin.getConfig().getString("discord.guild-id");
        if (guildId == null || guildId.isEmpty())
            return;
        Guild guild = event.getJDA().getGuildById(guildId);
        if (guild == null)
            return;

        Member member = event.getMember();
        if (member == null)
            member = guild.retrieveMemberById(event.getUser().getId()).complete();
        if (member == null)
            return;
        final Member fm = member;

        for (String groupName : mappings.getKeys(false)) {
            String discordRoleId = mappings.getString(groupName);
            if (discordRoleId == null || discordRoleId.isEmpty() || discordRoleId.equals("DISCORD_ROLE_ID_HERE"))
                continue;

            if (groups.contains(groupName.toLowerCase())) {
                Role role = guild.getRoleById(discordRoleId);
                if (role == null)
                    continue;
                guild.addRoleToMember(fm, role).queue(
                        s -> plugin.getLogger()
                                .info("\u2705 LuckPerms role " + groupName + " given to " + fm.getEffectiveName()),
                        e -> plugin.getLogger().warning("\u274C LuckPerms role failed: " + e.getMessage()));
            }
        }
    }

    // ===================================================================
    // HELPERS
    // ===================================================================

    private void sendLinkLog(String playerName, String discordTag) {
        if (plugin.getDiscordBot() == null)
            return;
        String description = MessageUtil.format("discord.log-linked", Map.of("player", playerName))
                + "\n**Discord:** " + discordTag;
        plugin.getDiscordBot().sendLogEmbed("\uD83D\uDD17 Account Linked", description, new Color(0, 200, 83));
    }

    private void sendEmbed(ModalInteractionEvent event, String title, String desc, Color color) {
        EmbedBuilder embed = new EmbedBuilder()
                .setDescription("### " + title + "\n\n" + desc)
                .setColor(java.awt.Color.decode("#2B2D31"))
                .setFooter("CordSync").setTimestamp(java.time.Instant.now());
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private long parseDuration(String duration) {
        if (duration == null || duration.isEmpty())
            return TimeUnit.HOURS.toMillis(24);
        try {
            String num = duration.replaceAll("[^0-9]", "");
            long value = Long.parseLong(num);
            if (duration.endsWith("d"))
                return TimeUnit.DAYS.toMillis(value);
            if (duration.endsWith("h"))
                return TimeUnit.HOURS.toMillis(value);
            if (duration.endsWith("m"))
                return TimeUnit.MINUTES.toMillis(value);
            return TimeUnit.HOURS.toMillis(value);
        } catch (NumberFormatException e) {
            return TimeUnit.HOURS.toMillis(24);
        }
    }

    private String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        if (hours > 0)
            return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    // ===================================================================
    // PENDING LINK DATA CLASS
    // ===================================================================

    private static class PendingLink {
        final UUID uuid;
        final String playerName;
        final String code;
        final String discordId;

        PendingLink(UUID uuid, String playerName, String code, String discordId) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.code = code;
            this.discordId = discordId;
        }
    }
}
