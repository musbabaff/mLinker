package com.blockstock.mlinker.discord;

import java.awt.Color;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.blockstock.mlinker.MLinker;
import com.blockstock.mlinker.commands.LinkVerifyCommand;
import com.blockstock.mlinker.utils.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class DiscordBot {

    private final MLinker plugin;
    private JDA jda;
    private BukkitTask statusTask;

    @SuppressWarnings("null")
    public DiscordBot(MLinker plugin, String token, String status) {
        this.plugin = plugin;
        try {
            // Botun ihtiyaç duyduğu Gateway Intent'leri (Geliştirici portalından da açık
            // olmalı!)
            EnumSet<GatewayIntent> intents = EnumSet.of(
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS);

            // JDA Kurulumu
            jda = JDABuilder.createDefault(token != null ? token : "")
                    .enableIntents(java.util.Collections.unmodifiableCollection(intents))
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setActivity(Activity.playing(status != null ? status : ""))
                    .addEventListeners(new LinkVerifyCommand(plugin)) // Discord etkileşim dinleyicisi
                    .build();

            // Botun tamamen hazır olmasını bekle
            jda.awaitReady();

            // Discord Slash Komutunu kaydet (/hesapesle)
            String slashDesc = MessageUtil.get("discord.slash-description");
            String slashOptDesc = MessageUtil.get("discord.slash-option-description");
            jda.updateCommands().addCommands(
                    Commands.slash("hesapesle", slashDesc != null ? slashDesc : "")
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
                                    "kod",
                                    slashOptDesc != null ? slashOptDesc : "",
                                    true))
                    .queue(
                            success -> plugin.getLogger().info(MessageUtil.get("discord.slash-registered")),
                            failure -> plugin.getLogger().severe(
                                    MessageUtil.format("discord.slash-failed", Map.of("error", failure.getMessage()))));

            plugin.getLogger().info(MessageUtil.get("discord.started"));

            // Oto mesaj gönderimi
            sendAutoMessage();

            // Oyuncu sayısı status güncelleyici
            startStatusUpdater();

        } catch (InterruptedException e) {
            plugin.getLogger().severe("Discord bot başlatılırken kesintiye uğradı: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            plugin.getLogger().severe(MessageUtil.format("discord.failed", Map.of("error", e.getMessage())));
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (statusTask != null) {
            statusTask.cancel();
            statusTask = null;
        }
        if (jda != null) {
            jda.shutdownNow();
            plugin.getLogger().info(MessageUtil.get("discord.bot-stopped"));
        }
    }

    public JDA getJda() {
        return jda;
    }

    // ===================================================================
    // STATUS GÜNCELLEYICI - Oyuncu sayısını Discord bot durumunda gösterir
    // ===================================================================

    /**
     * Her 30 saniyede bir Discord bot durumunu günceller.
     * Format: "X Oyuncu Çevrimiçi | /hesapesle"
     */
    private void startStatusUpdater() {
        if (jda == null)
            return;

        // Her 30 saniyede bir güncelle (20 tick = 1 saniye, 20*30 = 600 tick)
        statusTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (jda == null || jda.getStatus() != JDA.Status.CONNECTED)
                return;

            int onlinePlayers = Bukkit.getOnlinePlayers().size();
            String statusText = onlinePlayers + " Oyuncu Çevrimiçi | /hesapesle";
            jda.getPresence().setActivity(Activity.playing(statusText));
        }, 20L * 10, 20L * 30); // 10 saniye sonra başla, 30 saniyede bir tekrarla

        plugin.getLogger().info("🎮 Bot durumu oyuncu sayısı güncelleyicisi başlatıldı.");
    }

    /**
     * DİKKAT: Bu metot .complete() kullandığı için senkron çalışır.
     * Sadece asenkron (runTaskAsynchronously) Bukkit task'ları içinden
     * çağırılmalıdır!
     */
    public boolean isMemberInGuild(String discordId) {
        if (jda == null)
            return false;
        try {
            String guildIdStr = plugin.getConfig().getString("discord.guild-id", "");
            Guild guild = jda.getGuildById(guildIdStr != null ? guildIdStr : "");
            if (guild == null)
                return false;

            Member member = guild.retrieveMemberById(discordId != null ? discordId : "").complete();
            return member != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * DİKKAT: Bu metot .complete() kullandığı için senkron çalışır.
     * Sadece asenkron (runTaskAsynchronously) Bukkit task'ları içinden
     * çağırılmalıdır!
     */
    public boolean hasVerifiedRole(String discordId) {
        if (jda == null)
            return false;
        try {
            String guildIdStr = plugin.getConfig().getString("discord.guild-id", "");
            Guild guild = jda.getGuildById(guildIdStr != null ? guildIdStr : "");
            if (guild == null)
                return false;

            Member member = guild.retrieveMemberById(discordId != null ? discordId : "").complete();
            if (member == null)
                return false;

            String verifiedRoleId = plugin.getConfig().getString("discord.role-id-verified");
            return member.getRoles().stream().anyMatch(role -> role.getId().equals(verifiedRoleId));
        } catch (Exception e) {
            return false;
        }
    }

    // ===================================================================
    // LOG KANALI - Hesap eşleme/kaldırma işlemlerini log kanalına gönderir
    // ===================================================================

    /**
     * Log kanalına embed mesaj gönderir.
     * 
     * @param title       Embed başlığı
     * @param description Embed açıklaması
     * @param color       Embed rengi
     */
    public void sendLogEmbed(String title, String description, Color color) {
        if (jda == null)
            return;

        String logChannelId = plugin.getConfig().getString("discord.log-channel-id", "");
        if (logChannelId == null || logChannelId.isEmpty())
            return;

        try {
            TextChannel channel = jda.getTextChannelById(logChannelId);
            if (channel == null) {
                plugin.getLogger().warning(MessageUtil.get("discord.webhook-missing"));
                return;
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(description)
                    .setColor(color)
                    .setFooter("mLinker Log")
                    .setTimestamp(java.time.Instant.now());

            channel.sendMessageEmbeds(embed.build()).queue(
                    success -> plugin.getLogger().info(MessageUtil.get("discord.webhook-success")),
                    failure -> plugin.getLogger().warning(MessageUtil.format("discord.webhook-fail",
                            Map.of("error", failure.getMessage()))));
        } catch (Exception e) {
            plugin.getLogger().warning(MessageUtil.format("discord.webhook-fail",
                    Map.of("error", e.getMessage())));
        }
    }

    // ===================================================================
    // OTO MESAJ - Hesap eşleme kanalına 1 kere bilgilendirme mesajı atar
    // ===================================================================

    /**
     * Plugin başladığında hesapesle kanalına bilgilendirme embed'i gönderir.
     * Eğer kanalda botun daha önceden gönderdiği bir mesaj varsa tekrar göndermez.
     */
    private void sendAutoMessage() {
        if (jda == null)
            return;

        boolean enabled = plugin.getConfig().getBoolean("discord.auto-message.enabled", false);
        if (!enabled)
            return;

        String channelId = plugin.getConfig().getString("discord.auto-message.channel-id", "");
        if (channelId == null || channelId.isEmpty())
            return;

        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                plugin.getLogger().warning("⚠ Oto mesaj kanalı bulunamadı: " + channelId);
                return;
            }

            // Kanaldaki son 50 mesajı kontrol et - botun mesajı var mı?
            List<Message> messages = channel.getHistory().retrievePast(50).complete();
            boolean botMessageExists = false;

            for (Message msg : messages) {
                if (msg.getAuthor().getId().equals(jda.getSelfUser().getId())) {
                    botMessageExists = true;
                    break;
                }
            }

            if (botMessageExists) {
                plugin.getLogger().info(MessageUtil.get("discord.auto-message-exists"));
                return;
            }

            // Mesaj yok, yeni embed gönder
            String title = plugin.getConfig().getString("discord.auto-message.embed-title", "🔗 Hesap Eşleştirme");
            String description = plugin.getConfig().getString("discord.auto-message.embed-description",
                    "Minecraft hesabınızı Discord hesabınıza bağlamak için oyun içinde `/hesapesle` yazın.");
            String colorHex = plugin.getConfig().getString("discord.auto-message.embed-color", "#00BFFF");

            Color embedColor;
            try {
                embedColor = Color.decode(colorHex);
            } catch (Exception e) {
                embedColor = new Color(0, 191, 255); // Varsayılan mavi
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(description)
                    .setColor(embedColor)
                    .setFooter("mLinker • Hesap Eşleştirme Sistemi")
                    .setTimestamp(java.time.Instant.now());

            channel.sendMessageEmbeds(embed.build()).queue(
                    success -> plugin.getLogger().info(MessageUtil.get("discord.auto-message-sent")),
                    failure -> plugin.getLogger().warning("❌ Oto mesaj gönderilemedi: " + failure.getMessage()));

        } catch (Exception e) {
            plugin.getLogger().warning("❌ Oto mesaj kontrolü sırasında hata: " + e.getMessage());
        }
    }
}