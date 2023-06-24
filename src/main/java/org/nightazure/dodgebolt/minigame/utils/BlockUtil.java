package org.nightazure.dodgebolt.minigame.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BlockUtil {
    public static void serializeBlocks(Location pointA, Location pointB, File outputFile) {
        World world = pointA.getWorld();

        int minX = Math.min(pointA.getBlockX(), pointB.getBlockX());
        int minY = Math.min(pointA.getBlockY(), pointB.getBlockY());
        int minZ = Math.min(pointA.getBlockZ(), pointB.getBlockZ());
        int maxX = Math.max(pointA.getBlockX(), pointB.getBlockX());
        int maxY = Math.max(pointA.getBlockY(), pointB.getBlockY());
        int maxZ = Math.max(pointA.getBlockZ(), pointB.getBlockZ());

        List<BlockData> blockDataList = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location blockLocation = new Location(world, x, y, z);
                    Block block = blockLocation.getBlock();

                    Material material = block.getType();
                    byte data = block.getData();

                    BlockData blockData = new BlockData(blockLocation, material, data);
                    blockDataList.add(blockData);
                }
            }
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(blockDataList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void deserializeBlocks(File inputFile) {
        try (FileInputStream fis = new FileInputStream(inputFile);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            List<BlockData> blockDataList = (List<BlockData>) ois.readObject();

            for (BlockData blockData : blockDataList) {
                Location location = blockData.getLocation();
                Material material = blockData.getMaterial();
                byte data = blockData.getData();

                Block block = location.getBlock();
                block.setType(material);

                if (blockData instanceof Slab) {
                    Slab slab = (Slab) blockData;
                    slab.setType(Slab.Type.values()[data]);
                    block.setBlockData(slab);
                } else if (blockData instanceof Stairs) {
                    Stairs stairs = (Stairs) blockData;
                    stairs.setFacing(BlockFace.values()[data]);
                    block.setBlockData(stairs);
                } else {
                    // Handle other block data types accordingly
                    // For example: Fence, Door, Chest, etc.
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    private static class BlockData implements Serializable {
        private final double x;
        private final double y;
        private final double z;
        private final String worldName;
        private final Material material;
        private final byte data;

        public BlockData(Location location, Material material, byte data) {
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            this.worldName = location.getWorld().getName();
            this.material = material;
            this.data = data;
        }

        public Location getLocation() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                throw new IllegalArgumentException("Invalid world: " + worldName);
            }
            return new Location(world, x, y, z);
        }

        public Material getMaterial() {
            return material;
        }

        public byte getData() {
            return data;
        }
    }


    public static List<Block> changeBlockSides(Location pointA, Location pointB, Material material, int offset) {
        World world = pointA.getWorld();
        int minX = Math.min(pointA.getBlockX(), pointB.getBlockX());
        int minZ = Math.min(pointA.getBlockZ(), pointB.getBlockZ());
        int maxX = Math.max(pointA.getBlockX(), pointB.getBlockX());
        int maxZ = Math.max(pointA.getBlockZ(), pointB.getBlockZ());

        List<Block> affectedBlocks = new ArrayList<>();

        for (int x = minX + offset; x <= maxX - offset; x++) {
            for (int z = minZ + offset; z <= maxZ - offset; z++) {
                if (x == minX + offset || x == maxX - offset || z == minZ + offset || z == maxZ - offset) {
                    Block block = world.getBlockAt(x, pointA.getBlockY(), z);
                    if (block.getType() != material) {
                        block.setType(material);
                        affectedBlocks.add(block);
                    }
                }
            }
        }

        return affectedBlocks;
    }

}
