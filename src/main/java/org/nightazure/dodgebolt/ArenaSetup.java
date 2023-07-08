package org.nightazure.dodgebolt;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.nightazure.dodgebolt.minigame.Team;

import java.util.AbstractMap;
import java.util.Map;


@SuppressWarnings("deprecation")
public class ArenaSetup implements Listener {
    private Player player;
    private String arenaName;
    private int step;
    private Location firstPos;
    private Location secondPos;
    private Map.Entry<Location, Location> firstTeam;
    private Map.Entry<Location, Location> secondTeam;
    private Map.Entry<Location, Location> arenaPos;
    private Location redArrow;
    private Location blueArrow;
    private DodgeboltArena arena;
    private Dodgebolt dodgebolt;

    public ArenaSetup(Player player, Dodgebolt dodgebolt) {
        this.player = player;
        this.step = 0;
        this.dodgebolt = dodgebolt;
        String message = ChatColor.GOLD + "---" + ChatColor.RESET + ChatColor.BOLD + "Entering Dodgebolt Arena Setup" + ChatColor.RESET + ChatColor.GOLD + "---\n" +
                ChatColor.YELLOW + "Please enter your desired arena name in the chat:";
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        this.player.sendMessage(message);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.getPlayer() != this.player) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();

        if (message.equalsIgnoreCase("quit")) {
            player.sendMessage(ChatColor.RED + "The Arena setup has been cancelled");
            dodgebolt.cancelSetup(this);
            return;
        }

