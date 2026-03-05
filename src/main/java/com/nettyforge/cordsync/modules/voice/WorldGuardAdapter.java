package com.nettyforge.cordsync.modules.voice;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.entity.Player;

public class WorldGuardAdapter {

    // Safely abstracting the WorldGuard 7+ API logic to prevent
    // ClassNotFoundExceptions if the module is disabled
    public static String getRegionIdAt(Player player) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
            if (regions == null)
                return null;

            BlockVector3 position = BlockVector3.at(player.getLocation().getX(), player.getLocation().getY(),
                    player.getLocation().getZ());
            ApplicableRegionSet set = regions.getApplicableRegions(position);

            for (ProtectedRegion region : set) {
                // Return the first valid priority region ID we find
                return region.getId();
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // WG not found or failed
        }
        return null;
    }
}
