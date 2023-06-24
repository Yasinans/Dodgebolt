package org.nightazure.dodgebolt;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.nightazure.dodgebolt.minigame.Team;
import org.nightazure.dodgebolt.minigame.utils.Config;
import org.nightazure.dodgebolt.minigame.Arena;
import org.nightazure.dodgebolt.minigame.utils.BlockUtil;
import org.nightazure.dodgebolt.minigame.utils.LocationUtil;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DodgeboltArena extends Arena {
    public Map.Entry<Location, Location> redTeam;
    public Map.Entry<Location, Location> blueTeam;
    public Map.Entry<Location, Location> planeArea;
    public Location redArrow;
    public Location blueArrow;
    //loc 1 arena, 2 first, 3 second, 4 plane
    public DodgeboltArena(String name, List<Team> teams, Map.Entry<Location, Location> loc1, Map.Entry<Location, Location> loc2, Map.Entry<Location, Location> loc3, Map.Entry<Location, Location> loc4, Location redArrow, Location blueArrow, int waitingTime, Plugin plugin) {
        super(name, teams, true, waitingTime, plugin);
        super.setArenaPos(loc1.getKey(), loc1.getValue());
        this.redArrow = redArrow;
        this.blueArrow = blueArrow;
        this.redTeam = loc2;
        this.blueTeam = loc3;
        this.planeArea = loc4;
    }
    public void savePlane(){
        File file = new File(Bukkit.getServer().getPluginManager().getPlugin("Dodgebolt").getDataFolder(), "/arenas/"+this.getName()+".dat");
        if (!file.exists()){
            try{
                file.createNewFile();
            } catch (IOException e){}
        }
        BlockUtil.serializeBlocks(planeArea.getKey(), planeArea.getValue(),file);
    }
    public File getPlaneData(){
        File file = new File(Bukkit.getServer().getPluginManager().getPlugin("Dodgebolt").getDataFolder(), "/arenas/"+this.getName()+".dat");
        return file;
    }
    public void setRedTeamPos(Location loc, Location loc2){
        this.redTeam = new AbstractMap.SimpleEntry<>(loc,loc2);
    }
    public void setBlueTeamPos(Location loc, Location loc2){
        this.blueTeam = new AbstractMap.SimpleEntry<>(loc,loc2);
    }
    public void dropArrow(Team team){
        if(team.getName()=="Red") redArrow.getWorld().dropItem(redArrow, new ItemStack(Material.ARROW)).setVelocity(new Vector(0,-1,0));
        else blueArrow.getWorld().dropItem(blueArrow, new ItemStack(Material.ARROW)).setVelocity(new Vector(0,-1,0));
    }
    public void setRedArrow(Location loc){
        loc.setY(redArrow.getY()+1);
        this.redArrow = loc;
    }
    public void setBlueArrow(Location loc){
        loc.setY(redArrow.getY()+1);
        this.blueArrow = loc;
    }
    public void generateConfig(Plugin plugin){
        Config config = new Config(this.getName(), "arenas", plugin);
        config.setup();
        savePlane();
        config.get().set("arenaPos.x", LocationUtil.serializeSimple(this.getArenaPos().getKey()));
        config.get().set("arenaPos.y", LocationUtil.serializeSimple(this.getArenaPos().getValue()));
        config.get().set("redTeamRoom.x", LocationUtil.serializeSimple(this.redTeam.getKey()));
        config.get().set("redTeamRoom.y", LocationUtil.serializeSimple(this.redTeam.getValue()));
        config.get().set("blueTeamRoom.x", LocationUtil.serializeSimple(this.blueTeam.getKey()));
        config.get().set("blueTeamRoom.y", LocationUtil.serializeSimple(this.blueTeam.getValue()));
        config.get().set("arenaPlane.x", LocationUtil.serializeSimple(this.planeArea.getKey()));
        config.get().set("arenaPlane.y", LocationUtil.serializeSimple(this.planeArea.getValue()));
        config.get().set("redArrow", LocationUtil.serializeSimple(this.redArrow));
        config.get().set("blueArrow", LocationUtil.serializeSimple(this.blueArrow));
        List<String> simplifiedLocation = new ArrayList<String>();
        for(Location location: this.getSpawnLocation(this.getTeamByName("Red"))){
            simplifiedLocation.add(LocationUtil.serializeFully(location));
        }
        config.get().set("spawnLocation.red", simplifiedLocation);
        simplifiedLocation = new ArrayList<String>();
        for(Location location: this.getSpawnLocation(this.getTeamByName("Blue"))){
            simplifiedLocation.add(LocationUtil.serializeFully(location));
        }
        config.get().set("spawnLocation.blue", simplifiedLocation);
        config.save();
    }


}
