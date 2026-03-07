package com.nettyforge.cordsync.utils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Universal scheduler abstraction for Folia and standard Bukkit/Paper.
 * <p>
 * On Folia: uses GlobalRegionScheduler and AsyncScheduler via reflection.
 * On Bukkit/Paper: uses the classic BukkitScheduler.
 * <p>
 * All Folia APIs are called via reflection so the plugin compiles
 * cleanly against the Spigot API without any Folia compile-time dependency.
 */
public class SchedulerUtil {

    private static Boolean isFolia = null;

    // Cached reflection objects for Folia (resolved once, reused)
    private static Object globalScheduler = null;
    private static Object asyncScheduler = null;
    private static boolean foliaResolved = false;

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
     * Resolves and caches Folia scheduler instances via reflection.
     */
    private static void resolveFoliaSchedulers() {
        if (foliaResolved) return;
        foliaResolved = true;
        try {
            // Folia: Bukkit.getServer().getGlobalRegionScheduler()
            Method getGlobal = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler");
            globalScheduler = getGlobal.invoke(Bukkit.getServer());

            // Folia: Bukkit.getServer().getAsyncScheduler()
            Method getAsync = Bukkit.getServer().getClass().getMethod("getAsyncScheduler");
            asyncScheduler = getAsync.invoke(Bukkit.getServer());
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CordSync] Failed to resolve Folia schedulers: " + e.getMessage());
            isFolia = false; // Fallback to Bukkit
        }
    }

    // =========================================================================
    //  SYNC TASKS (Main thread / Global region)
    // =========================================================================

    /**
     * Runs a task on the main thread.
     * Folia: GlobalRegionScheduler.run(plugin, task)
     * Bukkit: BukkitScheduler.runTask(plugin, task)
     */
    public static void runSync(JavaPlugin plugin, Runnable task) {
        if (isFolia()) {
            resolveFoliaSchedulers();
            try {
                // GlobalRegionScheduler.run(Plugin, Consumer<ScheduledTask>)
                Method run = globalScheduler.getClass().getMethod("run", Plugin.class,
                        java.util.function.Consumer.class);
                run.invoke(globalScheduler, plugin,
                        (java.util.function.Consumer<?>) scheduledTask -> task.run());
            } catch (Exception e) {
                plugin.getLogger().warning("[Folia] runSync fallback: " + e.getMessage());
                task.run();
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Runs a task on the main thread after a delay.
     * Folia: GlobalRegionScheduler.runDelayed(plugin, task, delayTicks)
     * Bukkit: BukkitScheduler.runTaskLater(plugin, task, delayTicks)
     */
    public static void runSyncLater(JavaPlugin plugin, Runnable task, long delayTicks) {
        if (isFolia()) {
            resolveFoliaSchedulers();
            try {
                Method runDelayed = globalScheduler.getClass().getMethod("runDelayed",
                        Plugin.class, java.util.function.Consumer.class, long.class);
                runDelayed.invoke(globalScheduler, plugin,
                        (java.util.function.Consumer<?>) scheduledTask -> task.run(),
                        Math.max(1L, delayTicks));
            } catch (Exception e) {
                plugin.getLogger().warning("[Folia] runSyncLater fallback: " + e.getMessage());
                task.run();
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Runs a repeating task on the main thread.
     * Folia: GlobalRegionScheduler.runAtFixedRate(plugin, task, initialDelayTicks, periodTicks)
     * Bukkit: BukkitScheduler.runTaskTimer(plugin, task, delay, period)
     */
    public static void runSyncTimer(JavaPlugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            resolveFoliaSchedulers();
            try {
                Method runAtFixedRate = globalScheduler.getClass().getMethod("runAtFixedRate",
                        Plugin.class, java.util.function.Consumer.class, long.class, long.class);
                runAtFixedRate.invoke(globalScheduler, plugin,
                        (java.util.function.Consumer<?>) scheduledTask -> task.run(),
                        Math.max(1L, delayTicks), Math.max(1L, periodTicks));
            } catch (Exception e) {
                plugin.getLogger().warning("[Folia] runSyncTimer fallback: " + e.getMessage());
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    // =========================================================================
    //  ASYNC TASKS
    // =========================================================================

    /**
     * Runs a task asynchronously.
     * Folia: AsyncScheduler.runNow(plugin, task)
     * Bukkit: BukkitScheduler.runTaskAsynchronously(plugin, task)
     */
    public static void runAsync(JavaPlugin plugin, Runnable task) {
        if (isFolia()) {
            resolveFoliaSchedulers();
            try {
                Method runNow = asyncScheduler.getClass().getMethod("runNow",
                        Plugin.class, java.util.function.Consumer.class);
                runNow.invoke(asyncScheduler, plugin,
                        (java.util.function.Consumer<?>) scheduledTask -> task.run());
            } catch (Exception e) {
                plugin.getLogger().warning("[Folia] runAsync fallback: " + e.getMessage());
                task.run();
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    /**
     * Runs a task asynchronously after a delay.
     * Folia: AsyncScheduler.runDelayed(plugin, task, delay, TimeUnit)
     * Bukkit: BukkitScheduler.runTaskLaterAsynchronously(plugin, task, delayTicks)
     */
    public static void runAsyncLater(JavaPlugin plugin, Runnable task, long delayTicks) {
        if (isFolia()) {
            resolveFoliaSchedulers();
            try {
                // Convert ticks to milliseconds (1 tick = 50ms)
                long delayMs = delayTicks * 50L;
                Method runDelayed = asyncScheduler.getClass().getMethod("runDelayed",
                        Plugin.class, java.util.function.Consumer.class, long.class, TimeUnit.class);
                runDelayed.invoke(asyncScheduler, plugin,
                        (java.util.function.Consumer<?>) scheduledTask -> task.run(),
                        delayMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                plugin.getLogger().warning("[Folia] runAsyncLater fallback: " + e.getMessage());
                task.run();
            }
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        }
    }

    /**
     * Runs a repeating task asynchronously.
     * Folia: AsyncScheduler.runAtFixedRate(plugin, task, initialDelay, period, TimeUnit)
     * Bukkit: BukkitScheduler.runTaskTimerAsynchronously(plugin, task, delay, period)
     */
    public static void runAsyncTimer(JavaPlugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            resolveFoliaSchedulers();
            try {
                long delayMs = Math.max(1L, delayTicks * 50L);
                long periodMs = Math.max(1L, periodTicks * 50L);
                Method runAtFixedRate = asyncScheduler.getClass().getMethod("runAtFixedRate",
                        Plugin.class, java.util.function.Consumer.class, long.class, long.class, TimeUnit.class);
                runAtFixedRate.invoke(asyncScheduler, plugin,
                        (java.util.function.Consumer<?>) scheduledTask -> task.run(),
                        delayMs, periodMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                plugin.getLogger().warning("[Folia] runAsyncTimer fallback: " + e.getMessage());
            }
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        }
    }

    // =========================================================================
    //  CANCELLATION
    // =========================================================================

    /**
     * Cancels all tasks for a plugin.
     * Folia: GlobalRegionScheduler.cancelTasks(plugin) + AsyncScheduler.cancelTasks(plugin)
     * Bukkit: BukkitScheduler.cancelTasks(plugin)
     */
    public static void cancelAll(JavaPlugin plugin) {
        if (isFolia()) {
            resolveFoliaSchedulers();
            try {
                if (globalScheduler != null) {
                    Method cancel = globalScheduler.getClass().getMethod("cancelTasks", Plugin.class);
                    cancel.invoke(globalScheduler, plugin);
                }
                if (asyncScheduler != null) {
                    Method cancel = asyncScheduler.getClass().getMethod("cancelTasks", Plugin.class);
                    cancel.invoke(asyncScheduler, plugin);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[Folia] cancelAll fallback: " + e.getMessage());
            }
        } else {
            Bukkit.getScheduler().cancelTasks(plugin);
        }
    }

    // =========================================================================
    //  BACKWARD COMPATIBILITY ALIASES
    // =========================================================================

    /** @deprecated Use {@link #runSync(JavaPlugin, Runnable)} */
    public static void runTask(JavaPlugin plugin, Runnable task) {
        runSync(plugin, task);
    }

    /** @deprecated Use {@link #runSyncLater(JavaPlugin, Runnable, long)} */
    public static void runTaskLater(JavaPlugin plugin, Runnable task, long delayTicks) {
        runSyncLater(plugin, task, delayTicks);
    }

    /** @deprecated Use {@link #runAsyncTimer(JavaPlugin, Runnable, long, long)} */
    public static void runTimerAsync(JavaPlugin plugin, Runnable task, long delay, long period) {
        runAsyncTimer(plugin, task, delay, period);
    }
}
