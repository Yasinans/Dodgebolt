package org.nightazure.dodgebolt;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.nightazure.dodgebolt.minigame.utils.Config;
import org.nightazure.dodgebolt.minigame.Minigame;
import org.nightazure.dodgebolt.minigame.Team;
import org.nightazure.dodgebolt.minigame.utils.BlockUtil;
import org.nightazure.dodgebolt.minigame.utils.LocationUtil;
import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class Dodgebolt extends JavaPlugin {
    Minigame minigame;
    Team firstTeam;
    Team secondTeam;
    public List<DodgeboltArena> arenas;
    public ArenaSetup arenaSetup;
    //this plugin is temporary only
    FileConfiguration gameConfig;
    boolean allowTeamPass;
    boolean disableShrink;
    @Override
    public void onEnable() {
        this.arenas = new ArrayList<>();
        this.minigame = new Minigame(2, "Dodgebolt", this);
        this.minigame.setWaitingTime(30);
        this.disableShrink = false;
        this.minigame.setBestOfNum(3);
        this.allowTeamPass = true;
        this.firstTeam = this.minigame.addTeam("Red", 3, 1 , true);
        this.secondTeam = this.minigame.addTeam("Blue", 3, 1 , true);
        this.firstTeam.setColor(ChatColor.RED);
        this.secondTeam.setColor(ChatColor.BLUE);
        this.getServer().getPluginManager().registerEvents(new DodgeListener(this), this);
        checkConfig();
        loadArena();
        CommandList dodgebolt = new CommandList(this);
        getServer().getPluginCommand("dodgebolt").setExecutor(dodgebolt);
        getServer().getPluginCommand("dodgebolt").setTabCompleter(dodgebolt);
    }

    @Override
    public void onDisable() {
        emergencyStop();
    }

    public void emergencyStop(){
        for(DodgeboltArena arena: arenas){
            if(arena.getStatus()>= 2){
                List<Player> players = arena.getPlayers();
                for (Player player : players) {
                    arena.getPlayerManager().getPlayerInfo(player).loadState(true);
                }
                BlockUtil.deserializeBlocks(arena.getArenaData());
                arena.getPlayerManager().clearPlayers();
                arena.setStatus(0);
                for (Team team : arena.teams.keySet()) {
                    team.clearPlayer(false);
                    team.setScore(0);
                }
            }
        }
    }
    public boolean containsPlayer(Player p){
        for(DodgeboltArena arena: arenas){
            if(arena.getPlayers().contains(p)) return true;
        }
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////
    public Minigame getMinigame(){
        return minigame;
    }
    public void loadArena(){
        File arenaFolder = new File(Bukkit.getServer().getPluginManager().getPlugin(this.getName()).getDataFolder()+"/arenas/");
        File[] listOfArenas = arenaFolder.listFiles();
        if(listOfArenas != null){
            for(File arenas: listOfArenas){
                if(arenas.isFile() && arenas.getName().endsWith(".yml")){
                    try {
                        String name = arenas.getName().replaceFirst("[.][^.]+$", "");
                        Config configuration = new Config(name, "arenas", this);
                        configuration.setup();
                        FileConfiguration arenaConfig = configuration.get();
                        DodgeboltArena arena = new DodgeboltArena(name, this.minigame.getTeams(),
                                new AbstractMap.SimpleEntry<>(LocationUtil.deserializeSimple(arenaConfig.getString("arenaPos.x")), LocationUtil.deserializeSimple(arenaConfig.getString("arenaPos.y"))),
                                new AbstractMap.SimpleEntry<>(LocationUtil.deserializeSimple(arenaConfig.getString("redTeamRoom.x")), LocationUtil.deserializeSimple(arenaConfig.getString("redTeamRoom.y"))),
                                new AbstractMap.SimpleEntry<>(LocationUtil.deserializeSimple(arenaConfig.getString("blueTeamRoom.x")), LocationUtil.deserializeSimple(arenaConfig.getString("blueTeamRoom.y"))),
                                new AbstractMap.SimpleEntry<>(LocationUtil.deserializeSimple(arenaConfig.getString("arenaPlane.x")), LocationUtil.deserializeSimple(arenaConfig.getString("arenaPlane.y"))),
                                LocationUtil.deserializeSimple(arenaConfig.getString("redArrow")),
                                LocationUtil.deserializeSimple(arenaConfig.getString("blueArrow")),
                                this.minigame.getWaitingTime(), this);
                        if(arenaConfig.getString("spectatorLocation")!=null){
                            arena.setSpectatorLocation(LocationUtil.deserializeSimple(arenaConfig.getString("spectatorLocation")));
                        }
                        for (String serializedLocation : arenaConfig.getStringList("spawnLocation.red")) {
                            arena.addSpawnLocation(arena.getTeamByName("Red"), LocationUtil.deserializeFully(serializedLocation));
                        }
                        for (String serializedLocation : arenaConfig.getStringList("spawnLocation.blue")) {
                            arena.addSpawnLocation(arena.getTeamByName("Blue"), LocationUtil.deserializeFully(serializedLocation));
                        }
                        if (!arena.isTeamsConfigured(arena.getTeamByName("Red"))) {
                            Bukkit.getLogger().log(Level.WARNING, "The Red Team's Spawn Locations at Arena \"" + arena.getName() + "\" is not fully setup");
                        }
                        if (!arena.isTeamsConfigured(arena.getTeamByName("Blue"))) {
                            Bukkit.getLogger().log(Level.WARNING, "The Blue Team's Spawn Locations at Arena \"" + arena.getName() + "\" is not fully setup");
                        }

                        this.arenas.add(arena);
                    } catch (Exception e){
                        Bukkit.getLogger().log(Level.SEVERE, "There is a problem in your arena config. Ignoring this file");
                    }
                }
            }
        }

    }
    private void checkConfig(){
        //
        try {
            Config configuration = new Config("config", null, this);
            configuration.setup();
            configuration.get().addDefault("waiting", 30);
            configuration.get().addDefault("bestOf", 3);
            configuration.get().addDefault("allowTeamPass", true);
            configuration.get().addDefault("maxPlayer", 3);
            configuration.get().addDefault("disableShrink", false);
            configuration.get().addDefault("minPlayer", 1);
            configuration.get().options().copyDefaults(true);
            configuration.save();
            gameConfig = configuration.get();
            this.minigame.setWaitingTime(gameConfig.getInt("waiting"));
            this.allowTeamPass = gameConfig.getBoolean("allowTeamPass");
            int maxPlayer = gameConfig.getInt("maxPlayer");
            int minPlayer = gameConfig.getInt("minPlayer");
            this.minigame.setBestOfNum(gameConfig.getInt("bestOf"));
            this.disableShrink = gameConfig.getBoolean("disableShrink");
            if (minPlayer <= 0 || minPlayer > maxPlayer) {
                throw new RuntimeException("There is a error in the config! Changing back to default.");
            }
            for(Team team: this.minigame.getTeams()){
                team.minPlayer = minPlayer;
                team.maxPlayer = maxPlayer;
            }
        }catch (Exception e) {
            recreateConfig();
        }
    }
    private void recreateConfig(){
        Config configuration = new Config("config", null, this);
        configuration.setup();
        configuration.get().set("disableShrink", false);
        configuration.get().set("bestOf", 3);
        configuration.get().set("waiting", 30);
        configuration.get().set("allowTeamPass", true);
        configuration.get().set("maxPlayer", 3);
        configuration.get().set("minPlayer", 1);
        configuration.save();
    }

    public void addArena(DodgeboltArena arena){
        this.arenas.add(arena);
        HandlerList.unregisterAll(arenaSetup);
        arena.generateConfig(this);
        arenaSetup = null;
    }
    public void setupArena(Player p){
        arenaSetup = new ArenaSetup(p, this);
        this.getServer().getPluginManager().registerEvents(arenaSetup, this);
    }
    public DodgeboltArena getArenaByName(String name){
        for(DodgeboltArena dodgeArena: arenas){
            if(dodgeArena.getName().equalsIgnoreCase(name)){
                return dodgeArena;
            }
        }
        return null;
    }
    public boolean deleteArena(DodgeboltArena arena){
        try{
            if(arenas.contains(arena)){
                arenas.remove(arena);
                return true;
            } else {
                return false;
            }
        } catch(Exception e){
            return false;
        }
    }
    public void cancelSetup(ArenaSetup arena){
        HandlerList.unregisterAll(arena);
    }
}
