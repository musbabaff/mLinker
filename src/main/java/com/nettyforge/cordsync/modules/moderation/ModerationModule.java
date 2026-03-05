package com.nettyforge.cordsync.modules.moderation;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.modules.CordModule;
import com.nettyforge.cordsync.utils.SchedulerUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

public class ModerationModule extends CordModule implements Listener {

    private boolean isRunning = false;
    private ModerationDiscordListener jdaListener;

    // SPARK: Cached config values (read once, not per-chat-event)
    private List<String> cachedForbiddenWords;
    private String cachedLogChannelId;

    public ModerationModule(CordSync plugin) {
        super(plugin, "Moderation Module");
    }

    @Override
    protected void setupDefaultConfig() {
        getConfig().set("log-channel-id", "YOUR_LOG_CHANNEL_ID");
        getConfig().set("forbidden-words", java.util.Arrays.asList("fuck", "shit", "bitch"));
        getConfig().set("mute-command", "mute {player} {duration} {reason}");
        getConfig().set("kick-command", "kick {player} {reason}");
        getConfig().set("ban-command", "ban {player} {duration} {reason}");
        saveConfig();
        plugin.getLogger().info("🔨 Created default config for ModerationModule!");
    }

    @Override
    public void onEnable() {
        isRunning = true;

        // SPARK: Cache config once — avoids per-event getStringList/getString calls
        cachedForbiddenWords = getConfig().getStringList("forbidden-words")
                .stream().map(String::toLowerCase).collect(java.util.stream.Collectors.toList());
        cachedLogChannelId = getConfig().getString("log-channel-id", "");

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        jdaListener = new ModerationDiscordListener();
        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null) {
            plugin.getDiscordBot().getJda().addEventListener(jdaListener);
        }
        plugin.getLogger().info("🔨 Moderation Module Online! Modal punishment system active.");
    }

    @Override
    public void onDisable() {
        isRunning = false;
        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null && jdaListener != null) {
            plugin.getDiscordBot().getJda().removeEventListener(jdaListener);
        }
        plugin.getLogger().info("🔨 Moderation Module Offline.");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!isRunning)
            return;

        String message = event.getMessage().toLowerCase();

        // SPARK: Use pre-cached & pre-lowercased keywords — zero allocation per event
        for (String word : cachedForbiddenWords) {
            if (message.contains(word)) {
                sendModerationAlert(event.getPlayer().getName(), event.getMessage());
                break;
            }
        }
    }

    public void sendModerationAlert(String playerName, String reasonMessage) {
        if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)
            return;

        // SPARK: Use cached channel ID
        if (cachedLogChannelId.isEmpty() || cachedLogChannelId.equals("YOUR_LOG_CHANNEL_ID"))
            return;

        SchedulerUtil.runAsync(plugin, () -> {
            TextChannel channel = plugin.getDiscordBot().getJda()
                    .getTextChannelById(Objects.requireNonNull(cachedLogChannelId));
            if (channel != null) {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("🚨 Chat Flag: " + playerName);
                eb.setDescription("**Message:**\n" + reasonMessage);
                eb.setColor(Color.RED);
                eb.setThumbnail("https://mc-heads.net/avatar/" + playerName + "/100.png");

                Button muteBtn = Button.primary("mod_mute_" + playerName, "🔇 Mute");
                Button kickBtn = Button.secondary("mod_kick_" + playerName, "🚷 Kick");
                Button banBtn = Button.danger("mod_ban_" + playerName, "⛔ Ban");

                channel.sendMessageEmbeds(eb.build())
                        .addActionRow(muteBtn, kickBtn, banBtn)
                        .queue();
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // JDA Listener: Buttons → Modal Pop-up → ModalInteraction
    // ═══════════════════════════════════════════════════════════════
    private class ModerationDiscordListener extends ListenerAdapter {

        // ── STEP 1: Button Click → Open Modal Form ──
        @Override
        public void onButtonInteraction(@javax.annotation.Nonnull ButtonInteractionEvent event) {
            if (!isRunning)
                return;

            String componentId = event.getComponentId();
            if (!componentId.startsWith("mod_"))
                return;

            String actionType; // mute, kick, ban
            String playerName;

            if (componentId.startsWith("mod_mute_")) {
                actionType = "mute";
                playerName = componentId.substring("mod_mute_".length());
            } else if (componentId.startsWith("mod_kick_")) {
                actionType = "kick";
                playerName = componentId.substring("mod_kick_".length());
            } else if (componentId.startsWith("mod_ban_")) {
                actionType = "ban";
                playerName = componentId.substring("mod_ban_".length());
            } else {
                return;
            }

            // Build the Modal (Pop-up Form)
            // Modal ID format: modform_<action>_<playerName>
            String modalId = "modform_" + actionType + "_" + playerName;

            TextInput reasonInput = TextInput.create("reason", "Reason", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("e.g. Spam, Toxicity, Hacking...")
                    .setRequired(true)
                    .setMinLength(3)
                    .setMaxLength(200)
                    .build();

            String modalTitle = getModalTitle(actionType, playerName);
            Modal.Builder modalBuilder = Modal.create(modalId, modalTitle != null ? modalTitle : "Moderation");

            // Mute & Ban get a Duration field; Kick does not
            if (actionType.equals("mute") || actionType.equals("ban")) {
                TextInput durationInput = TextInput.create("duration", "Duration", TextInputStyle.SHORT)
                        .setPlaceholder("e.g. 1h, 3d, 30m, permanent")
                        .setRequired(true)
                        .setMinLength(1)
                        .setMaxLength(20)
                        .build();
                modalBuilder.addComponents(ActionRow.of(durationInput), ActionRow.of(reasonInput));
            } else {
                // Kick: only reason
                modalBuilder.addComponents(ActionRow.of(reasonInput));
            }

            event.replyModal(modalBuilder.build()).queue();
        }

        // ── STEP 2: Modal Submitted → Execute Punishment ──
        @Override
        public void onModalInteraction(@javax.annotation.Nonnull ModalInteractionEvent event) {
            if (!isRunning)
                return;

            String modalId = event.getModalId();
            if (!modalId.startsWith("modform_"))
                return;

            // Parse: modform_<action>_<playerName>
            String withoutPrefix = modalId.substring("modform_".length());
            int firstUnderscore = withoutPrefix.indexOf('_');
            if (firstUnderscore == -1)
                return;

            String actionType = withoutPrefix.substring(0, firstUnderscore);
            String playerName = withoutPrefix.substring(firstUnderscore + 1);

            // Extract form values
            ModalMapping reasonMapping = event.getValue("reason");
            String reason = reasonMapping != null ? reasonMapping.getAsString() : "No reason provided";

            String duration = "";
            if (actionType.equals("mute") || actionType.equals("ban")) {
                ModalMapping durationMapping = event.getValue("duration");
                duration = durationMapping != null ? durationMapping.getAsString() : "1h";
            }

            // Determine config key
            String configKey = actionType + "-command";
            String commandTemplate = getConfig().getString(configKey);

            if (commandTemplate == null) {
                event.reply("⚠️ Configuration error: `" + configKey + "` not found in config.")
                        .setEphemeral(true).queue();
                return;
            }

            // Build the final command
            String finalCommand = commandTemplate
                    .replace("{player}", playerName)
                    .replace("{reason}", reason)
                    .replace("{duration}", duration);

            // Dispatch on Bukkit main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
            });

            // Send ephemeral confirmation to the staff member
            String actionLabel = getActionLabel(actionType);
            event.reply("✅ **" + actionLabel + "** applied on `" + playerName + "`"
                    + (duration.isEmpty() ? "" : " for **" + duration + "**")
                    + "\n📝 Reason: *" + reason + "*")
                    .setEphemeral(true).queue();

            // Disable original buttons on the log message to prevent double-punishment
            net.dv8tion.jda.api.entities.Message originalMsg = event.getMessage();
            if (originalMsg != null) {
                originalMsg.editMessageComponents(
                        ActionRow.of(
                                Button.primary("mod_mute_" + playerName, "🔇 Mute").asDisabled(),
                                Button.secondary("mod_kick_" + playerName, "🚷 Kick").asDisabled(),
                                Button.danger("mod_ban_" + playerName, "⛔ Ban").asDisabled()))
                        .queue();
            }
        }

        private String getModalTitle(String actionType, String playerName) {
            return switch (actionType) {
                case "mute" -> "🔇 Mute " + playerName;
                case "kick" -> "🚷 Kick " + playerName;
                case "ban" -> "⛔ Ban " + playerName;
                default -> "Moderation: " + playerName;
            };
        }

        private String getActionLabel(String actionType) {
            return switch (actionType) {
                case "mute" -> "🔇 Mute";
                case "kick" -> "🚷 Kick";
                case "ban" -> "⛔ Ban";
                default -> "Action";
            };
        }
    }
}
