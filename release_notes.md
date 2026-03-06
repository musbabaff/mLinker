# 🚀 CordSync v1.4.4 — Code Quality Polish

Minor code quality improvement — removed an unnecessary `@SuppressWarnings("unchecked")` annotation from `TPSMonitor.java` flagged by the IDE.

---

# 🚀 CordSync v1.4.3 — Critical Locale Structure Fix

This hotfix resolves the **Missing message key: discord.log-linked** error that appeared in Discord log embeds after account linking.

### 🐛 Root Cause & Fix
- **YAML Key Misplacement Fixed:** The discord bot keys (`log-linked`, `log-unlinked`, `disabled`, `started`, `bot-stopped`, `failed`, `auto-message-sent`, `unlinked`, `role-remove-error`, `role-fail`, `luckperms-role-removed`) were incorrectly nested under the `commands:` section instead of `discord:` in all 10 language files.
- **All 10 Locales Corrected:** Moved 12 keys from `commands:` to `discord:` across `en`, `tr`, `de`, `es`, `fr`, `ru`, `pl`, `az`, `zh`, `ja`.
- **New Key Added:** `discord.verified-console` added to all locales.

---

# 🚀 CordSync v1.4.2 — 2FA & Smart Reverify Hotfix

This patch introduces highly requested security features and rectifies Discord bridging bugs.

### 🛡️ 2FA Security & Reverify
- **Player-Controlled 2FA Toggle:** Players can now actively toggle Two-Factor Authentication via an interactive 🟥 **Toggle 2FA** button directly from the Discord link embed.
- **Smart Reverify Localization:** Added the missing `reverify.start` translation key across all 10 language dictionaries, securing global readability.

### 🛠️ Bridge & Config Fixes
- **Join/Quit Sync Fixed:** Completely rewrote the Join/Quit listeners. Messages correctly map to your dedicated `discord.join-quit-messages.channel-id` variable.
- **Console Bridge Hardening:** Wrapped the console bridge BukkitScheduler in a try-catch block, preventing the logger loop from permanently crashing on single-message exceptions.
- **Config & Command Cleans:** Resolved corrupted charset issues in `/csinfo` and removed dummy `gui` string requirements from `config.yml`.

---

# 🚀 CordSync v1.4.1 — Localization & Polish Update

This patch focuses on expanding the plugin's global reach, standardizing configuration files, and addressing critical bridge bugs.

### 🌐 Global Localization Sync
- Added 5 brand-new languages: `ru.yml` (Russian), `pl.yml` (Polish), `az.yml` (Azerbaijani), `zh.yml` (Chinese), and `ja.yml` (Japanese).
- Synchronized all existing languages (`tr`, `de`, `es`, `fr`) to perfectly match the massive 229-key structure of `en.yml`.
- Replaced hardcoded Turkish logs and missing keys (e.g. `discord.unlinked`) with dynamic translation variables.

### 🛠️ Config & Bridge Fixes
- **ASCII Art Update:** Repainted the `config.yml` header logo from the deprecated CORDSYN to the modern CORDSYNC design.
- **Join/Quit Sync:** Repaired the logic and configuration paths that prevented player join/quit events from displaying in Discord.
- **Chat Bridge:** Fixed bidirectional chat relay permission/webhook logic. Discord <-> Game communication now flows flawlessly.
- **Reward Clean-up:** Removed the legacy `items` node from linking rewards; rewards are now purely command-driven (e.g. `eco give`).
- **Clean Console:** Suppressed JDA's internal INFO-level WebSocket logs to prevent console spam.
- **IDE Cleanups:** Removed unused imports (like `TextChannel` in `ChatBridgeListener`).

---

# 🚀 CordSync v1.4.0 — The Ultimate Performance Update

The long-awaited **v1.4.0** is here! This is the most massive update since the creation of CordSync, focusing on **Zero-Impact Performance**, **brand-new premium modules**, and **dynamic integrations**.

CordSync now runs completely invisibly on Spark profiler reports, even on 1000+ player networks, while offering features you'd normally need 4 different discord plugins to achieve.

---

## 🌟 What's New?

### 1. 🚨 Advanced Bug & Report System
- Rebuilt from the ground up! Added `/bug <description>` for technical reports.
- **Discord Teleportation:** Discord report embeds now feature a real-time `[📍 Teleport]` button. A linked staff member clicking this button on Discord will be instantly teleported to the reported player in-game!
- Auto-captures TPS, RAM usage, dimensions, and XYZ coordinates in every bug report.
- Configurable spam-prevention cooldowns.

### 2. 📊 Live Server Status & MSPT Monitor (Spark Optimized)
- The live status auto-updating embed is completely rewritten.
- Now tracks **MSPT (Milliseconds Per Tick)** with 1m, 5m, and 15m rolling averages.
- Includes a traffic light emoji system based on server health (🟢 / 🟡 / 🔴).
- Displays loaded chunks & entities alongside `2d 4h 13m` formatted uptime.
- *Zero-Dependency Spark Integration:* Automatically detects Spark API via reflection for enhanced metrics.

### 3. 🏆 Dynamic AJLeaderboards Integration
- Say goodbye to hardcoded "vault or kills" options.
- The leaderboard module now supports **unlimited boards** via the config file.
- Automatically connects to `ajLeaderboards` API or fallback to `PlaceholderAPI`.
- Every leaderboard has its own independent async ticker and separate Discord message.
- Top 3 players get automatic 🥇 🥈 🥉 medal formatting.

### 4. 🔨 Interactive Modal Moderation
- Instead of instantly punishing players via simple buttons, staff log embeds now spawn Discord **Modal Forms** (popups).
- Moderators can type custom durations (e.g., `3d`, `1h`) and custom reasons right from Discord before executing Ban, Kick, or Mute actions!
- Buttons automatically disable after an action is taken to prevent double-punishments.

### 5. 🛡️ 2FA Login Dual-Instance Hotfix
- Fixed a devastating bug where players would click "Approve" but be rejected due to Bukkit and JDA keeping separate memory instances of the `pendingLogins` cache.
- Hardened the `LoginVerifyListener` architecture to a strictly enforced **Single-Instance** model.
- Removed blocking `.complete()` calls across the Discord API to drastically improve main-thread performance.

### 6. 🎫 Two-Way Ticket System
- Added a full in-game `/ticket` system bridging players with Discord support staff.
- Creates Discord threads per ticket with automated responses to the player in-game.

---

## ⚡ Zero-Impact Spark Optimizations

We went through the entire codebase and successfully eliminated **every single source of Garbage Collection (GC) pressure and main-thread blockage**.

- **Ring Buffer TPS Monitor:** Eliminated the `LinkedList` in favor of a fixed `double[450]` array. Result: *Zero object allocations per tick.*
- **No More Zombie Tasks:** Fixed an issue where `/reload` would leave phantom asynchronous tasks alive. CordSync now cleanly executes `cancelTasks()` on shutdown.
- **Aggressive Config Caching:** The Live Status embed no longer reads configuration files or iterates through Bukkit Worlds every 30 seconds. Data is read once on startup and updated via cached volatile variables.
- **Pre-Compiled Chat Filters:** Moderation forbidden words are now cached and pre-lowercased during startup — removing the memory hit on `AsyncPlayerChatEvent`.
- **Zero `new Thread()` Policy:** Strict enforcement across the plugin to ensure BukkitScheduler or JDA asynchronous queues handle all processing.

## 🧹 Removed Features
- **Auction Module Removed:** As per server owner requests, the native auction broadcaster has been retired to keep the codebase lean and free of unnecessary bloat.
