package ru.refontstudio.restcooldownneo.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.Iterator;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardUtils {
    public WorldGuardUtils() {
    }

    public static boolean isPvpAllowed(Player player) {
        Location location = player.getLocation();
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager != null) {
            ApplicableRegionSet set = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
            Iterator var4 = set.iterator();

            while(var4.hasNext()) {
                ProtectedRegion region = (ProtectedRegion)var4.next();
                if (region.getFlag(Flags.PVP) == State.DENY) {
                    return false;
                }
            }
        }

        return true;
    }
}