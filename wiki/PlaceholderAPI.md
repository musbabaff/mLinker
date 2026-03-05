# 🔌 PlaceholderAPI Integration

CordSync provides extensive Support for PlaceholderAPI (PAPI). These placeholders can be used in your tablist, scoreboard, chat formats, or any other plugin that supports PAPI.

---

## 📋 Available Placeholders

All placeholders start with `%cordsync_...%`.

| Placeholder | Description | Example Output |
|---|---|---|
| `%cordsync_is_linked%` | Returns "Linked" or "Unlinked" (configurable in strings) | `Linked` |
| `%cordsync_is_linked_bool%` | Returns true or false | `true` |
| `%cordsync_discord_name%` | Returns the player's Discord username | `musbabaff` |
| `%cordsync_discord_id%` | Returns the player's Discord ID | `123456789012345678` |
| `%cordsync_discord_role%` | Returns the player's highest Discord role name | `Admin` |
| `%cordsync_discord_role_hex%` | Returns the player's highest Discord role color as a hex code | `#ff0000` |
| `%cordsync_relink_count%` | The number of times the player has unlinked & relinked | `2` |
| `%cordsync_has_reward%` | Check if they claimed the first-link reward (true/false) | `true` |
| `%cordsync_voice_time%` | Total time spent in CordSync voice channels | `1d 4h 32m` |

---

## 🏆 Leaderboard Integration

If you enabled the **Leaderboard Module**, CordSync can use `ajLeaderboards` placeholders, or standard PAPI placeholders for any statistic you configure in `modules/leaderboard/config.yml`.

Example placeholders provided by `ajLeaderboards`:
- `%ajlb_lb_statistic_player_kills_1_name%` → Returns the name of the #1 killer
- `%ajlb_lb_statistic_player_kills_1_value%` → Returns the value of the #1 killer

*CordSync's Leaderboard Module automatically parses these placeholders to generate beautiful Discord embeds.*

---

## ⚙️ Configuration

You can customize the text returned by the `%cordsync_is_linked%` placeholder in the language files (e.g. `messages_en.yml`):

```yaml
placeholders:
  linked: "&aLinked"
  unlinked: "&cUnlinked"
  none: "&7None"
```

## 🛠️ Requirements
To use these placeholders, you must install:
1. [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
2. Restart the server (CordSync automatically registers itself with PAPI on startup).
