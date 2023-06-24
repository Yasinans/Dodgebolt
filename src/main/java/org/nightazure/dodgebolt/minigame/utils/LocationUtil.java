package org.nightazure.dodgebolt.minigame.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class LocationUtil {
    public static char token = ':';

    /**
     * Gets the all Locations in the area of pos1 and pos2.
     * @param pos1 The first location of the area.
     * @param pos2 The second location of the area.
     * @return A List<Location> of all block locations in the area.
     */
    public static List<Location> getArea(Location pos1, Location pos2) {
        Location[] sorted = getMinMaxLocations(pos1, pos2);
        pos1 = sorted[0];
        pos2 = sorted[1];
        List<Location> locs = new ArrayList<Location>();
        for (int x = pos1.getBlockX(); x < pos2.getX(); x++) {
            for (int y = pos1.getBlockY(); y < pos2.getY(); y++) {
                for (int z = pos1.getBlockZ(); z < pos2.getZ(); z++) {
                    locs.add(new Location(pos1.getWorld(), x, y, z));
                }
            }
        }
        return locs;
    }

    /**
     * Checks if a location is inside of an area.
     * @param pos1 The first location of the area.
     * @param pos2 The second location of the area.
     * @param loc The location.
     * @return If loc is inside the area of pos1 and pos2.
     */
    public static boolean isInside(Location pointA, Location pointB, Location location) {
        World world = location.getWorld();

        int minX = Math.min(pointA.getBlockX(), pointB.getBlockX());
        int minY = Math.min(pointA.getBlockY(), pointB.getBlockY());
        int minZ = Math.min(pointA.getBlockZ(), pointB.getBlockZ());
        int maxX = Math.max(pointA.getBlockX(), pointB.getBlockX());
        int maxY = Math.max(pointA.getBlockY(), pointB.getBlockY());
        int maxZ = Math.max(pointA.getBlockZ(), pointB.getBlockZ());

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
    /**
     * Gets the smaller and bigger locations of the coordinates.
     * @param pos1 The first location.
     * @param pos2 The second location.
     * @return A Location array holding the smaller and the bigger location.
     */
    public static Location[] getMinMaxLocations(Location pos1, Location pos2) {
        double minX = Math.min(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());

        double maxX = Math.max(pos1.getX(), pos2.getX());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        pos1 = new Location(pos1.getWorld(), minX, minY, minZ);
        pos2 = new Location(pos2.getWorld(), maxX, maxY, maxZ);
        return new Location[] {pos1, pos2};
    }


    /**
     * Serializes a location into a String that contains the world name, x, y and z.
     * @param loc The location that will be serialized.
     * @return A String that contains the world name, x, y and z.
     */
    public static String serialize(Location loc) {
        if (loc == null) return null;
        return loc.getWorld().getName()
                +token+loc.getX()+token+loc.getY()+token+loc.getZ();
    }

    /**
     * Serializes a location into a String that contains the world name, block x, block y and block z.
     * @param loc The location that will be serialized.
     * @return A String that contains the world name, block x, block y and block z.
     */
    public static String serializeSimple(Location loc) {
        if (loc == null) return null;
        return loc.getWorld().getName()+token+
                loc.getBlockX()+token+loc.getBlockY()+token+loc.getBlockZ();
    }

    /**
     * Serializes a location into a String that contains the world name, x, y, z, yaw and pitch.
     * @param loc The location that will be serialized.
     * @return A String that contains the world name, x, y, z, yaw and pitch.
     */
    public static String serializeFully(Location loc) {
        if (loc == null) return null;
        return serialize(loc)+token+loc.getYaw()+token+loc.getPitch();
    }

    /**
     * Deserializes a String that contains the world name, x, y and z into a Location.
     * @param str The string that is deserialized.
     * @return A Location that contains the world, x, y and z.
     */
    public static Location deserialize(String str) {
        if (str == null) return null;
        Location loc = new Location(null, 0, 0, 0);
        String[] split = str.split(String.valueOf(token));
        loc.setWorld(Bukkit.getWorld(split[0]));
        loc.setX(Double.parseDouble(split[1]));
        loc.setY(Double.parseDouble(split[2]));
        loc.setZ(Double.parseDouble(split[3]));
        return loc;
    }

    /**
     * Deserializes a String that contains the world name, block x, block y and block z into a Location.
     * @param str The string that is deserialized.
     * @return A Location that contains the world, block x, block y and block z.
     */
    public static Location deserializeSimple(String str) {
        if (str == null) return null;
        Location loc = new Location(null, 0, 0, 0);
        String[] split = str.split(String.valueOf(token));
        loc.setWorld(Bukkit.getWorld(split[0]));
        loc.setX((int) Double.parseDouble(split[1]));
        loc.setY((int) Double.parseDouble(split[2]));
        loc.setZ((int) Double.parseDouble(split[3]));
        return loc;
    }

    /**
     * Deserializes a String that contains the world name, x, y, z, yaw and pitch into a Location.
     * @param str The string that is deserialized.
     * @return A Location that contains the world, x, y, z, yaw and pitch.
     */
    public static Location deserializeFully(String str) {
        if (str == null) return null;
        Location loc = deserialize(str);
        String[] split = str.split(String.valueOf(token));
        loc.setYaw(Float.parseFloat(split[4]));
        loc.setPitch(Float.parseFloat(split[5]));
        return loc;
    }
}
