package com.nettyforge.cordsync.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.utils.CodeUtil;
import com.nettyforge.cordsync.utils.MessageUtil;
import com.nettyforge.cordsync.utils.SchedulerUtil;

public class LinkManager {

    private final Map<UUID, String> activeCodes = new HashMap<>();
    private final Map<String, UUID> reverseLookup = new HashMap<>();
    private final Map<UUID, Long> expiryTimes = new HashMap<>();

    private final int expireSeconds;

    public LinkManager(int expireSeconds) {
        this.expireSeconds = expireSeconds;
        startCleanupTask();
    }

    public String generateCode(Player player) {
        if (player == null) return null;

        UUID uuid = player.getUniqueId();

        if (activeCodes.containsKey(uuid)) {
            String oldCode = activeCodes.get(uuid);
            reverseLookup.remove(oldCode);
        }

        String newCode = CodeUtil.generateCode(6);
        activeCodes.put(uuid, newCode);
        reverseLookup.put(newCode, uuid);
        expiryTimes.put(uuid, System.currentTimeMillis() + (expireSeconds * 1000L));

        return newCode;
    }

    public boolean isValidCode(String code) {
        return code != null && reverseLookup.containsKey(code);
    }

    public UUID getPlayerByCode(String code) {
        if (code == null) return null;
        return reverseLookup.get(code);
    }

    public String getCode(Player player) {
        if (player == null) return null;
        return activeCodes.get(player.getUniqueId());
    }

    public void removeCode(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        removeCodeByUUID(uuid);
    }

    public void removeCodeByUUID(UUID uuid) {
        if (uuid == null) return;

        if (activeCodes.containsKey(uuid)) {
            String code = activeCodes.remove(uuid);
            if (code != null) reverseLookup.remove(code);
            expiryTimes.remove(uuid);
        }
    }

    private void startCleanupTask() {
        SchedulerUtil.runAsyncTimer(CordSync.getInstance(), () -> {
            long now = System.currentTimeMillis();
            List<UUID> expired = new ArrayList<>();

            for (Map.Entry<UUID, Long> entry : expiryTimes.entrySet()) {
                if (entry.getValue() <= now) {
                    expired.add(entry.getKey());
                }
            }

            for (UUID id : expired) {
                expiryTimes.remove(id);
                removeCodeByUUID(id);

                OfflinePlayer offline = Bukkit.getOfflinePlayer(id);
                Player player = offline.getPlayer();

                if (player != null && player.isOnline()) {
                    player.sendMessage(MessageUtil.get("link.code-expired"));
                }
            }
        }, 20L * 30L, 20L * 60L);
    }
}
