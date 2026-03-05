<div align="center">

# ⚡ CordSync Master Wiki
**Welcome to the next generation of Minecraft tools & integrations.**

*Crafted with passion by **musbabaff (NettyForge Studios)***

[![Wiki Status](https://img.shields.io/badge/Wiki-Updated-brightgreen.svg)]()
[![Modules](https://img.shields.io/badge/Modules-8_Active-blue.svg)]()

</div>

---

## 👋 Introduction

Welcome to **CordSync**! Designed by **NettyForge Studios**, CordSync is taking the synchronization between your Minecraft Server and your Discord Community to the absolute limits.

Built on an incredibly robust, **100% Asynchronous & Thread-Safe Modular Architecture**, CordSync allows you to toggle heavyweight community features dynamically at **Zero RAM Cost** when disabled. Whether you want to hook Vault directly into Discord Slash Commands, reward your players for chilling in Voice Channels, or establish a Cross-Server Staff Chat without Redis, CordSync is your definitive bridging solution.

This wiki details all **8 Premium Modules** and their configuration guidelines.

---

## 🚨 1. Report Module
A fully interactive reporting ecosystem linking in-game reports directly to your Discord moderation channels.

| Command / Trigger | Permission | Description |
| :--- | :--- | :--- |
| `/report <player> <reason>` | `cordsync.report` | Allows a player to file an interactive report. |
| *Admin Display* | `cordsync.report.receive` | See the report notification inside the game. |

**Key Configuration (`modules/report/config.yml`):**
- `report-channel-id`: The Discord channel ID where embeds are dispatched.
- `cooldown-seconds`: Built-in spam prevention (default: 60s).

---

## 📊 2. Live Status Module
Track your server's health, memory spikes, and player fluctuations dynamically inside Discord.

| Feature Details | Requirements | Description |
| :--- | :--- | :--- |
| **Real-Time Embeds** | Valid Discord Token | Updates an existing message ID continuously. |
| **Color Code Status** | None | Changes embed colors (🟢🟩🟨🟥) automatically reacting to server TPS load. |

**Key Configuration (`modules/livestatus/config.yml`):**
- `message-id`: Once you send a dummy message, paste its ID here and the bot will take over.
- `update-interval`: How frequently to push updates to Discord (default: 30s).

---

## 🔒 3. Security Module
Your absolute shield against alt-accounts, ban evasion, and disconnected communities. 

| Verification Stage | Permission | Description |
| :--- | :--- | :--- |
| **Forced Linking** | `cordsync.bypass.force` | Kicks players pre-login if their MC account is not linked. |
| **Alt-Account Shield** | `cordsync.bypass.alt` | Blocks Discord accounts created less than X days ago. |
| **Discord Leave Sync** | -- | If a user leaves your Discord Guild, their MC account is automatically unlinked and kicked from the server. |

**Key Configuration (`modules/security/config.yml`):**
- `require-link-to-play`: Master toggle for blocking unlinked entries.
- `min-discord-age-days`: Rejects Discord accounts newer than this age (great for ban bypassers).

---

## 💰 4. Economy Module (Vault/PAPI)
Interact with the Minecraft Economy directly from your Discord Client using registered Slash Commands! *(Soft-Depends on Vault & PlaceholderAPI)*.

| Discord Slash Command | Discord Permission | Description |
| :--- | :--- | :--- |
| `/stats <player>` | *Everyone* | Renders a gorgeous embed containing live PlaceholderAPI parsed data. |
| `/bal <player>` | *Everyone* | Checks the Vault balance of an active player. |
| `/eco give <p> <amount>` | **Admin Role ID** | Grants money magically from Discord directly into the player's game wallet! |

**Key Configuration (`modules/economy/config.yml`):**
- `admin-role-id`: Critical security check to see who can use `/eco give`.
- `stats-card-format`: A customizable string array mapping `%papi_tags%` into the Discord Embed!

---

## ⚙️ 5. DevOps Module
The developer's best friend. Track backend errors and control server crashing events before they happen.

| Automation Feature | Security Bypass | Description |
| :--- | :--- | :--- |
| **TPS Watchdog** | *Async Cooldowns* | Pings Discord Admins internally if TPS slips below critical marks. |
| **Smart Console Filter** | Anti-Loop Guard | Captures core backend errors (`WARN`/`SEVERE`/`Exceptions`) and maps them to Discord cleanly. Filters out JDA OkHttp noise. |
| **Auto-Broadcaster** | -- | Synchronized messaging array blasting over Game Chat and Discord simultaneously! |

**Key Configuration (`modules/devops/config.yml`):**
- `dev-channel-id`: Private channel where backend console errors are securely logged.
- `min-tps-alarm`: Warning threshold that triggers when the TPS Watchdog hits its limit (default: 17.5).

---

## 🎁 6. Rewards Module
Gamify your community building! This module measures time spent in Discord Voice Channels and converts it directly into Minecraft loot!

| Gamification | Safety Protocols | Description |
| :--- | :--- | :--- |
| **Voice XP Timer** | `isMuted()`, `isDeafened()` | Computes asynchronous elapsed time in Voice Channels. If users self-mute/deafen, they yield zero rewards! |
| **AFK Evasion** | `getAfkChannel()` | Hard blocks any progression mapping inside registered AFK Voice channels. |

**Key Configuration (`modules/rewards/config.yml`):**
- `reward-interval-minutes`: Consecutive voice minutes required to unlock the drop.
- `reward-commands`: List of Bukkit commands fired by console once the timer successfully hits.
- `reward-message`: Direct Message sent secretly to the player through Discord when they win!

---

## 🌐 7. Network Module (The Global Bridge)
Redefine cross-server networking. Connect all your proxy sub-servers (Skyblock, Lobby, Survival) into one global Staff Chat room using the power of JDA Relays! Absolutely no Redis needed!

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/staffchat <message>` | `cordsync.staffchat` | Broadcasts an inter-dimensional message to all connected servers. |
| *(Alias)* `/sc` | `cordsync.staffchat` | Same functionality, faster typing. |

**Key Configuration (`modules/network/config.yml`):**
- `server-name`: A unique tag (e.g. `[Skyblock]`) applied to outbounding messages so others know where the message came from.
- `discord-staff-channel-id`: The core hidden Discord channel acting as the "Bridge" connecting the Network. Messages sent here are synchronized identically across the servers playing as listeners (`MessageReceivedEvent`).

---

## 🎫 8. SyncTicket Module (2-Way Ticket System)
The ultimate support pipeline. Let your players communicate with your moderation team seamlessly from the game directly into a dedicated Discord Thread/Channel, and vice-versa!

| Command / Interaction | Permission | Description |
| :--- | :--- | :--- |
| `/ticket create <message>` | `cordsync.ticket` | Opens a private ticket. Creates a personalized Discord Channel securely tracking the player's TPS and Ping. |
| `/ticket close` | `cordsync.ticket` | Closes the player's active ticket. |
| *(Discord)* **Close Button** | *Channel Perms* | Staff can click the `Button.danger()` attached to the ticket embed to automatically delete the channel and notify the player in-game. |
| *(Discord)* **Staff Reply** | *Channel Perms* | Any normal text sent by a Staff member inside the Ticket channel is asynchronously relayed back to the player in-game! |

**Key Configuration (`modules/ticket/config.yml`):**
- `ticket-category-id`: The Discord Category ID where all new tickets will be generated.
- `messages.discord-reply`: The customizable string format determining how Staff responses look in Minecraft.

---

## 🔨 9. Moderation Module (Interactive Discord Moderation)
Control your server's chat toxicity and execute punishments via Discord **Modal Forms** (Pop-up Dialogs)!

| Interaction Flow | Component | Description |
| :--- | :--- | :--- |
| **1. Chat Filtering** | `AsyncPlayerChatEvent` | Cancels any message containing forbidden strings and sends an alert embed to Discord with 3 action buttons. |
| **2. Button Click** | `ButtonInteractionEvent` | Clicking `[🔇 Mute]`, `[🚷 Kick]` or `[⛔ Ban]` does NOT execute instantly. Instead, a **Modal Pop-up Form** opens on the staff member's screen. |
| **3. Modal Form** | `Modal` + `TextInput` | **Mute/Ban:** The form includes a **Duration** field (e.g. `1h`, `3d`, `permanent`) and a **Reason** field (required, paragraph). **Kick:** Only the **Reason** field appears. |
| **4. Punishment** | `ModalInteractionEvent` | On submit, the plugin dynamically builds the punishment command using `{player}`, `{duration}`, `{reason}` placeholders and dispatches it on the Bukkit main thread. Buttons are then disabled to prevent double-punishment. |

**Key Configuration (`modules/moderation/config.yml`):**
- `log-channel-id`: The channel where flagged chats trigger interactive Action Buttons.
- `forbidden-words`: An array of banned words that trigger the Watchdog.
- `mute-command`: Template with `{player}`, `{duration}`, `{reason}` placeholders (e.g. `mute {player} {duration} {reason}`).
- `kick-command`: Template with `{player}`, `{reason}` placeholders.
- `ban-command`: Template with `{player}`, `{duration}`, `{reason}` placeholders.

---

## 🏆 10. Leaderboard Module
Gamify your server ranking natively in Discord. 

| Supported Stats | Mechanism | Description |
| :--- | :--- | :--- |
| **Vault Economy** | `OfflinePlayer` Iterator | Loops through all Offline Players, fetches their Vault Balances asynchronously, and sorts the top 10 into an Embed. |
| **Stat: Kills** | `Statistic.PLAYER_KILLS` | Ranks the server's top 10 PVP players automatically. |

**Key Configuration (`modules/leaderboard/config.yml`):**
- `leaderboard-message-id`: The static message ID that the module will continuously edit with the latest Top 10 data. 
- `leaderboard-type`: Set to `VAULT` or `KILLS`.

---

## 🛒 11. Auction Module
Hook your in-game markets straight to your Discord community seamlessly. It provides a raw API command for integration with standard AuctionHouse plugins.

| Command / Hook | Permission | Description |
| :--- | :--- | :--- |
| `/nettyforge ahbroadcast <player> <price> <item>` | `cordsync.admin` | Secure command intended to be run by Console via third-party Auction Plugin execution triggers. It dispatches a beautiful "New Auction Listed!" Embed directly to Discord. |

**Key Configuration (`modules/auction/config.yml`):**
- `auction-channel-id`: The dedicated Discord channel tracking live market activity.
- `embed-color`: Specify the HEX color for the broadcast cards!

---

## 🎧 12. Voice Module (Dynamic WorldGuard Integration)
Merge the realms of Minecraft and Discord Voice dynamically *(Soft-Depends on WorldGuard 7+)*.

| Event Flow | Abstraction Level | Description |
| :--- | :--- | :--- |
| **Region Enter** | `PlayerMoveEvent` | When a player enters a monitored WorldGuard Region boundary (e.g. `dungeon_1`), the Voice Module detects the abstract Region ID and creates a temporary JDA Voice Channel named `🔊 dungeon_1`. |
| **Region Leave** | `PlayerQuit` & `Move` | Once the last occupant leaves the applicable WorldGuard Region, the module automatically destroys the temporary Discord Voice Channel to save space. |

**Key Configuration (`modules/voice/config.yml`):**
- `voice-category-id`: The container Category where all dynamic channels will spawn.
- `watched-regions`: A list of all WorldGuard regions that trigger this event (e.g. `['pvp_arena', 'dungeon_boss']`).

---

<div align="center">

> "Bridging dimensions, one message at a time." 🚀<br>
> **Crafted with passion by musbabaff (NettyForge Studios)**<br>
> Building the next generation of Minecraft tools & integrations.
> 
> [www.nettyforge.com](https://nettyforge.com)

</div>
