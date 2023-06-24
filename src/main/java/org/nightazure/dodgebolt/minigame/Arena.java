package org.nightazure.dodgebolt.minigame;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.nightazure.dodgebolt.minigame.events.ArenaEndEvent;
import org.nightazure.dodgebolt.minigame.events.ArenaStartEvent;
import org.nightazure.dodgebolt.minigame.utils.BroadcastUtil;
import org.nightazure.dodgebolt.minigame.utils.PlayerManagement;

import java.util.*;

public class Arena {
    private final Plugin plugin;
    private final String name;
    private int status;
    private final int waitingTime;
    private final PlayerManagement playermanager;
    public final HashMap<Team, List<Location>> teams = new HashMap<>();
    private Map.Entry<Location, Location> arenaPos;
    private final boolean onHoldSystem;
    public List<Player> onHoldPlayers = new ArrayList<>();

    public Arena(String name, List<Team> teams, boolean onHoldSystem, int waitingTime, Plugin plugin) {
        if (name.length() == 0 || teams.size() == 0) {
            throw new IllegalArgumentException("Invalid arguments");
        }
        this.plugin = plugin;
        this.playermanager = new PlayerManagement();
        this.name = name;
        this.status = 0;
        this.waitingTime = waitingTime;
        this.onHoldSystem = onHoldSystem;
        for (Team team : teams) {
            this.teams.put(team, new ArrayList<>());
        }
    }
    public int getStatus() {
        return status;
    }

    //0- no queue, 1-queue, 2-freeze, 3-game 4-end
    public void setStatus(int status) {
        this.status = status;
    }

    public void checkReady() {
        //remember to call this after method
        if (isTeamsReady(onHoldSystem)) {
            if(getStatus()==0) {
                setStatus(1);
                runQueue();
            }
        } else if (getPlayers().size() <= 1) {
            setStatus(0);
        }
    }
    public void endArena(Team winner) {
        if (getStatus() >= 2) {
            Bukkit.getServer().getPluginManager().callEvent(new ArenaEndEvent(this, winner));
            setStatus(0);
            for (Team team : teams.keySet()) {
                team.clearPlayer(false);
            }
        } else {
            throw new RuntimeException("This shouldn't happen. Please report this to the developer");
        }
    }

    public void startArena() {
        if (!isTeamsReady(onHoldSystem)) {
            broadcastPlayer(ChatColor.RED + "There is no enough players to start.", null);
        } else if (this.getStatus() >= 2) {

        } else {
            if (onHoldSystem) distributeHold();
            //set status to 3 - freeze
            setStatus(2);
            //add players
            for (Player p : getPlayers()) {
                if(p.isDead()) p.spigot().respawn();
                playermanager.addPlayer(p);
                PlayerManagement.PlayerInfo player = playermanager.getPlayerInfo(p);
                player.saveState();
                p.getInventory().clear();
            }
            teleportPlayers(false);
            Bukkit.getServer().getPluginManager().callEvent(new ArenaStartEvent(this));
            new BukkitRunnable() {
                int count = 5;
                @Override
                public void run() {
                    if (getStatus() == 0) cancel();
                    BroadcastUtil.titleBroadcast(ChatColor.GREEN + "Game Starting in", ChatColor.GOLD + String.valueOf(count) + " seconds", getPlayers());
                    for (Player p : getPlayers()) {
                        if(count == 0)p.playSound(p.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 1.0f, 1.0f);
                        else p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    }
                    if (count <= 0) {
                        setStatus(3);
                        cancel();
                    }
                    count--;
                }
            }.runTaskTimer(plugin, 0, 20);
        }
    }

