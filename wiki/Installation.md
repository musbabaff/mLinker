# 📦 Installation Guide

Installing **CordSync** is a fast and simple process. Follow the steps below to prepare your Minecraft server and Discord server perfectly.

## 1. Prerequisites
- A Minecraft Server running **1.8.x - 1.21.x** (Paper, Spigot, Purpur, Leaf, or Folia)
- **Java 17** or newer installed.
- (Optional) **LuckPerms** (for reverse-sync rank giving).
- (Optional) **PlaceholderAPI** (for placeholders).
- A Discord Bot Application Token.

## 2. Setting Up the Discord Bot
1. Go to the [Discord Developer Portal](https://discord.com/developers/applications).
2. Click **New Application** and give it a name (e.g., "YourServer Sync").
3. Go to the **Bot** tab on the left menu.
4. Click **Reset Token** and copy the string provided. **(Keep this secret!)**
5. **Important:** Scroll down and enable **ALL Privileged Gateway Intents** (Presence Intent, Server Members Intent, Message Content Intent).
6. Go to **OAuth2 -> URL Generator**.
7. Select scopes: `bot` and `applications.commands`.
8. Select permissions: `Administrator`.
9. Copy the generated URL and invite the bot to your Discord Server.

## 3. Installing the Plugin
1. Download `CordSync-1.4.4.jar` from the GitHub Releases or directly from your build.
2. Place the JAR file in your server's `plugins/` folder.
3. Start or restart your Minecraft server to generate the configuration files.
4. Stop the server.

## 4. Configuration
1. Open `plugins/CordSync/config.yml`.
2. Paste your **Bot Token** into the `bot-token` field.
3. Set your **Guild ID** (Support Server ID).
4. Set the `channel-id` for the main linking channel.
5. (Optional) Set up database details if you are using MySQL. Otherwise, SQLite is enabled automatically.
6. Configure premium features (`console-bridge`, `2fa-login`, `reverse-sync`).
7. Save the file and restart your server.

**You're done!** Players can now type `/link` in-game and begin syncing their accounts. Check the **Configuration** page for advanced settings.
