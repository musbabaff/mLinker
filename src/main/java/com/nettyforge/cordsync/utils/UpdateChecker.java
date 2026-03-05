package com.nettyforge.cordsync.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import org.bukkit.Bukkit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.nettyforge.cordsync.CordSync;

public class UpdateChecker {

    private final CordSync plugin;
    private final String repoOwner = "musbabaff";
    private final String repoName = "CordSync";
    private final String githubAPI = "https://api.github.com/repos/%s/%s/releases/latest";

    public UpdateChecker(CordSync plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.debug("UpdateChecker: Fetching latest release from GitHub...");
            try {
                String apiUrl = String.format(githubAPI, repoOwner, repoName);
                HttpURLConnection connection = (HttpURLConnection) java.net.URI.create(apiUrl).toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "CordSync-UpdateChecker");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    plugin.getLogger().warning("⚠ GitHub API request failed! Code: " + responseCode);
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(response.toString());

                String latestVersion = (String) json.get("tag_name");
                if (latestVersion == null || latestVersion.isEmpty()) {
                    plugin.getLogger().warning("⚠ No valid version tag found in the GitHub response.");
                    return;
                }

                String currentVersion = plugin.getDescription().getVersion();
                String repoLink = "https://www.spigotmc.org/resources/133118/";

                if (isNewerVersion(latestVersion, currentVersion)) {
                    plugin.setUpdateAvailable(true);
                    plugin.setLatestVersion(latestVersion);

                    plugin.getLogger().info("§e----------------------------------------------------");
                    plugin.getLogger().info("§6CordSync Update Checker");
                    plugin.getLogger().info("§cA new version is available! §f(" + latestVersion + ")");
                    plugin.getLogger().info("§7Current version: §e" + currentVersion);
                    plugin.getLogger().info("§aDownload: " + repoLink);
                    plugin.getLogger().info("§e----------------------------------------------------");

                    // Notify any online operators instantly (useful after /csreload)
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                            if (p.isOp() || p.hasPermission("cordsync.admin")) {
                                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                        "&e----------------------------------------------------"));
                                p.sendMessage(
                                        org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6CordSync Update"));
                                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                        "&cA new version is available! &f(" + latestVersion + ")"));
                                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                        "&aDownload: &e" + repoLink));
                                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                        "&e----------------------------------------------------"));
                            }
                        }
                    });
                } else {
                    plugin.getLogger().info("✅ CordSync is up to date (v" + currentVersion + ")");
                }

            } catch (Exception e) {
                plugin.getLogger().warning("❌ Update check failed: " + e.getMessage());
            }
        });
    }

    private boolean isNewerVersion(String latest, String current) {
        try {
            String l = latest.replaceAll("[^0-9.]", "");
            String c = current.replaceAll("[^0-9.]", "");

            String[] latestParts = l.split("\\.");
            String[] currentParts = c.split("\\.");

            for (int i = 0; i < Math.max(latestParts.length, currentParts.length); i++) {
                int latestNum = (i < latestParts.length) ? Integer.parseInt(latestParts[i]) : 0;
                int currentNum = (i < currentParts.length) ? Integer.parseInt(currentParts[i]) : 0;
                if (latestNum > currentNum)
                    return true;
                if (latestNum < currentNum)
                    return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
