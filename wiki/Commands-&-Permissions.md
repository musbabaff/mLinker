# 📋 Commands & Permissions

Complete reference for all CordSync commands, aliases, and permissions.

---

## Player Commands

| Command | Aliases | Description | Permission | Default |
|---|---|---|---|---|
| `/link` | `/hesapesle`, `/cslink` | Start the linking process or open the GUI | `cordsync.use` | ✅ All |
| `/link help` | `/link yardim` | Dynamically list available commands for active modules | `cordsync.use` | ✅ All |
| `/unlink` | `/hesapkaldir`, `/csunlink` | Remove the link between your Minecraft and Discord | `cordsync.use` | ✅ All |
| `/report <player> <reason>` | — | Report a player to Discord admins with interactive buttons | — | ✅ All |
| `/bug <description>` | `/bugreport`, `/hata` | Submit a technical bug report (includes TPS, RAM, coords) | `cordsync.bug` | ✅ All |
| `/ticket create <message>` | `/destek`, `/csticket` | Open a two-way support ticket with Discord staff | `cordsync.ticket` | ✅ All |

---

## Staff Commands

| Command | Aliases | Description | Permission | Default |
|---|---|---|---|---|
| `/staffchat <message>` | `/sc`, `/staffc` | Send a cross-server message to online staff via Discord | `cordsync.staffchat` | ❌ OP |

---

## Admin Commands

| Command | Aliases | Description | Permission | Default |
|---|---|---|---|---|
| `/csreload` | `/cordsyncreload`, `/csrl` | Reload all CordSync configuration files | `cordsync.admin` | ❌ OP |
| `/csinfo` | `/cordsyncinfo`, `/csver` | Show CordSync status, version info, and API details | `cordsync.admin` | ❌ OP |
| `/csreverify` | `/reverify`, `/linkcheck` | Manually start the smart re-verification system | `cordsync.admin` | ❌ OP |

---

## Discord Slash Commands

| Command | Description | Configuration |
|---|---|---|
| `/<configurable>` | Link your Minecraft account with Discord (name set in `config.yml`) | `commands.discord-slash-command` |

---

## Permission Nodes

| Permission | Description | Default |
|---|---|---|
| `cordsync.use` | Account linking and unlinking commands | All players |
| `cordsync.bug` | Submit bug reports via `/bug` | All players |
| `cordsync.ticket` | Open support tickets via `/ticket` | All players |
| `cordsync.chat` | Cross-chat messaging permission | All players |
| `cordsync.staffchat` | Staff chat access | OP only |
| `cordsync.admin` | Full admin access (reload, info, reverify) | OP only |
| `cordsync.rewards` | Access to the reward system | OP only |
| `cordsync.reverify` | Manual re-verification trigger | OP only |
| `cordsync.api` | CordSync API access and management | OP only |

---

## Discord Button Interactions

These are not commands but interactive Discord button clicks handled by the plugin:

| Button | Location | Action |
|---|---|---|
| `✅ Approve` / `🚫 Deny` | 2FA DM | Approve or deny a login request |
| `🔇 Mute` / `🚷 Kick` / `⛔ Ban` | Moderation log | Opens a Modal form for punishment details |
| `📍 Teleport` | Player report embed | Teleports linked staff to the reported player |
| `🔗 Link` | Auto-message embed | Opens the account linking modal |
| `❓ How To` / `📊 Status` | Auto-message embed | Info and linking status buttons |
