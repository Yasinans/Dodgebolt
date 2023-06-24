package org.nightazure.dodgebolt.minigame.utils;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class EntityUtil {
    public static void removeEntities(Location pointA, Location pointB) {
        int minX = Math.min(pointA.getBlockX(), pointB.getBlockX());
        int minY = Math.min(pointA.getBlockY(), pointB.getBlockY());
        int minZ = Math.min(pointA.getBlockZ(), pointB.getBlockZ());
        int maxX = Math.max(pointA.getBlockX(), pointB.getBlockX());
        int maxY = Math.max(pointA.getBlockY(), pointB.getBlockY());
        int maxZ = Math.max(pointA.getBlockZ(), pointB.getBlockZ());

        for (Entity entity : pointA.getWorld().getEntities()) {
            if (entity instanceof Player) {
                continue; // Skip players
            }

            Location entityLocation = entity.getLocation();
            int entityX = entityLocation.getBlockX();
            int entityY = entityLocation.getBlockY();
            int entityZ = entityLocation.getBlockZ();

            if (entityX >= minX && entityX <= maxX
                    && entityY >= minY && entityY <= maxY
                    && entityZ >= minZ && entityZ <= maxZ) {
                entity.remove();
            }
        }
    }
}