    public void runQueue() {
        broadcastPlayer(ChatColor.GREEN + "The game will start in "+ ChatColor.GOLD + ChatColor.BOLD + waitingTime + ChatColor.RESET+ChatColor.GOLD+" seconds", null);
        if (plugin == null) throw new AssertionError();
        new BukkitRunnable() {
            int countdownSec = waitingTime;

            @Override
            public void run() {
                if (getStatus() == 0) cancel();
                else if (waitingTime == waitingTime * 0.50)
                    broadcastPlayer(ChatColor.GREEN + "The game will start in " + ChatColor.GOLD + ChatColor.BOLD + countdownSec + ChatColor.RESET+ChatColor.GOLD+ " seconds", null);
                else if (waitingTime == waitingTime * 0.30)
                    broadcastPlayer(ChatColor.GREEN + "The game will start in " + ChatColor.GOLD + ChatColor.BOLD + countdownSec + ChatColor.RESET+ChatColor.GOLD+ " seconds", null);
                else if (countdownSec <= 5 && countdownSec != 0)
                    broadcastPlayer(ChatColor.GREEN + "The game will start in " + ChatColor.GOLD + ChatColor.BOLD + countdownSec + ChatColor.RESET+ChatColor.GOLD+ " seconds", null);
                else if (countdownSec == 0) {
                    broadcastPlayer(ChatColor.GOLD + "Teleporting players...........", null);
                    cancel();
                    startArena();
                }
                countdownSec--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public String getName() {
        return name;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    public int getTotalMax() {
        int total = 0;
        for (Team team : teams.keySet()) {
            total += team.maxPlayer;
        }
        return total;
    }

    public void distributeHold() {
        int total = getTotalMax();
        int dist = total / teams.size();
        int extra = total % teams.size();
        int count = 0;
        int i = 0;
        distribute:
        while (count <= dist) {
            for (Team team : teams.keySet()) {
                if (onHoldPlayers.size() - 1 < i) break distribute;
                team.addPlayer(onHoldPlayers.get(i));
                i++;
            }
            count++;
        }
        if (extra > 0) {
            for (Team team : teams.keySet()) {
                if (extra == 0) break;
                if (onHoldPlayers.size() - 1 < i) break;
                team.addPlayer(onHoldPlayers.get(i));
                extra--;
                i++;
            }
        }
        onHoldPlayers.clear();
    }

    public List<Player> getPlayers() {
        List<Player> players = new ArrayList<>();
        if (onHoldSystem && getStatus() < 2) return onHoldPlayers;
        for (Team team : teams.keySet()) {
            players.addAll(team.getPlayers());
        }
        return players;
    }

    public void broadcastPlayer(String message, Player player) {
        if (getPlayers() != null) {
            for (Player p : getPlayers()) {
                if (player != null && p == player) {
                    continue;
                }
                p.sendMessage(message);
            }
        }
    }

    public Team getPlayerTeam(Player p) {
        for (Team team : teams.keySet()) {
            if (team.containsPlayer(p)) {
                return team;
            }
        }
        return null;
    }

    public void playerLeave(Player p) {
        if (getPlayers().contains(p)) {
            if (this.status <= 1) {
                if (!onHoldSystem) getPlayerTeam(p).removePlayer(p);
                else onHoldPlayers.remove(p);
                if (getPlayers().size() <= 1) {
                    broadcastPlayer(ChatColor.RED + "Not enough players to start the queue.", p);
                }
                checkReady();
            } else {
                handleElimination(p, 0, null);
            }
        }
    }

    public void handleElimination(Player victim, int reason, Player attacker) {
        //0 - left the match (quit), 1- eliminated
        if (reason == 0) broadcastPlayer(getPlayerTeam(victim).getColor()+victim.getName()+ChatColor.RED + " has left the match", victim);
        if (reason == 1 && attacker!=null) broadcastPlayer(getPlayerTeam(victim).getColor() + "" + ChatColor.BOLD + victim.getName() + ChatColor.RESET + "" + ChatColor.GOLD + " has been eliminated by "
                + getPlayerTeam(attacker).getColor() + "" + ChatColor.BOLD + attacker.getName(), null);
        if (reason == 2) broadcastPlayer(getPlayerTeam(victim).getColor() + "" + ChatColor.BOLD + victim.getName() + ChatColor.RESET + "" + ChatColor.GOLD + " has fell to their death", null);
        //update this soon to support spectator MMMMMM
        Team team = getPlayerTeam(victim);
        if (team.getPlayers().size() == 1 && getRemainingTeams().size()==2) {
            if(attacker!=null)endArena(getPlayerTeam(attacker));
            else endArena(getTopTeam());
        } else {
            if(attacker!=null) victim.sendMessage(ChatColor.RED+"You have been eliminated by "+getPlayerTeam(attacker).getColor()+ChatColor.BOLD+attacker.getName());
            team.removePlayer(victim);
            playermanager.getPlayerInfo(victim).loadState(true);
        }

    }
    public List<Team> getRemainingTeams(){
        List<Team> remainingTeam = new ArrayList<>();
        for(Team team: teams.keySet()){
            if(team.getPlayers().size() >= 1) remainingTeam.add(team);
        }
        return remainingTeam;
    }

    public Team getTopTeam(){
        Team highestTeam = null;
        for (Team team : teams.keySet()) {
            if(highestTeam==null) highestTeam = team;
            if (highestTeam.getPlayers().size() < team.getPlayers().size()) highestTeam = team;
        }
        return highestTeam;
    }

    public PlayerManagement getPlayerManager() {
        return playermanager;
    }

    //man watf is this
    public void playerJoin(Player player, Team team) {
        if (this.status <= 1) {
            if (onHoldSystem) {
                if (getTotalMax() > onHoldPlayers.size()) {
                    onHoldPlayers.add(player);
                    player.sendMessage(ChatColor.GREEN + "You have successfully joined the queue!");
                    broadcastPlayer(ChatColor.GOLD + player.getName() + ChatColor.DARK_GREEN + " have joined the queue!", player);
                } else {
                    player.sendMessage(ChatColor.RED + "This arena is already full!");
                }
            } else {

                if (team.getPlayers().size() >= team.maxPlayer) {
                    player.sendMessage(ChatColor.RED + "That team is already full!");
                } else if (team.containsPlayer(player)) {
                    player.sendMessage(ChatColor.RED + "You already in the queue!");
                } else {
                    player.sendMessage(ChatColor.GREEN + "You have successfully joined the " + team.getName() + " team!");
                    broadcastPlayer(ChatColor.GOLD + player.getName() + ChatColor.DARK_GREEN + " have joined the " + team.getName() + " team!", player);
                    team.addPlayer(player);
                }
            }
            checkReady();
        } else {
            player.sendMessage(ChatColor.RED + "Sorry, this arena is currently not available!");
        }
    }

    public void teleportPlayers(Boolean randomizedTeleport) {
        for (Team team : teams.keySet()) {
            if (team.isUniqueSpawn()) {
                List<Location> clonedSpawnLoc = new ArrayList<>(teams.get(team));
                if (team.getPlayers().size() > teams.get(team).size()) {
                    throw new RuntimeException("Not enough spawn locations");
                }
                if (randomizedTeleport) {
                    Collections.shuffle(clonedSpawnLoc);
                }
                for (int i = 0; i < team.getPlayers().size(); i++) {
                    team.getPlayers().get(i).teleport(clonedSpawnLoc.get(i));
                }
            } else {
                for (int i = 0; i < team.getPlayers().size(); i++) {
                    if (getSpawnLocation(team).size() > 0) {
                        team.getPlayers().get(i).teleport(teams.get(team).get(0));
                    } else {
                        throw new RuntimeException("No spawn location set");
                    }
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////
    public boolean isArenaReady() {
        for (Team team : teams.keySet()) {
            if (team.isUniqueSpawn()) {
                if (teams.get(team).size() < team.maxPlayer) {
                    return false;
                }
            } else {
                if (teams.get(team).size() == 0) {
                    return true;
                }
            }
        }
        return true;
    }

    public void setArenaPos(Location firstPos, Location secondPos) {
        arenaPos = new AbstractMap.SimpleEntry<>(firstPos, secondPos);
    }

    public Map.Entry<Location, Location> getArenaPos() {
        return arenaPos;
    }

    //////////////////////////////////////////////////////////////
    public boolean isTeamsReady(boolean isHold) {
        //check if teams ready, loop all
        if (!isHold) {
            for (Team team : teams.keySet()) {
                //remove offlineplayer
                team.clearPlayer(true);
                //players are more than minimum
                if (team.getPlayers().size() < team.minPlayer) return false;
            }
        } else {
            return getPlayers().size() >= 2;
        }
        return true;
    }

    public boolean isTeamsConfigured(Team team) {
        if (team.isUniqueSpawn()) {
            return teams.get(team).size() >= team.maxPlayer;
        } else {
            return teams.get(team).size() >= 1;
        }
    }

    public Team getTeamByName(String name) {
        for (Team team : teams.keySet()) {
            if (team.getName().equalsIgnoreCase(name)) {
                return team;
            }
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////
    public void clearSpawnLocation(Team team) {
        teams.get(team).clear();
    }

    public List<Location> getSpawnLocation(Team team) {
        return teams.get(team);
    }

    public boolean addSpawnLocation(Team team, Location location) {
        if (!team.isUniqueSpawn()) {
            teams.get(team).set(0, location);
        } else if (teams.get(team).size() <= team.maxPlayer) {
            teams.get(team).add(location);
        } else {
            return false;
        }
        return true;
    }
}
