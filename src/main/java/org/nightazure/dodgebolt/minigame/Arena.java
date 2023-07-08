package org.nightazure.dodgebolt.minigame;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.nightazure.dodgebolt.minigame.events.ArenaEndEvent;
import org.nightazure.dodgebolt.minigame.events.ArenaNextRoundEvent;
import org.nightazure.dodgebolt.minigame.events.ArenaStartEvent;
import org.nightazure.dodgebolt.minigame.utils.BlockUtil;
import org.nightazure.dodgebolt.minigame.utils.BroadcastUtil;
import org.nightazure.dodgebolt.minigame.utils.LocationUtil;
import org.nightazure.dodgebolt.minigame.utils.PlayerManagement;

import java.io.File;
import java.util.*;

@SuppressWarnings("deprecation")
public class Arena {
    public final HashMap<Team, List<Location>> teams = new HashMap<>();
    public List<Player> onHoldPlayers = new ArrayList<>();
    private List<Player> spectators = new ArrayList<>();
    private Map.Entry<Location, Location> arenaPos;
    public Location spectatorLocation;
    private final PlayerManagement playermanager;
    private final int waitingTime;
    private final Minigame minigame;
    private final String name;
    private int status;
    private int eliminationType;
    private final boolean onHoldSystem;
    private File arenaData;
    public Arena(String name, List<Team> teams, boolean onHoldSystem, int waitingTime, Minigame plugin) {
        if (name.length() == 0 || teams.size() == 0) {
            throw new IllegalArgumentException("Invalid arguments");
        }
        this.minigame = plugin;
        this.playermanager = new PlayerManagement();
        this.name = name;
        this.status = 0;
        this.eliminationType = 1;
        this.waitingTime = waitingTime;
        this.onHoldSystem = onHoldSystem;
        for (Team team : teams) {
            this.teams.put(team, new ArrayList<>());
        }
    }
    public Location getSpectatorLocation(){
        return spectatorLocation;
    }
    public boolean setSpectatorLocation(Location location){
        if(arenaPos == null || !LocationUtil.isInside(getArenaPos().getKey(),getArenaPos().getValue(),location)) return false;
        spectatorLocation = location;
        return true;
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
    public File getArenaData(){
        return arenaData;
    }
    public void endArena(Team winner, boolean isQuit) {
        if(winner!=null)winner.setScore(winner.getScore()+1);
        if (getStatus() >= 2) {
            if(isQuit || isScoreMet()) {
                Bukkit.getServer().getPluginManager().callEvent(new ArenaEndEvent(this, winner, isQuit));
                setStatus(0);
                BlockUtil.deserializeBlocks(getArenaData());
                for (Team team : teams.keySet()) {
                    team.clearPlayer(false);
                    team.setScore(0);
                }
            } else {
                for(Player player: spectators){
                    player.setGameMode(GameMode.SURVIVAL);
                }
                spectators.clear();
                Bukkit.getServer().getPluginManager().callEvent(new ArenaEndEvent(this, winner, false));
            }
        } else {
            throw new RuntimeException("This shouldn't happen. Please report this to the developer");
        }
    }
    public void startArena(boolean isNextRound) {
        if (!isTeamsReady(onHoldSystem)) {
            broadcastPlayer(ChatColor.RED + "There is no enough players to start.", null);
        } else if (this.getStatus() >= 2 && !isNextRound) {

        } else {
            if (onHoldSystem) distributeHold();
            //set status to 3 - freeze
            setStatus(2);
            runActionBar();
            //add players
            if(!isNextRound) {
                this.arenaData = new File(Bukkit.getServer().getPluginManager().getPlugin("Dodgebolt").getDataFolder(), "/arenas/" + this.getName() + ".dat");
                for (Player p : getPlayers()) {
                    if (p.isDead()) p.spigot().respawn();
                    playermanager.addPlayer(p);
                    PlayerManagement.PlayerInfo player = playermanager.getPlayerInfo(p);
                    player.saveState();
                    p.getInventory().clear();
                }
                teleportPlayers(false);
                Bukkit.getServer().getPluginManager().callEvent(new ArenaStartEvent(this));
            } else {
                BlockUtil.deserializeBlocks(getArenaData());
                for (Player p : getPlayers()) {
                    p.getInventory().clear();
                }
                teleportPlayers(false);
                Bukkit.getServer().getPluginManager().callEvent(new ArenaNextRoundEvent(this));
            }
            new BukkitRunnable() {
                int count = 5;
                @Override
                public void run() {
                    if (getStatus() == 0) cancel();
                    if(count!=0) BroadcastUtil.titleBroadcast(ChatColor.GREEN + toSmallCaps("GAME WILL START IN"), "\ud83e\udc1e"+ChatColor.GOLD + toSmallCaps(String.valueOf(count)) + ChatColor.RESET+"\ud83e\udc1c", getPlayers());
                    else BroadcastUtil.titleBroadcast(ChatColor.GREEN + toSmallCaps("THE GAME HAS BEGUN"), ChatColor.GOLD + toSmallCaps("GOOD LUCK !") + ChatColor.RESET, getPlayers());


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
            }.runTaskTimer(minigame.getPlugin(), 0, 20);
        }
    }


    public void runActionBar(){
        new BukkitRunnable() {
            @Override
            public void run() {
                if(status<2) cancel();
                for(Player p: getPlayers()){
                    StringBuilder text = new StringBuilder();
                    for(Team team : teams.keySet()){
                        text.append(ChatColor.BOLD+""+team.getColor()+toSmallCaps(team.getName().toUpperCase())+": "+ChatColor.GOLD);
                        int score = team.getScore();
                        for(int i = 0; i < minigame.getBestOfNum(); i++){
                            if(score!=0) {
                                text.append("\u25cf");
                                score--;
                            } else{
                                text.append("\u25cb");
                            }
                        }
                        if(teams.keySet().stream().reduce((one, two) -> two).get()!=team)text.append(" || ");
                    }
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(text.toString()));
                }
            }
        }.runTaskTimer(minigame.getPlugin(), 0, 20);
    }
    public static String toSmallCaps(String input) {
        String[] mathBoldSansCapitals = {
                "\uD835\uDDD4", "\uD835\uDDD5", "\uD835\uDDD6", "\uD835\uDDD7", "\uD835\uDDD8",
                "\uD835\uDDD9", "\uD835\uDDDa", "\uD835\uDDDb", "\uD835\uDDDc", "\uD835\uDDDd",
                "\uD835\uDDDe", "\uD835\uDDDf", "\uD835\uDDe0", "\uD835\uDDe1", "\uD835\uDDe2",
                "\uD835\uDDe3", "\uD835\uDDe4", "\uD835\uDDe5", "\uD835\uDDe6", "\uD835\uDDe7",
                "\uD835\uDDe8", "\uD835\uDDe9", "\uD835\uDDea", "\uD835\uDDeb", "\uD835\uDDec",
                "\uD835\uDDed"
        };
        String[] mathBoldSansNumbers = {
                "\uD835\udfec","\uD835\udfed", "\uD835\udfee", "\uD835\udfef", "\uD835\udff0", "\uD835\udff1",
                "\uD835\udff2", "\uD835\udff3", "\uD835\udff4", "\uD835\udff5"
        };

        StringBuilder output = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                output.append(mathBoldSansCapitals[c - 'A']);
            } else if (c >= '0' && c <= '9') {
                output.append(mathBoldSansNumbers[c - '0']);
            } else {
                output.append(c);
            }
        }
        return output.toString();
    }
    public void runQueue() {
        broadcastPlayer(ChatColor.GREEN + "The game will start in "+ ChatColor.GOLD + ChatColor.BOLD + waitingTime + ChatColor.RESET+ChatColor.GOLD+" seconds", null);
        if (minigame == null) throw new AssertionError();
        new BukkitRunnable() {
            int countdownSec = waitingTime;

            @Override
            public void run() {
                if (getStatus() == 0) cancel();
                else if (waitingTime == waitingTime * 0.50)
                    broadcastPlayer(ChatColor.GRAY + "The game will start in " + ChatColor.GOLD + ChatColor.BOLD + countdownSec + ChatColor.RESET+ChatColor.GOLD+ " seconds", null);
                else if (waitingTime == waitingTime * 0.30)
                    broadcastPlayer(ChatColor.GRAY + "The game will start in " + ChatColor.GOLD + ChatColor.BOLD + countdownSec + ChatColor.RESET+ChatColor.GOLD+ " seconds", null);
                else if (countdownSec <= 5 && countdownSec != 0)
                    broadcastPlayer(ChatColor.GRAY + "Teleporting you in " + ChatColor.GOLD + ChatColor.BOLD + countdownSec + ChatColor.RESET+ChatColor.GOLD+ " seconds", null);
                else if (countdownSec == 0) {
                    broadcastPlayer(ChatColor.GOLD + "Teleporting players...........", null);
                    cancel();
                    startArena(false);
                }
                countdownSec--;
            }
        }.runTaskTimer(minigame.getPlugin(), 0, 20);
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
        //0 - remove them from game, 1 -- spectator
        Team team = getPlayerTeam(victim);
        int realRemaining = 0;
        for(Player p: team.getPlayers()){
            if(isSpectator(p)){
                realRemaining++;
            }
        }
        if ((team.getPlayers().size()-realRemaining) == 1 && getRemainingTeams().size()==2) {
            List<Team> ignoreTeam = Arrays.asList(team);
            if(attacker!=null)endArena(getPlayerTeam(attacker), false);
            else endArena(getTopTeam(ignoreTeam), true);
        } else {
            if(attacker!=null) victim.sendMessage(ChatColor.RED+"You have been eliminated by "+getPlayerTeam(attacker).getColor()+ChatColor.BOLD+attacker.getName());
            if(eliminationType == 0 || reason == 0){
                team.removePlayer(victim);
                playermanager.getPlayerInfo(victim).loadState(true);
            } else if(eliminationType == 1){
                sendToSpectator(victim);
            }
        }
    }
    public boolean isSpectator(Player player){
        return spectators.contains(player);
    }

    public boolean isScoreMet(){
        for(Team team: teams.keySet()){
            if(team.getScore() == minigame.getBestOfNum()){
                return true;
            }
        }
        return false;
    }
    private void sendToSpectator(Player player){
        spectators.add(player);
        player.setGameMode(GameMode.SPECTATOR);
    }
    public List<Team> getRemainingTeams(){
        List<Team> remainingTeam = new ArrayList<>();
        for(Team team: teams.keySet()){
            if(team.getPlayers().size() >= 1) remainingTeam.add(team);
        }
        return remainingTeam;
    }
    public Team getTopTeam(List<Team> ignoredTeams){
        Team highestTeam = null;
        for (Team team : teams.keySet()) {
            if(ignoredTeams!=null){
                if(ignoredTeams.contains(team)) continue;
            }
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
        if(eliminationType==1){
            if(spectatorLocation==null) return false;
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
