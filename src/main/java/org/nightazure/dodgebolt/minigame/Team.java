package org.nightazure.dodgebolt.minigame;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Team {
    private String name;
    public ChatColor color;
    public Integer maxPlayer;
    public Integer minPlayer;
    private Boolean uniqueSpawn;
    private int score;
    private List<Player> players = new ArrayList<Player>();
    public Boolean isUniqueSpawn(){
        return uniqueSpawn;
    }
    public void setUniqueSpawn(Boolean uniqueSpawn){
        this.uniqueSpawn = uniqueSpawn;
    }
    public Team(String teamName, Integer maxPlayers, Integer minPlayers, Boolean uniqueSpawn){
        if(teamName.length() <= 0){
            throw new IllegalArgumentException("Empty team name");
        } else if (maxPlayers < minPlayers || maxPlayers <= 0){
            throw new IllegalArgumentException("Wrong arguments for maxPlayers and minPlayers");
        }
        this.name = teamName;
        this.maxPlayer = maxPlayers;
        this.minPlayer = minPlayers;
        this.uniqueSpawn = uniqueSpawn;
    }
    public void setScore(int score){
        this.score = score;
    }
    public int getScore(){
        return score;
    }
    public ChatColor getColor(){
        return color;
    }
    public void setColor(ChatColor color){
        this.color = color;
    }
    public String getName(){
        return name;
    }
    public void addPlayer(Player player) {
        if(players.contains(player)){
            throw new IllegalArgumentException("Player is already in the team");
        } else if (players.size() >= maxPlayer) {
            throw new IllegalArgumentException("Team is already full");
        } else{
            players.add(player);
        }
    }
    public void clearPlayer(boolean removeOffline){
        if(removeOffline){
            players.removeIf(p -> !p.isOnline());
        } else {
            players.clear();
        }
    }
    public List<Player> getPlayers(){
        return players;
    }
    public boolean containsPlayer(Player player){
        return players.contains(player);
    }
    public void removePlayer(Player player){
        if(!players.contains(player)){
            throw new IllegalArgumentException("Player is not in the team");
        } else {
            players.remove(player);
        }
    }




}
