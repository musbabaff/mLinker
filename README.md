<div align="center">

# ⚡ CordSync

### Advanced Discord ↔ Minecraft Account Linking System

[![Version](https://img.shields.io/github/v/release/musbabaff/CordSync?color=blue&label=version)](https://github.com/musbabaff/CordSync/releases)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.8--1.21+-brightgreen.svg)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://adoptium.net/)
[![Folia](https://img.shields.io/badge/Folia-Supported-purple.svg)](https://papermc.io/software/folia)

**The most feature-rich Discord account linking plugin for Minecraft servers.**

[Features](#-features) • [Installation](#-installation) • [Configuration](#-configuration) • [Commands](#-commands) • [API](#-api) • [Wiki](https://github.com/musbabaff/CordSync/wiki)

---

</div>

## 🌟 Features

### 🔗 Smart Account Linking
- **Modal-Based Linking** — Users click a button → fill a form → confirm with 2FA
- **No slash commands needed** — Clean, button-driven experience
- **2FA Login Protection** — IP verification + Discord approval on server join
- **Anti-Abuse System** — Cooldowns, relink limits, and one-time rewards

### 💬 Chat Bridge
- **Bidirectional messaging** — MC ↔ Discord chat sync
- **Configurable access** — ALL users or LINKED_ONLY
- **Message sanitization** — Prevents @everyone/@here exploits

### 🎮 Discord Integration
- **Rotating bot status** — Dynamic status with player count, linked accounts
- **Premium embeds** — Rich, styled log messages for all events
- **Role sync** — LuckPerms group → Discord role mapping
- **Nickname sync** — Automatically update Discord nicknames
- **Booster rewards** — Special rewards for server boosters

### 🛡️ Security
- **2FA Login System** — Verify via Discord before entering the server
- **IP verification** — Detect unknown IPs and require Discord confirmation
- **Unlink cooldown** — Prevent reward farming
- **Relink limits** — Maximum re-linking attempts
- **First reward protection** — One-time only first-link rewards

### 🎁 Reward System
- **First-link rewards** — Commands + items on first link
- **Periodic rewards** — Daily/hourly rewards for linked players
- **Booster rewards** — Extra rewards for Discord boosters
- **Reward logging** — Full audit trail

### 🌍 Multi-Language Support
- **5 built-in languages** — English, Turkish, German, Spanish, French
- **Custom translations** — Create your own `locales/xx.yml`
- **English fallback** — Missing keys automatically fall back to English

### ⚡ Performance & Compatibility
- **Folia support** — Compatible with Folia's regionalized threading
- **Multi-version** — Works on Minecraft 1.8 through 1.21+
- **Proxy support** — BungeeCord & Velocity compatible
- **Multiple storage backends** — SQLite, MySQL, YAML
- **Async operations** — No server lag from database/Discord calls

### 🔧 Developer-Friendly
- **Config auto-migration** — Updates configs without losing your settings
- **Comprehensive API** — Hook into CordSync from your plugins
- **Event system** — `PlayerLinkedEvent` / `PlayerUnlinkedEvent`

---

## 📦 Installation

1. Download the latest release from [Releases](https://github.com/musbabaff/CordSync/releases)
2. Place `CordSync-x.x.x.jar` in your server's `plugins/` folder
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

This project is licensed under the [MIT License](LICENSE).

---

<div align="center">

**Made with ❤️ by [musbabaff](https://github.com/musbabaff)**

⭐ Star this project if you find it useful!

</div>