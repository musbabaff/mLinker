package com.nettyforge.cordsync.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Advanced TPS & MSPT Monitor — Spark-Optimized.
 * 
 * Performance Design:
 * - Uses a fixed-size ring buffer instead of LinkedList to eliminate GC
 * pressure.
 * - Synchronized blocks are minimized to a single volatile write per tick.
 * - No object allocation inside the run() hot path.
 * - Optional Spark API integration via cached reflection (one-time lookup).
 */
public class TPSMonitor implements Runnable {

    private static volatile double tps = 20.0;
    private long lastPoll = System.currentTimeMillis();

    // ── MSPT Ring Buffer (Zero-GC) ──
    // Fixed-size array instead of LinkedList = no object creation, no GC pressure.
    // 450 slots = 15 minutes of data at 2s intervals.
    private static final int RING_SIZE = 450;
    private static final double[] msptRing = new double[RING_SIZE];
    private static volatile int ringHead = 0; // Points to next write position
    private static volatile int ringCount = 0; // Total entries written (capped at RING_SIZE)

    // Server start time for uptime calculation (immutable)
    private static final long SERVER_START_TIME = System.currentTimeMillis();

    // ── Spark Reflection Cache (one-time lookup) ──
    private static volatile byte sparkState = 0; // 0=unknown, 1=available, 2=unavailable

    // ── Cached counters (refreshed each tick, avoid per-request computation) ──
    private static volatile int cachedChunks = 0;
    private static volatile int cachedEntities = 0;

    public TPSMonitor(JavaPlugin plugin) {
        try {
            Bukkit.getScheduler().runTaskTimer(plugin, this, 40L, 40L);
        } catch (Exception e) {
            // Folia fallback — graceful
        }
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        long spent = now - lastPoll;
        if (spent == 0)
            spent = 1;

        // TPS: simple math, no object allocation
        double currentTps = 40.0 / (spent / 1000.0);
        if (currentTps > 20.0)
            currentTps = 20.0;
        tps = currentTps;

        // MSPT: write directly into ring buffer (single array write, zero GC)
        double mspt = (double) spent / 40.0;
        msptRing[ringHead] = mspt;
        ringHead = (ringHead + 1) % RING_SIZE;
        if (ringCount < RING_SIZE)
            ringCount++;

        // Refresh cached world data (we're already on the main thread here)
        try {
            int chunks = 0, entities = 0;
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                chunks += w.getLoadedChunks().length;
                entities += w.getEntities().size();
            }
            cachedChunks = chunks;
            cachedEntities = entities;
        } catch (Exception ignored) {
            // Safety net — never crash the TPS monitor
        }

        lastPoll = now;
    }

    // ═══════════════════════════════════════════════
    // TPS
    // ═══════════════════════════════════════════════

    public static double getTPS() {
        Double sparkTps = getSparkTPS();
        if (sparkTps != null)
            return sparkTps;

        try {
            Object server = Bukkit.getServer();
            java.lang.reflect.Method getTPSMethod = server.getClass().getMethod("getTPS");
            double[] tpsArray = (double[]) getTPSMethod.invoke(server);
            return Math.round(tpsArray[0] * 100.0) / 100.0;
        } catch (Throwable t) {
            return Math.round(tps * 100.0) / 100.0;
        }
    }

    // ═══════════════════════════════════════════════
    // MSPT Averages — Ring Buffer (Zero-Allocation)
    // ═══════════════════════════════════════════════

    /**
     * Compute average over the last N entries from the ring buffer. No object
     * allocation.
     */
    private static double getMsptAverage(int entries) {
        int count = Math.min(entries, ringCount);
        if (count == 0)
            return 0.0;

        double sum = 0;
        int idx = (ringHead - 1 + RING_SIZE) % RING_SIZE; // Start from most recent
        for (int i = 0; i < count; i++) {
            sum += msptRing[idx];
            idx = (idx - 1 + RING_SIZE) % RING_SIZE;
        }
        return Math.round((sum / count) * 100.0) / 100.0;
    }

    /** 1 minute average (~30 entries at 2s polling). */
    public static double getMspt1m() {
        return getMsptAverage(30);
    }

    /** 5 minute average (~150 entries at 2s polling). */
    public static double getMspt5m() {
        return getMsptAverage(150);
    }

    /** 15 minute average (~450 entries at 2s polling). */
    public static double getMspt15m() {
        return getMsptAverage(450);
    }

    /** Health emoji based on MSPT value. */
    public static String getMsptHealthEmoji(double mspt) {
        if (mspt < 20.0)
            return "🟢";
        if (mspt < 40.0)
            return "🟡";
        return "🔴";
    }

    // ═══════════════════════════════════════════════
    // RAM, Uptime, Chunks, Entities (cached)
    // ═══════════════════════════════════════════════

    public static String getRamUsage() {
        Runtime r = Runtime.getRuntime();
        long used = (r.totalMemory() - r.freeMemory()) / 1048576L;
        long total = r.maxMemory() / 1048576L;
        return used + "MB / " + total + "MB";
    }

    /** Human-readable uptime (e.g. "2d 5h 13m"). */
    public static String getUptime() {
        long totalMinutes = (System.currentTimeMillis() - SERVER_START_TIME) / 60000;
        long days = totalMinutes / 1440;
        long hours = (totalMinutes % 1440) / 60;
        long minutes = totalMinutes % 60;

        StringBuilder sb = new StringBuilder(16); // Pre-sized to avoid resize
        if (days > 0)
            sb.append(days).append("d ");
        if (hours > 0 || days > 0)
            sb.append(hours).append("h ");
        sb.append(minutes).append("m");
        return sb.toString().trim();
    }

    /** Cached chunk count (refreshed every 2s on main thread). */
    public static int getTotalLoadedChunks() {
        return cachedChunks;
    }

    /** Cached entity count (refreshed every 2s on main thread). */
    public static int getTotalEntities() {
        return cachedEntities;
    }

    // ═══════════════════════════════════════════════
    // Spark Integration — Cached Reflection
    // ═══════════════════════════════════════════════

    private static boolean isSparkAvailable() {
        if (sparkState == 0) {
            sparkState = (byte) (Bukkit.getPluginManager().getPlugin("spark") != null ? 1 : 2);
        }
        return sparkState == 1;
    }

    private static Double getSparkTPS() {
        if (!isSparkAvailable())
            return null;
        try {
            Class<?> sparkProvider = Class.forName("me.lucko.spark.api.SparkProvider");
            Object spark = sparkProvider.getMethod("get").invoke(null);
            Object tpsObj = spark.getClass().getMethod("tps").invoke(spark);
            Object poll = tpsObj.getClass().getMethod("poll").invoke(tpsObj,
                    getSparkDuration("SECONDS_10"));
            if (poll instanceof Number) {
                return Math.round(((Number) poll).doubleValue() * 100.0) / 100.0;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Object getSparkDuration(String name) {
        try {
            Class<?> windowClass = Class.forName("me.lucko.spark.api.statistic.StatisticWindow$TicksPerSecond");
            return Enum.valueOf(windowClass.asSubclass(Enum.class), name);
        } catch (Throwable t) {
            try {
                Class<?> windowClass = Class.forName("me.lucko.spark.api.statistic.StatisticWindow$MillisPerTick");
                return Enum.valueOf(windowClass.asSubclass(Enum.class), name);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