        StringBuilder response = new StringBuilder();
        switch (step) {
            case 0:
                this.arenaName = message;
                if(dodgebolt.getArenaByName(message)!=null) {
                    player.sendMessage(ChatColor.RED+"This arena name already exist! Find another one");
                    response.append(ChatColor.YELLOW + "Please enter your desired arena name in the chat:");
                    break;
                }
                response.append(ChatColor.GREEN + "The arena name has been set to " + ChatColor.GREEN + ChatColor.BOLD + arenaName);
                response.append(ChatColor.YELLOW + "\nLet's proceed with setting up the arena room. \n"+ ChatColor.LIGHT_PURPLE +"Please break a block where the first position would be. \n" + ChatColor.RESET+ChatColor.ITALIC + "(Use Golden Axe to Break)");
                step = 1;
                break;
            case 3:
                if (message.equalsIgnoreCase("reset")) {
                    this.arenaName = message;
                    response.append(ChatColor.YELLOW + "\nLet's proceed with setting up the arena room. \n"+ ChatColor.LIGHT_PURPLE +"Please break a block where the first position would be. \n");
                    step = 1;
                } else {
                    this.arenaPos = new AbstractMap.SimpleEntry<>(firstPos, secondPos);
                    response.append(ChatColor.YELLOW + "\nLet's proceed with setting the room of the Red team. \n"+ ChatColor.LIGHT_PURPLE +"Please break a block where the first position would be. \n");
                    step = 4;
                }
                break;
            case 6:
                if (message.equalsIgnoreCase("reset")) {
                    response.append(ChatColor.YELLOW + "\nLet's proceed with setting the room of the Red team. \n"+ ChatColor.LIGHT_PURPLE +"Please break a block where the first position would be. \n");
                    step = 4;
                } else {
                    firstPos.setY(-256);
                    secondPos.setY(256);
                    this.firstTeam = new AbstractMap.SimpleEntry<>(firstPos, secondPos);
                    response.append(ChatColor.YELLOW + "\nLet's proceed with setting the room of the Blue team. \n"+ ChatColor.LIGHT_PURPLE +"Please break a block where the first position would be. \n");
                    step = 7;
                }
                break;
            case 9:
                if (message.equalsIgnoreCase("reset")) {
                    response.append(ChatColor.YELLOW + "\nLet's proceed with setting the room of the Blue team. \n"+ ChatColor.LIGHT_PURPLE +"Please break a block where the first position would be. \n");
                    step = 7;
                } else {
                    firstPos.setY(-256);
                    secondPos.setY(256);
                    this.secondTeam = new AbstractMap.SimpleEntry<>(firstPos, secondPos);
                    response.append(ChatColor.YELLOW + "\nLet's proceed with setting the Arena Field. \n"+ ChatColor.LIGHT_PURPLE +"Please break a block where the first position would be. \n");
                    step = 10;
                }
                break;
            case 12:
                if (message.equalsIgnoreCase("reset")) {
                    response.append(ChatColor.YELLOW + "\nLet's proceed with setting the Arena Field. \n"+ ChatColor.LIGHT_PURPLE +"Please break a block where the first position would be. \n");
                    step = 10;
                } else {
                    response.append(ChatColor.YELLOW + "\nNow let's set the Arrow Spawn Location for each team.\n");
                    response.append(ChatColor.LIGHT_PURPLE + "\nSay "+ChatColor.GREEN+ChatColor.BOLD+"SET"+ChatColor.RESET+ChatColor.LIGHT_PURPLE+" to set the Arrow Spawn Location for the Red team");
                    step = 13;
                }
                break;
            case 13:
                if (message.equalsIgnoreCase("set")) {
                    redArrow = player.getLocation();
                    redArrow.setY(redArrow.getY() + 1);
                    response.append(ChatColor.YELLOW + "\nArrow Spawn Location for Red Team has been set.\n");
                    response.append(ChatColor.LIGHT_PURPLE + "\nSay "+ChatColor.GREEN+ChatColor.BOLD+"SET"+ChatColor.RESET+ChatColor.LIGHT_PURPLE+" to set the Arrow Spawn Location for the Blue team");
                    step = 14;
                }
                break;
            case 14:
                if (message.equalsIgnoreCase("set")) {
                    blueArrow = player.getLocation();
                    blueArrow.setY(blueArrow.getY() + 1);
                    Map.Entry<Location, Location> planeArea = new AbstractMap.SimpleEntry<>(firstPos, secondPos);
                    arena = new DodgeboltArena(arenaName, dodgebolt.getMinigame().getTeams(), arenaPos, firstTeam, secondTeam, planeArea, redArrow, blueArrow, dodgebolt.minigame.getWaitingTime(), dodgebolt);
                    response.append(ChatColor.YELLOW + "\nArrow Spawn Location for Blue Team has been set. Now let's set the Spawn Locations for each team\n");
                    response.append(ChatColor.LIGHT_PURPLE + "\nSay "+ChatColor.GREEN+ChatColor.BOLD+"SET"+ChatColor.RESET+ChatColor.LIGHT_PURPLE+" to add a Spawn Location for the Red team");
                    step = 15;
                }
                break;
            case 15:
                if (message.equalsIgnoreCase("set")) {
                    Team red = arena.getTeamByName("Red");
                    arena.addSpawnLocation(red, player.getLocation());
                    if (arena.getSpawnLocation(red).size() == red.maxPlayer) {
                        response.append(ChatColor.YELLOW + "\nSpawn Locations for Red Team is Done. Now let's set the Spawn Locations for the Blue Team.\n");
                        response.append(ChatColor.LIGHT_PURPLE + "\nSay "+ChatColor.GREEN+ChatColor.BOLD+"SET"+ChatColor.RESET+ChatColor.LIGHT_PURPLE+" to add a Spawn Location for the Blue team");
                        step = 16;
                    } else {
                        int remaining = red.maxPlayer - arena.getSpawnLocation(red).size();
                        response.append(ChatColor.YELLOW + "\nNew location has been added for the Red Team. " + remaining + " more remaining!\n" + ChatColor.GOLD + "\nSay "+ChatColor.GREEN+ChatColor.BOLD+"SET"+ChatColor.RESET+ChatColor.LIGHT_PURPLE+" to add a Spawn Location for the Red team");
                    }
                }
                break;
            case 16:
                if (message.equalsIgnoreCase("set")) {
                    Team blue = arena.getTeamByName("Blue");
                    arena.addSpawnLocation(blue, player.getLocation());
                    if (arena.getSpawnLocation(blue).size() == blue.maxPlayer) {
                        response.append(ChatColor.YELLOW + "\nSpawn Locations for Blue Team is Done. Now let's set the Spectator Location.\n");
                        response.append(ChatColor.LIGHT_PURPLE + "\nSay "+ChatColor.GREEN+ChatColor.BOLD+"SET"+ChatColor.RESET+ChatColor.LIGHT_PURPLE+" to set the Spectator Location");
                        step = 17;
                    } else {
                        int remaining = blue.maxPlayer - arena.getSpawnLocation(blue).size();
                        response.append(ChatColor.YELLOW + "\nNew location has been added for the Blue Team. " + remaining + " more remaining!\n" + ChatColor.GOLD + "\n Say "+ChatColor.GREEN+ChatColor.BOLD+"SET"+ChatColor.RESET+ChatColor.LIGHT_PURPLE+" to add a Spawn Location for the Blue team");
                    }
                }
                break;
            case 17:
                if (message.equalsIgnoreCase("set")) {
                    if (arena.setSpectatorLocation(player.getLocation())) {
                        response.append(ChatColor.YELLOW + "Congratulations! The arena is done with the setup.\n\nIf there are any increase with the max player in the config, you must add new spawn location for both teams.");
                        event.setCancelled(true);
                        dodgebolt.addArena(arena);
                        step = 18;
                    } else {
                        response.append(ChatColor.RED + "The spectator location must be set inside the arena!\n");
                    }
                }
                break;
        }

