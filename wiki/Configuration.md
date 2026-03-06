# ⚙️ Configuration Guide

CordSync uses a modular configuration system. The main `config.yml` controls core settings, while each module has its own config file under `plugins/CordSync/modules/<module>/config.yml`.

---

## Core Configuration (`config.yml`)

### Discord Bot Settings

```yaml
discord:
  token: "YOUR_BOT_TOKEN_HERE"        # Bot token from Discord Developer Portal
  guild-id: "YOUR_GUILD_ID"           # Your Discord server ID
  role-id-verified: "ROLE_ID"         # Role given to verified/linked players
  log-channel-id: "CHANNEL_ID"        # Channel for system log embeds

  status:
    rotate: true                       # Enable rotating status messages
    interval: 15                       # Seconds between status rotations
    messages:
      - "PLAYING:{online} Players Online | CordSync"
      - "WATCHING:{linked} Linked Accounts"
```

### Security & 2FA

```yaml
security:
  2fa-login:
    enabled: true                      # Enable 2FA login protection
    session-minutes: 5                 # Session approval duration
    request-timeout-seconds: 300       # 2FA button interaction timeout
    2fa-log-channel-id: "CHANNEL_ID"   # Channel for detailed 2FA event logs
  force-link: false                    # Require linking before play
  alt-protection: true                 # Block alt accounts
```

### Chat Bridge

```yaml
chat-bridge:
  enabled: true
  channel-id: "CHANNEL_ID"            # Discord channel for chat bridge
  discord-to-mc: "ALL"                # ALL or LINKED_ONLY
  minecraft-format: "&7[&bDiscord&7] &f{player}&7: &f{message}"
  ignored-prefixes:
    - "/"
    - "!"
```

### Storage

```yaml
storage:
  type: YAML                           # Options: YAML, SQLITE, MYSQL
  mysql:                               # Only needed if type is MYSQL
    host: "localhost"
    port: 3306
    database: "cordsync"
    username: "root"
    password: "password"
```

---

## Module Configurations

### Enabling/Disabling Modules (`modules.yml`)

```yaml
modules:
  report-module: true       # 🚨 /report + /bug system
  live-status: true          # 📊 MSPT & Performance Monitor
  security-module: true      # 🛡️ Alt-Account Protection
  moderation-module: true    # 🔨 Interactive Modal Moderation
  leaderboard-module: true   # 🏆 Dynamic AJLeaderboards
  voice-module: true         # 🎧 WorldGuard Voice Channels
  ticket-module: true        # 🎫 Two-Way Ticket System
  # ... and more
```

> ⚠️ A server restart is recommended after toggling modules.

---

### 📊 Live Status Module (`modules/livestatus/config.yml`)

```yaml
discord:
  status-channel-id: "CHANNEL_ID"     # Channel containing the status embed
  message-id: "MESSAGE_ID"            # ID of the message to edit
  update-interval: 30                  # Seconds between Discord embed updates
messages:
  embed-title: "📊 Live Server Status"
  embed-color: "#2B2D31"
```

### 🚨 Report & Bug Module (`modules/report/config.yml`)

```yaml
report-channel-id: "CHANNEL_ID"       # Channel for player reports
bug-reports-channel-id: "CHANNEL_ID"  # Channel for bug reports
report-cooldown-seconds: 60           # Cooldown between reports
bug-cooldown-seconds: 300             # Cooldown between bug reports (5 min)
```

### 🔨 Moderation Module (`modules/moderation/config.yml`)

```yaml
log-channel-id: "CHANNEL_ID"          # Channel for moderation alerts
forbidden-words:                       # Words that trigger the chat filter
  - "fuck"
  - "shit"
mute-command: "mute {player} {duration} {reason}"
kick-command: "kick {player} {reason}"
ban-command: "ban {player} {duration} {reason}"
```

### 🏆 Leaderboard Module (`modules/leaderboard/config.yml`)

```yaml
boards:
  top_kills:
    channel-id: "CHANNEL_ID"
    message-id: ""                     # Auto-saved on first send
    title: "⚔️ Top 10 Killers"
    ajlb-board: "statistic_player_kills"
    update-interval: 600               # Seconds (10 minutes)
    embed-color: "#FFD700"
  top_money:
    channel-id: "CHANNEL_ID"
    title: "💸 Top 10 Richest Players"
    ajlb-board: "vault_eco_balance"
    update-interval: 600
    embed-color: "#2ECC71"
```

> 💡 You can add unlimited boards — just add new sections under `boards:`.

### 🎧 Voice Module (`modules/voice/config.yml`)

```yaml
category-id: "CATEGORY_ID"            # Discord category for voice channels
channel-name-format: "🎮 {region}"    # Format for created channel names
regions:                               # WorldGuard regions to monitor
  - "pvp_arena"
  - "spawn"
```

### 🎫 Ticket Module (`modules/ticket/config.yml`)

```yaml
ticket-category-id: "CATEGORY_ID"     # Discord category for ticket channels
ticket-channel-format: "ticket-{player}"
```

---

## Tips

- **Never hardcode text** — All player-facing messages are configurable
- **Channel IDs** — Right-click a channel in Discord → Copy ID (enable Developer Mode in Discord settings)
- **Hot Reload** — Use `/csreload` to reload configs without restarting (module configs require restart)
