package org.nightazure.dodgebolt.minigame;



import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class Minigame {

    private final Plugin plugin;
    private List<Arena> arenaList = new ArrayList<>();
    private String name;
    private int waitingTime;
    private int teamNum;
    private int bestOfNum = 1;
    public List<Team> teams = new ArrayList<>();
    public Minigame(int teamNum, String name, Plugin plugin) {
        if (teamNum <= 0){
            throw new IllegalArgumentException("Invalid team number");
        } else if(name == null){
            throw new IllegalArgumentException("Invalid name");
        }
        this.plugin = plugin;
        this.teamNum = teamNum;
        this.name = name;
    }
    public Plugin getPlugin() {
        return plugin;
    }
    public void setBestOfNum(int rounds){
        if(rounds <= 0){
            throw new IllegalArgumentException("Invalid round numbers in config");
        } else{
            this.bestOfNum = rounds;
        }
    }
    public int getBestOfNum(){
        return bestOfNum;
    }
    public int getWaitingTime(){
        return waitingTime;
    }
    public void setWaitingTime(int time) {
        if(time <= 0 || time > 10000){
            throw new IllegalArgumentException("Time must be between 0 and 10000 seconds");
        } else {
            this.waitingTime = time;
        }
    }
    public void setTeams(List<Team> teams){
        this.teams = teams;
    }
    public Team addTeam(String teamName, Integer maxPlayers, Integer minPlayers, Boolean uniqueSpawn){
        if(teams.size() <= teamNum) {
            Team team = new Team(teamName, maxPlayers, minPlayers, uniqueSpawn);
            teams.add(team);
            return team;
        } else {
            throw new IllegalArgumentException("You cant teams more than your team");
        }
    }
    public void removeTeam(Team team) {
        teams.remove(team);
    }
    public List<Team> getTeams(){
        return teams;
    }
    public Team getTeamByName(String name){
        for(Team team: teams){
            if(team.getName().equalsIgnoreCase(name)){
                return team;
            }
        }
        return null;
    }
    public Arena getArenaByName(String name) {
        for(Arena arena : arenaList){
            if(arena.getName() == name){
                return arena;
            }
        }
        return null;
    }

    private boolean checkReady(){
        if(teams.size() == teamNum){
            return true;
        }
        return false;
    }
    public Arena createArena(int id, String name, boolean onHoldSystem, int waitingTime) {
        if(checkReady()){
            Arena arena = new Arena(name, this.teams, onHoldSystem, waitingTime, this);
            arenaList.add(arena);
        }
        throw new RuntimeException("Minigame is not ready. Please check your team");
    }
    public void removeArena(Arena arena) {
        arenaList.remove(arena);
    }



}
