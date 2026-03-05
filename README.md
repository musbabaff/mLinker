<div align="center">

# ⚡ CordSync — by NettyForge Studios

### The Ultimate Discord ↔ Minecraft Synchronization Core

[![Version](https://img.shields.io/badge/version-1.4.0-blue.svg)](https://github.com/musbabaff/CordSync/releases)
[![License](https://img.shields.io/badge/license-Custom%20EULA-red.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.8--1.21+-brightgreen.svg)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://adoptium.net/)
[![Folia](https://img.shields.io/badge/Folia-Supported-purple.svg)](https://papermc.io/software/folia)
[![Spark](https://img.shields.io/badge/Spark-Optimized-ff69b4.svg)](https://spark.lucko.me/)

**The most feature-rich, performance-obsessed Discord-Minecraft bridge ever built.**
**11 modular systems. Zero hardcoded text. Zero impact on TPS.**

[Features](#-features) • [Modules](#-modular-architecture) • [Performance](#-zero-impact-performance) • [Installation](#-quick-start) • [Commands](#-commands) • [Wiki](https://github.com/musbabaff/CordSync/wiki)

---

</div>

## 🌟 Features

### � Account Linking & Verification
- **Slash Command Integration** — `/link` on Discord with configurable command name
- **Interactive GUI** — Beautiful chest menu for in-game linking
- **Auto-Role Assignment** — Automatically grant Discord roles on verification
- **Smart Re-Verification** — Periodically validates that linked accounts still meet requirements

### 🛡️ Advanced 2FA Security
- **Single-Instance Architecture** — Bulletproof login verification with static session maps
- **IP-Based Session Cache** — Approved IPs are remembered, no repeat verification
- **Async Discord DM** — Non-blocking `.queue()` approval requests with ✅/🚫 buttons
- **5-Minute Auto-Expiry** — Pending logins automatically purged after timeout

### 💬 Bi-Directional Chat Bridge
- **Webhook Chat** — Player messages appear in Discord with their 3D Minecraft skin as avatar
- **Discord → Minecraft** — Discord messages broadcast in-game with formatted colors
- **Message Sanitization** — Prevents @everyone/@here ping exploits

### 🖥️ Secure Console Bridge
- **Two-Way Control** — View live server logs and execute commands from Discord
- **Role-Based Security** — Only authorized Discord roles can access the console
- **Command Blacklist** — Block dangerous commands (`/stop`, `/op`) from remote execution

### 🔨 Interactive Modal Moderation
- **Chat Filter** — Configurable forbidden word detection with instant Discord alerts
- **Discord Modal Forms** — Mute/Kick/Ban buttons open pop-up forms for duration & reason
- **Dynamic Commands** — `{player}`, `{duration}`, `{reason}` placeholders in punishment commands
- **Button Disable** — Prevents double-punishment after action is taken

### � Bug Tracker & Report System
- **`/bug <description>`** — Players report technical issues with auto-captured TPS, RAM, coordinates
- **`/report <player> <reason>`** — Player reports with **� Teleport** button for staff
- **Cooldown System** — Configurable spam prevention (default: 5min for bugs, 1min for reports)
- **Discord ID Linking** — Shows reporter's linked Discord account in the embed

### � MSPT & Live Performance Monitor
- **MSPT Tracking** — 1-minute, 5-minute, and 15-minute rolling averages
- **Health Indicators** — 🟢 `<20ms` | 🟡 `20-40ms` | 🔴 `>45ms`
- **Chunks & Entities** — Real-time loaded chunk and entity count
- **Server Uptime** — Human-readable `2d 5h 13m` format
- **Spark Integration** — Auto-detects Spark plugin for enhanced TPS/MSPT data

### 🏆 Dynamic AJLeaderboards
- **Unlimited Boards** — Define as many leaderboard tabs as you want via config
- **ajLeaderboards API** — Direct reflection-based integration (zero compile dependency)
- **PlaceholderAPI Fallback** — Works with PAPI if ajLeaderboards isn't available
- **Auto Edit-Message** — Boards update themselves on Discord at configurable intervals
- **Medal Formatting** — 🥇🥈🥉🏅 automatic ranking emojis

### 🎫 Two-Way Ticket System
- **`/ticket create <message>`** — Opens a dedicated Discord channel/thread
- **Discord → Minecraft** — Staff replies appear in-game to the ticket creator
- **Server Context** — TPS and player ping included in ticket embeds

### 🎧 Dynamic Voice Channels
- **WorldGuard Integration** — Enter a region → Discord voice channel auto-created
- **Auto Cleanup** — Channel deleted when all players leave the region
- **Category Organization** — Channels created under a configurable Discord category

### � Reverse Sync & Rewards
- **Discord → Minecraft Roles** — LuckPerms groups auto-assigned based on Discord roles
- **Booster Rewards** — Instant in-game perks for Discord Nitro Boosters
- **Voice XP** — Earn rewards for time spent in Discord voice channels

### � PlaceholderAPI Support
- `%cordsync_is_linked%` • `%cordsync_discord_name%` • `%cordsync_discord_role%` • 5+ more tags

---

## � Modular Architecture

Every feature is an independent module that can be toggled in `modules.yml`:

```yaml
modules:
  report-module: true       # 🚨 /report + /bug system
  live-status: true          # 📊 MSPT & Performance Monitor
  security-module: true      # 🛡️ Alt-Account Protection
  economy-module: true       # 💰 Vault & PAPI Economy
  devops-module: true        # ⚙️ TPS Alarms & Console Filter
  rewards-module: true       # 🎁 Voice XP & Activity Rewards
  network-module: true       # 🌐 Cross-Server Staff Chat
  ticket-module: true        # 🎫 Two-Way Ticket System
  moderation-module: true    # 🔨 Interactive Modal Moderation
  leaderboard-module: true   # 🏆 Dynamic AJLeaderboards
  voice-module: true         # 🎧 WorldGuard Voice Channels
```

> **Zero RAM Cost** — Disabled modules are never loaded into memory.

---

## ⚡ Zero-Impact Performance

CordSync is engineered for **1000+ player servers**. Every system is built to be invisible on Spark profiler reports.

| Optimization | Implementation |
|---|---|
| **Zero-GC TPS Monitor** | Fixed-size `double[450]` ring buffer — no `LinkedList`, no object allocation per tick |
| **Cached Config** | All config values read once on `onEnable()` — zero `getString()` per update cycle |
| **Cached World Data** | Chunks & entities counted on main thread tick → stored as `volatile` — zero iteration per request |
| **Pre-Lowercased Chat Filter** | Forbidden words lowercased once on enable — zero `toLowerCase()` per chat event |
| **Async Everything** | All JDA calls use `.queue()` callbacks — zero blocking `.complete()` calls |
| **Zero Zombie Tasks** | `cancelTasks(this)` + `HandlerList.unregisterAll(this)` on disable — clean `/reload` |
| **No `new Thread()`** | Project-wide guarantee: all async work via `BukkitScheduler` or JDA callbacks |
| **Spark API Support** | Auto-detects Spark via reflection for enhanced metrics — zero hard dependency |

```
🔥 Spark Hot Path Analysis:
────────────────────────────────────
TPSMonitor.run()     → 0 allocation/tick   ✓
ChatFilter.onChat()  → 0 allocation/event  ✓
LiveStatus.update()  → 0 config read/cycle ✓
onDisable()          → cancelTasks + unregisterAll ✓
```

---

## 🚀 Quick Start

### Prerequisites
- **Java 17+**
- **Spigot/Paper 1.19+** (Folia compatible)
- **Discord Bot Token** — [Create one here](https://discord.com/developers/applications)

### Installation

1. Download `CordSync-1.4.0.jar` from [Releases](https://github.com/musbabaff/CordSync/releases)
2. Place it in your server's `plugins/` folder
3. Start the server once to generate config files
4. Edit `plugins/CordSync/config.yml` — add your **Bot Token** and **Guild ID**
5. Restart the server

> 📖 **Detailed setup guide**: [wiki/Setup_Guide.md](wiki/Setup_Guide.md)

---

## 📋 Commands

| Command | Description | Permission |
|---|---|---|
| `/link` | Link your Minecraft account with Discord | `cordsync.use` |
| `/unlink` | Remove your Discord account link | `cordsync.use` |
| `/report <player> <reason>` | Report a player to Discord admins | — |
| `/bug <description>` | Submit a bug/technical issue report | `cordsync.bug` |
| `/ticket create <message>` | Open a Discord support ticket | `cordsync.ticket` |
| `/staffchat <message>` | Cross-server staff communication | `cordsync.staffchat` |
| `/csreload` | Reload all CordSync configurations | `cordsync.admin` |
| `/csinfo` | Show plugin status and version info | `cordsync.admin` |
| `/csreverify` | Manually trigger re-verification | `cordsync.admin` |

> 📖 **Full command reference**: [wiki/Commands-&-Permissions.md](wiki/Commands-&-Permissions.md)

---

## � Soft Dependencies

| Plugin | Usage | Required? |
|---|---|---|
| **Vault** | Economy integration for leaderboards | Optional |
| **LuckPerms** | Reverse sync (Discord → MC roles) | Optional |
| **PlaceholderAPI** | Dynamic placeholders & leaderboard fallback | Optional |
| **WorldGuard** | Dynamic voice channel regions | Optional |
| **ajLeaderboards** | Primary leaderboard data source | Optional |
| **Spark** | Enhanced TPS/MSPT metrics | Optional |

---

## � Documentation

| Document | Description |
|---|---|
| [Setup Guide](wiki/Setup_Guide.md) | Step-by-step installation & Discord bot setup |
| [Configuration](wiki/Configuration.md) | All config files explained |
| [Commands & Permissions](wiki/Commands-&-Permissions.md) | Full command reference |
| [Modules](wiki/Modules.md) | Detailed module documentation |

---

## � bStats

CordSync uses [bStats](https://bstats.org/) for anonymous usage statistics. You can opt out in `config.yml`.

---

<div align="center">

**Built with ❤️ by [NettyForge Studios](https://nettyforge.com)**

*CordSync v1.4.0 — The Ultimate Discord-Minecraft Synchronization Core*

</div>