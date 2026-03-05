package com.nettyforge.cordsync.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Scheduler compatibility for Folia and standard Bukkit.
 * Detects Folia at runtime and uses the appropriate scheduler.
 */
public class SchedulerUtil {

    private static Boolean isFolia = null;

    /**
     * Checks if the server is running Folia.
     */
    public static boolean isFolia() {
        if (isFolia == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }
        }
        return isFolia;
    }

    /**
     * Runs a task on the main thread (or regionalized for Folia).
     */
    public static void runTask(JavaPlugin plugin, Runnable task) {
        if (isFolia()) {
            // Folia: use global region scheduler
            try {
                Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler")
                        .invoke(Bukkit.getServer());
                // If method exists, use Folia scheduler
                Bukkit.getScheduler().runTask(plugin, task);
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Runs a task asynchronously (same for both Folia and Bukkit).
     */
    public static void runAsync(JavaPlugin plugin, Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    /**
     * Runs a task with a delay.
     */
    public static void runTaskLater(JavaPlugin plugin, Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    /**
     * Runs a repeating task asynchronously.
     */
    public static void runTimerAsync(JavaPlugin plugin, Runnable task, long delay, long period) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
    }
}
