<div align="center">

# ⚡ CordSync

### Advanced Discord ↔ Minecraft Account Linking System

[![Version](https://img.shields.io/github/v/release/musbabaff/CordSync?color=blue&label=version)](https://github.com/musbabaff/CordSync/releases)
[![License](https://img.shields.io/badge/license-Custom%20EULA-red.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.8--1.21+-brightgreen.svg)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://adoptium.net/)
[![Folia](https://img.shields.io/badge/Folia-Supported-purple.svg)](https://papermc.io/software/folia)

**The most feature-rich Discord account linking plugin for Minecraft servers.**

[Features](#-features) • [Installation](#-installation) • [Configuration](#-configuration) • [Commands](#-commands) • [API](#-api) • [Wiki](https://github.com/musbabaff/CordSync/wiki)

---

</div>

## 🌟 Premium Features

### 📸 Webhook Chat Bridge
- **Minecraft Skins in Discord** — Messages from MC use the player's 3D head as their Discord avatar!
- **Immersive Experience** — Feels like a native Discord integration
- **Message Sanitization** — Prevents ping exploits

### 🖥️ Secure Console Bridge
- **Two-way Console** — View live server logs and send commands directly from a protected Discord channel
- **Role-Based Security** — Only authorized Discord admins can access the bridge
- **Command Blacklist** — Prevent dangerous commands (`/stop`, `/op`) from being run via Discord

### 🎒 Interactive In-Game GUI
- **Chest Menu Integration** — A beautiful, fully localized `/link` chest interface
- **No More Complex Commands** — Players link their accounts with intuitive button clicks

### 📦 PlaceholderAPI Support
- **Dynamic Placeholders** — Display linking status seamlessly in Tablists, Scoreboards, and Chat
- **Available Tags** — `%cordsync_is_linked%`, `%cordsync_discord_name%`, `%cordsync_discord_role%`, and 5 more!

### 🔔 Rich Join/Quit Embeds
- **Visual Tracking** — Automatically broadcast beautiful embeds to Discord when a player joins or leaves
- **Player Avatars** — Includes the player's 3D Minecraft face alongside their mapped Discord tag

### 🔄 Reverse Sync (LuckPerms)
- **Discord → Minecraft** — Automatically give LuckPerms groups when a player gets a Discord role
- **Supporter Perks** — Instantly grant in-game ranks to your Discord Boosters and Patreons

### 🛡️ 2FA Login Protection
- **IP Verification** — Detects unknown IPs on join and securely requests Discord confirmation
- **Anti-Grief Shield** — If an account is compromised, the attacker cannot log in without Discord approval!

### 🌍 100% Configurable & Localized
- **5 Built-In Languages** — English, Turkish, German, Spanish, French included out of the box
- **Fully Customizable** — Every single string, GUI title, and item lore is dynamically configurable

### ⚡ Performance & Compatibility
- **Folia Support** — Fully compatible with Folia's regionalized threading via native async scaling
- **Multi-Version** — Works flawlessly on Minecraft 1.8 through 1.21.4+
- **Proxy Ready** — BungeeCord & Velocity compatible

### 🔧 Developer-Friendly
- **Config auto-migration** — Updates configs without losing your settings
- **Comprehensive API** — Hook into CordSync from your plugins
- **Event system** — `PlayerLinkedEvent` / `PlayerUnlinkedEvent`

---

## 📦 Installation

1. Download the latest release from [Releases](https://github.com/musbabaff/CordSync/releases)
2. Place `CordSync-1.2.9.jar` in your server's `plugins/` folder
3. Restart the server
4. Edit `plugins/CordSync/config.yml` with your Discord bot token
5. Reload with `/csreload`

### Discord Bot Setup

1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Create a new application → Bot → Copy token
3. Enable these **Privileged Gateway Intents**:
   - Server Members Intent
   - Message Content Intent
4. Invite the bot with `applications.commands` + `bot` scopes
5. Required bot permissions: `Manage Roles`, `Send Messages`, `Manage Nicknames`

---

## ⚙️ Configuration

CordSync uses a modular configuration system:

| File | Description |
|------|-------------|
| `config.yml` | Main configuration |
| `locales/en.yml` | English messages (default) |
| `locales/tr.yml` | Turkish messages |
| `locales/de.yml` | German messages |
| `locales/es.yml` | Spanish messages |
| `locales/fr.yml` | French messages |

> **Note:** CordSync automatically adds new config keys when you update — your existing settings are preserved!

---

## 💻 Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/link` | `cordsync.use` | Generate a linking code |
| `/unlink` | `cordsync.use` | Remove your Discord link |
| `/csreload` | `cordsync.admin` | Reload configuration |
| `/csinfo` | `cordsync.admin` | Show plugin info |
| `/csreverify` | `cordsync.admin` | Run re-verification |

---

## 🔌 API

```java
// Get the CordSync API
CordSyncAPI api = CordSync.getInstance().getApi();

// Check if a player is linked
boolean linked = api.isLinked(playerUUID);

// Get linked Discord ID
String discordId = api.getDiscordId(playerUUID);
```

### Events
```java
@EventHandler
public void onLink(PlayerLinkedEvent event) {
    UUID uuid = event.getPlayerUUID();
    String discordId = event.getDiscordId();
}
```

---

## 🏗️ Building from Source

See [BUILDING.md](BUILDING.md) for detailed instructions.

```bash
git clone https://github.com/musbabaff/CordSync.git
cd CordSync
mvn clean package
```

---

## 📄 License

This project is licensed under a [Custom EULA (All Rights Reserved)](LICENSE). Source code is available for educational and personal use only.

---

<div align="center">

**Made with ❤️ by [musbabaff](https://github.com/musbabaff)**

⭐ Star this project if you find it useful!

</div>