        if (response.length() > 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.sendMessage(response.toString());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        dodgebolt.cancelSetup(this);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getFrom().getWorld() != event.getTo().getWorld()) {
            player.sendMessage("Setup has been cancelled due to changing of worlds.");
            dodgebolt.cancelSetup(this);
        }
    }

    @EventHandler
    public void onPlayerBreak(BlockBreakEvent event) {
        if (event.getPlayer() != this.player || event.getPlayer().getItemInHand().getType() != Material.GOLDEN_AXE) {
            return;
        }

        event.setCancelled(true);
        StringBuilder response = new StringBuilder();

        switch (step) {
            case 1:
                firstPos = event.getBlock().getLocation();
                response.append(ChatColor.GOLD + "\nNow, set the second position by breaking a block where the "+ChatColor.GREEN+ChatColor.BOLD+"second position"+ChatColor.RESET+ChatColor.GOLD+" would be");
                step = 2;
                break;
            case 2:
                secondPos = event.getBlock().getLocation();
                response.append(ChatColor.YELLOW + "\nYour first location is located at"+ChatColor.RESET+" x:" + firstPos.getBlockX() + ", y:" + firstPos.getBlockY() + ", z:" + firstPos.getBlockZ() + "\n");
                response.append(ChatColor.YELLOW + "\nYour second location is located at"+ChatColor.RESET+" x:" + secondPos.getBlockX() + ", y:" + secondPos.getBlockY() + ", z:" + secondPos.getBlockZ() + "\n");
                response.append(ChatColor.GOLD + "\nAre you sure with this one or do you want to reset? \n" + ChatColor.GREEN + "Type " + ChatColor.YELLOW + ChatColor.BOLD + "reset" + ChatColor.RESET + "" + ChatColor.GREEN + " if you want to reset or type anything to continue");
                step = 3;
                break;
            case 4:
                firstPos = event.getBlock().getLocation();
                response.append(ChatColor.GOLD + "\nNow, set the second position by breaking a block where the "+ChatColor.GREEN+ChatColor.BOLD+"second position"+ChatColor.RESET+ChatColor.GOLD+" would be");
                step = 5;
                break;
            case 5:
                secondPos = event.getBlock().getLocation();
                response.append(ChatColor.YELLOW + "\nYour first location is located at"+ChatColor.RESET+" x:" + firstPos.getBlockX() + ", z:" + firstPos.getBlockZ() + "\n");
                response.append(ChatColor.YELLOW + "\nYour second location is located at"+ChatColor.RESET+" x:" + secondPos.getBlockX() + ", z:" + secondPos.getBlockZ() + "\n");
                response.append(ChatColor.GOLD + "\nAre you sure with this one or do you want to reset? \n" + ChatColor.GREEN + "Type " + ChatColor.YELLOW + ChatColor.BOLD + "reset" + ChatColor.RESET + "" + ChatColor.GREEN + " if you want to reset or type anything to continue");
                step = 6;
                break;
            case 7:
                firstPos = event.getBlock().getLocation();
                response.append(ChatColor.GOLD + "\nNow, set the second position by breaking a block where the "+ChatColor.GREEN+ChatColor.BOLD+"second position"+ChatColor.RESET+ChatColor.GOLD+" would be");
                step = 8;
                break;
            case 8:
                secondPos = event.getBlock().getLocation();
                response.append(ChatColor.YELLOW + "\nYour first location is located at"+ChatColor.RESET+" x:" + firstPos.getBlockX() + ", z:" + firstPos.getBlockZ() + "\n");
                response.append(ChatColor.YELLOW + "\nYour second location is located at"+ChatColor.RESET+" x:" + secondPos.getBlockX() + ", z:" + secondPos.getBlockZ() + "\n");
                response.append(ChatColor.GOLD + "\nAre you sure with this one or do you want to reset? \n" + ChatColor.GREEN + "Type " + ChatColor.YELLOW + ChatColor.BOLD + "reset" + ChatColor.RESET + "" + ChatColor.GREEN + " if you want to reset or type anything to continue");
                step = 9;
                break;
            case 10:
                firstPos = event.getBlock().getLocation();
                response.append(ChatColor.GOLD + "\nNow, set the second position by breaking a block where the "+ChatColor.GREEN+ChatColor.BOLD+"second position"+ChatColor.RESET+ChatColor.GOLD+" would be");
                step = 11;
                break;
            case 11:
                secondPos = event.getBlock().getLocation();
                if (secondPos.getBlockY() != firstPos.getBlockY()) {
                    player.sendMessage("It must be 1 block high only!");
                } else {
                    response.append(ChatColor.YELLOW + "\nYour first location is located at"+ChatColor.RESET+" x:" +  firstPos.getBlockX() + ", y:" + firstPos.getBlockY() + ", z:" + firstPos.getBlockZ() + "\n");
                    response.append(ChatColor.YELLOW + "\nYour second location is located at"+ChatColor.RESET+" x:" + secondPos.getBlockX() + ", y:" + secondPos.getBlockY() + ", z:" + secondPos.getBlockZ() + "\n");
                    response.append(ChatColor.GOLD + "\nAre you sure with this one or do you want to reset? \n" + ChatColor.GREEN + "Type " + ChatColor.YELLOW + ChatColor.BOLD + "reset" + ChatColor.RESET + "" + ChatColor.GREEN + " if you want to reset or type anything to continue");
                    step = 12;
                }
                break;
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        if (response.length() > 0) {
            player.sendMessage(response.toString());
        }
    }
}
