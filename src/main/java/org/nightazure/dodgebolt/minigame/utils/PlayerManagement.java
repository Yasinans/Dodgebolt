package org.nightazure.dodgebolt.minigame.utils;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.UUID;

public class PlayerManagement {

    private HashSet<PlayerInfo> players = new HashSet<>();
    public PlayerInfo getPlayerInfo(Player p) {
        for (PlayerInfo info : players){
            if(info.player.equals(p.getUniqueId())){
                return info;
            }
        }
        return null;
    }
    public void clearPlayers(){
        players.clear();
    }
    public PlayerInfo addPlayer(Player p) {
        PlayerInfo newPlayer = new PlayerInfo(p);
        this.players.add(newPlayer);
        return newPlayer;
    }
    public void removePlayer(Player p) {
        for (PlayerInfo info : players){
            if(info.player.equals(p.getUniqueId())){
                this.players.remove(info);
            }
        }
    }
    public boolean contains(Player p) {
        for (PlayerInfo info : players){
            if(info.player.equals(p.getUniqueId())){
                return true;
            }
        } return false;
    }
    public static class PlayerInfo{
        UUID player;
        Location location;
        ItemStack[] inventoryContents;
        ItemStack[] armorContents;
        ItemStack offhand;
        GameMode playerGamemode;
        int experience;
        public PlayerInfo(Player p){
            this.player = p.getUniqueId();
        }
        public ItemStack[] getItemStack(){
            return inventoryContents;
        }

        public void saveState(){
            this.location = Bukkit.getPlayer(this.player).getLocation().clone();
            this.playerGamemode = Bukkit.getPlayer(this.player).getGameMode();
            this.inventoryContents = Bukkit.getPlayer(this.player).getInventory().getContents().clone();
            this.armorContents = Bukkit.getPlayer(this.player).getInventory().getArmorContents().clone();
            this.offhand = Bukkit.getPlayer(this.player).getInventory().getItemInOffHand().clone();
            this.experience = getPlayerExp(Bukkit.getPlayer(this.player));

        }
        public boolean loadState(boolean loadLocation) {
            Player player = Bukkit.getPlayer(this.player);
            if (loadLocation) {
                player.teleport(this.location);
            }
            player.setTotalExperience(0);  // Reset total experience
            player.setLevel(0);
            player.setExp(0);
            player.giveExp(this.experience);  // Set the player's XP directly
            player.setGameMode(playerGamemode);
            player.getInventory().setContents(this.inventoryContents);
            player.getInventory().setArmorContents(this.armorContents);
            player.getInventory().setItemInOffHand(this.offhand);
            return true;
        }
        //XP UTILITY
        private static int getExpToLevelUp(int level){
            if(level <= 15){
                return 2*level+7;
            } else if(level <= 30){
                return 5*level-38;
            } else {
                return 9*level-158;
            }
        }
        private static int getExpAtLevel(int level){
            if(level <= 16){
                return (int) (Math.pow(level,2) + 6*level);
            } else if(level <= 31){
                return (int) (2.5*Math.pow(level,2) - 40.5*level + 360.0);
            } else {
                return (int) (4.5*Math.pow(level,2) - 162.5*level + 2220.0);
            }
        }
        private static int getPlayerExp(Player player){
            int exp = 0;
            int level = player.getLevel();
            exp += getExpAtLevel(level);
            exp += Math.round(getExpToLevelUp(level) * player.getExp());
            return exp;
        }
        private static int changePlayerExp(Player player, int exp){
            int currentExp = getPlayerExp(player);
            player.setExp(0);
            player.setLevel(0);
            int newExp = currentExp + exp;
            player.giveExp(newExp);
            return newExp;
        }
    }
}
