# 🚀 Setup Guide

Step-by-step guide to set up CordSync on your Minecraft server.

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| **Java** | 17+ | [Download here](https://adoptium.net/) |
| **Server** | Spigot/Paper 1.19+ | Folia is also supported |
| **Discord Account** | — | Needed to create a bot |
| **Discord Server** | — | The server you want to bridge |

---

## Step 1: Create a Discord Bot

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications)
2. Click **"New Application"** → Name it (e.g., "CordSync Bot")
3. Go to the **"Bot"** tab on the left sidebar
4. Click **"Add Bot"** → Confirm
5. Under the bot's username, click **"Reset Token"** → **Copy** the token

> ⚠️ **NEVER share your bot token publicly!** Anyone with the token can control your bot.

6. Scroll down and enable these **Privileged Gateway Intents**:
   - ✅ **Presence Intent**
   - ✅ **Server Members Intent**
   - ✅ **Message Content Intent**

---

## Step 2: Invite the Bot to Your Server

1. Go to the **"OAuth2"** tab → **"URL Generator"**
2. Under **Scopes**, check:
   - ✅ `bot`
   - ✅ `applications.commands`
3. Under **Bot Permissions**, check:
   - ✅ `Administrator` (or manually select: Manage Channels, Manage Roles, Send Messages, Embed Links, Read Message History, Add Reactions, Use Slash Commands, Manage Threads, Create Public Threads)
4. Copy the generated URL at the bottom → Open it in your browser
5. Select your Discord server → Authorize

---

## Step 3: Install CordSync

1. Download the latest `CordSync.jar` from [Releases](https://github.com/musbabaff/CordSync/releases)
2. Place it in your server's `plugins/` folder
3. **Start the server** — CordSync will generate all config files
4. **Stop the server**

---

## Step 4: Configure the Bot

### 4.1 — Main Configuration

Edit `plugins/CordSync/config.yml`:

```yaml
discord:
  token: "PASTE_YOUR_BOT_TOKEN_HERE"
  guild-id: "YOUR_DISCORD_SERVER_ID"
  role-id-verified: "VERIFIED_ROLE_ID"
  log-channel-id: "LOG_CHANNEL_ID"
```

#### How to get IDs:
1. Open **Discord Settings** → **Advanced** → Enable **Developer Mode**
2. Right-click your **server name** → **Copy Server ID** → paste as `guild-id`
3. Right-click a **role** → **Copy Role ID** → paste as `role-id-verified`
4. Right-click a **channel** → **Copy Channel ID** → paste as `log-channel-id`

### 4.2 — Module Configuration

Edit `plugins/CordSync/modules.yml` to enable/disable modules:

```yaml
modules:
  report-module: true
  live-status: true
  moderation-module: true
  leaderboard-module: true
  # Set to false to disable any module
```

Each enabled module creates its own config at `plugins/CordSync/modules/<module>/config.yml`.

---

## Step 5: Start the Server

1. Start the server
2. Check the console for the CordSync ASCII logo and `✅` messages:

```
  ____              _ ____
 / ___|___  _ __ __| / ___|_   _ _ __   ___
| |   / _ \| '__/ _`|\___ \| | | | '_ \ / __|
| |__| (_) | | | (_| | ___) || |_| | | | | (__
 \____\___/|_|  \__,_||____/ \__, |_| |_|\___|
                              |___/
                               v1.4.0

✅ Discord bot connected!
✅ /link slash command registered!
📊 Live Status Module hooked!
🚨 Report System hooked! /report and /bug are now active.
🏆 Leaderboard Module Online!
🔨 Moderation Module Online!
```

3. Test with `/link` in Minecraft to verify the bot responds

---

## Step 6: Set Up Live Status (Optional)

1. In a Discord channel, send any message (e.g., "Loading...")
2. Right-click the message → **Copy Message ID**
3. Edit `plugins/CordSync/modules/livestatus/config.yml`:

```yaml
discord:
  status-channel-id: "THE_CHANNEL_ID"
  message-id: "THE_MESSAGE_ID"
  update-interval: 30
```

4. Run `/csreload` or restart → The message will be replaced with the live status embed

---

## Troubleshooting

| Issue | Solution |
|---|---|
| Bot doesn't come online | Verify your `token` in `config.yml`. Ensure all Gateway Intents are enabled. |
| Slash command not appearing | Wait 1-2 minutes for Discord to propagate. Try restarting the bot. |
| "Please set the channel-id" warnings | Update the relevant module config with actual Discord channel IDs. |
| Leaderboard shows "No data" | Ensure `ajLeaderboards` or `PlaceholderAPI` is installed. |
| Voice module won't start | Install `WorldGuard` — it's a required soft-dependency for this module. |
| 2FA always says "Expired" | This was fixed in v1.4.0. Make sure you're running the latest version. |

---

## Next Steps

- 📖 [Configuration Guide](Configuration.md) — Deep dive into all config options
- 📋 [Commands & Permissions](Commands-&-Permissions.md) — Full command reference
- 📦 [Modules](Modules.md) — Detailed module documentation
