package org.nightazure.dodgebolt;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nightazure.dodgebolt.minigame.Team;

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandList implements CommandExecutor, TabCompleter {
    private final Dodgebolt dodge;

    public CommandList(Dodgebolt dodge) {
        this.dodge = dodge;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;
        String playerName = player.getName();

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Dodgebolt Version 0.1");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                sendHelpMessage(player);
                break;
            case "join":
                handleJoinCommand(player, args);
                break;
            case "leave":
                handleLeaveCommand(player);
                break;
            case "arena":
                handleArenaCommand(player, args);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Invalid command. Type '/dodgebolt help' for a list of commands.");
                break;
        }

        return true;
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "arena", "join", "leave"));
        }  else if (args.length == 2 && args[0].equalsIgnoreCase("join") && sender.hasPermission("dodgebolt.join")) {
            completions.addAll(getReadyArenas());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("arena") && sender.hasPermission("dodgebolt.arena")) {
            completions.addAll(Arrays.asList("setup", "info", "list", "setarrow", "resetspawn", "addspawn", "remove", "savearena","setspectator"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("arena") && sender.hasPermission("dodgebolt.arena") && (args[1].equalsIgnoreCase("setarrow")
                || args[1].equalsIgnoreCase("info") || args[1].equalsIgnoreCase("setspectator") || args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("savearena") || args[1].equalsIgnoreCase("addspawn") || args[1].equalsIgnoreCase("resetspawn"))) {
            completions.addAll(getReadyArenas());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("arena") && sender.hasPermission("dodgebolt.arena") && (args[1].equalsIgnoreCase("setarrow")
                || args[1].equalsIgnoreCase("addspawn") || args[1].equalsIgnoreCase("resetspawn"))) {
            completions.addAll(Arrays.asList("red", "blue"));
        }

        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(completion -> !completion.toLowerCase().startsWith(input));
        Collections.sort(completions);

        return completions;
    }
    private void sendHelpMessage(Player player) {
        StringBuilder help = new StringBuilder();
        help.append(ChatColor.YELLOW + "-------------- " + ChatColor.RESET + "Dodgebolt Help" + ChatColor.YELLOW + " -----------\n");
        help.append(ChatColor.GOLD + "/dodgebolt arena: " + ChatColor.RESET + "Get more information about arena creation\n");
        help.append(ChatColor.GOLD + "/dodgebolt join <Arena Name>: " + ChatColor.RESET + "Join an arena!\n");
        help.append(ChatColor.GOLD + "/dodgebolt leave: " + ChatColor.RESET + "Leave the current arena!\n");
        player.sendMessage(help.toString());
    }

    private void handleJoinCommand(Player player, String[] args) {
        if (!player.hasPermission("dodgebolt.join")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Invalid argument! Proper Argument: /dodgebolt join <Arena Name>");
            return;
        }

        String arenaName = args[1];
        DodgeboltArena arena = dodge.getArenaByName(arenaName);

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "That arena does not exist.");
            return;
        }

        if (arena.getStatus() >= 2) {
            player.sendMessage(ChatColor.RED + "That arena is currently unavailable.");
            return;
        }

        if (dodge.containsPlayer(player)) {
            player.sendMessage(ChatColor.RED + "You have already joined the queue.");
            return;
        }

        arena.playerJoin(player, null);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void handleLeaveCommand(Player player) {
        for (DodgeboltArena arena : dodge.arenas) {
            if (arena.getPlayers().contains(player)) {
                if (arena.getStatus() >= 2) {
                    player.sendMessage(ChatColor.RED + "You cannot leave while the game is ongoing.");
                    return;
                } else {
                    arena.playerLeave(player);
                    player.sendMessage(ChatColor.GREEN + "You left the queue!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    return;
                }
            }
        }
        player.sendMessage(ChatColor.RED + "You are not in any game!");
    }

    private void handleArenaCommand(Player player, String[] args) {
        if (!player.hasPermission("dodgebolt.arena")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return;
        }

        if (args.length == 1) {
            sendArenaHelpMessage(player);
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "setup":
                dodge.setupArena(player);
                break;
            case "info":
                handleArenaInfoCommand(player, args);
                break;
            case "list":
                sendArenaList(player);
                break;
            case "setarrow":
                handleSetArrowCommand(player, args);
                break;
            case "resetspawn":
                handleResetSpawnCommand(player, args);
                break;
            case "setspectator":
                handleSetSpectatorCommand(player, args);
                break;
            case "savearena":
                handleSavePlaneCommand(player, args);
                break;
            case "addspawn":
                handleAddSpawnCommand(player, args);
                break;
            case "remove":
                handleRemoveCommand(player, args);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Invalid sub-command. Type '/dodgebolt arena' for a list of arena commands.");
                break;
        }
    }

    private void sendArenaHelpMessage(Player player) {
        StringBuilder help = new StringBuilder();
        help.append(ChatColor.YELLOW + "-------------- " + ChatColor.RESET + "Dodgebolt Help" + ChatColor.YELLOW + " -----------\n");
        help.append(ChatColor.GOLD + "/dodgebolt arena setup: " + ChatColor.RESET + "Start an arena setup\n");
        help.append(ChatColor.GOLD + "/dodgebolt arena info <Arena Name>: " + ChatColor.RESET + "Get the information of an arena\n");
        help.append(ChatColor.GOLD + "/dodgebolt arena savearena <Arena Name>: " + ChatColor.RESET + "Save the build data of your arena field. (Must do this if you change your arena build)\n");
        help.append(ChatColor.GOLD + "/dodgebolt arena remove <Arena Name>: " + ChatColor.RESET + "Remove an arena\n");
        help.append(ChatColor.GOLD + "/dodgebolt arena setspectator <Arena Name>: " + ChatColor.RESET + "Remove an arena\n");
        help.append(ChatColor.GOLD + "/dodgebolt arena resetspawn <Arena Name> (red|blue): " + ChatColor.RESET + "Reset the spawn location/s of a team\n");
        help.append(ChatColor.GOLD + "/dodgebolt arena addspawn <Arena Name> (red|blue): " + ChatColor.RESET + "Add a spawn location to a team at your location\n");
        help.append(ChatColor.GOLD + "/dodgebolt arena setarrow <Arena Name> (red|blue): " + ChatColor.RESET + "Set the arrow location of a team\n");
        help.append(ChatColor.GOLD + "/dodgebolt arena list: " + ChatColor.RESET + "List all the arenas");
        player.sendMessage(help.toString());
    }
    private void handleArenaInfoCommand(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(ChatColor.RED + "Invalid argument! Proper Argument: /dodgebolt arena info <Arena Name>");
            return;
        }

        String arenaName = args[2];
        DodgeboltArena arena = dodge.getArenaByName(arenaName);

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "The arena \"" + arenaName + "\" does not exist!");
            return;
        }

        StringBuilder info = new StringBuilder();
        info.append(ChatColor.GOLD + "Arena Name: " + ChatColor.LIGHT_PURPLE + arena.getName() + "\n");
        info.append(arena.isArenaReady() ? ChatColor.GOLD + "Status: " + ChatColor.GREEN + ChatColor.BOLD + "READY\n" :
                ChatColor.GOLD + "Ready: " + ChatColor.RED + ChatColor.BOLD + "NOT READY\n");
        info.append(ChatColor.GOLD + "Red Spawn Locations: \n");
        for (Location location : arena.getSpawnLocation(arena.getTeamByName("Red"))) {
            info.append(ChatColor.RESET + "- " + ChatColor.LIGHT_PURPLE + "x:" + location.x() + ", y:" + location.y() + ", z:" + location.z() + "\n");
        }
        info.append(ChatColor.GOLD + "Blue Locations: \n");
        for (Location location : arena.getSpawnLocation(arena.getTeamByName("Blue"))) {
            info.append(ChatColor.RESET + "- " + ChatColor.LIGHT_PURPLE + "x:" + location.x() + ", y:" + location.y() + ", z:" + location.z() + "\n");
        }
        if(arena.spectatorLocation==null)info.append(ChatColor.GOLD + "Spectator Location: Set your Spectator Location through \"/dodgebolt arena setspectator <Arena Name>\"\n");
        else info.append(ChatColor.GOLD + "Spectator Location: " + ChatColor.LIGHT_PURPLE + "x:" + arena.spectatorLocation.x() + ", y:" + arena.spectatorLocation.y() + ", z:" + arena.spectatorLocation.z() + "\n");
        info.append(ChatColor.GOLD + "Red Arrow Location: " + ChatColor.LIGHT_PURPLE + "x:" + arena.redArrow.x() + ", y:" + arena.redArrow.y() + ", z:" + arena.redArrow.z() + "\n");
        info.append(ChatColor.GOLD + "Blue Arrow Location: " + ChatColor.LIGHT_PURPLE + "x:" + arena.blueArrow.x() + ", y:" + arena.blueArrow.y() + ", z:" + arena.blueArrow.z() + "\n");
        info.append(ChatColor.GOLD + "Arena Region: \n");
        info.append(ChatColor.RESET + "Position 1 - " + ChatColor.LIGHT_PURPLE + "x:" + arena.getArenaPos().getKey().x() + ", y:" + arena.getArenaPos().getKey().y() + ", z:" + arena.getArenaPos().getKey().z() + "\n");
        info.append(ChatColor.RESET + "Position 2 - " + ChatColor.LIGHT_PURPLE + "x:" + arena.getArenaPos().getValue().x() + ", y:" + arena.getArenaPos().getValue().y() + ", z:" + arena.getArenaPos().getValue().z() + "\n");
        info.append(ChatColor.GOLD + "Red Region: \n");
        info.append(ChatColor.RESET + "Position 1 - " + ChatColor.LIGHT_PURPLE + "x:" + arena.redTeam.getKey().x() + ", y:" + arena.redTeam.getKey().y() + ", z:" + arena.redTeam.getKey().z() + "\n");
        info.append(ChatColor.RESET + "Position 2 - " + ChatColor.LIGHT_PURPLE + "x:" + arena.redTeam.getValue().x() + ", y:" + arena.redTeam.getValue().y() + ", z:" + arena.redTeam.getValue().z() + "\n");
        info.append(ChatColor.GOLD + "Blue Region: \n");
        info.append(ChatColor.RESET + "Position 1 - " + ChatColor.LIGHT_PURPLE + "x:" + arena.blueTeam.getKey().x() + ", y:" + arena.blueTeam.getKey().y() + ", z:" + arena.blueTeam.getKey().z() + "\n");
        info.append(ChatColor.RESET + "Position 2 - " + ChatColor.LIGHT_PURPLE + "x:" + arena.blueTeam.getValue().x() + ", y:" + arena.blueTeam.getValue().y() + ", z:" + arena.blueTeam.getValue().z() + "\n");
        player.sendMessage(info.toString());
    }

    private void sendArenaList(Player player) {
        StringBuilder list = new StringBuilder("List of arenas: \n");

        if (dodge.arenas != null) {
            for (DodgeboltArena arena : dodge.arenas) {
                list.append(ChatColor.RESET+"- ")
                        .append(ChatColor.GOLD)
                        .append(arena.getName()+ChatColor.RESET+" ->")
                        .append(arena.isArenaReady() ? ChatColor.GREEN : ChatColor.RED)
                        .append(ChatColor.BOLD)
                        .append(arena.isArenaReady() ? " READY" : " NOT READY")
                        .append("\n");
            }
        }

        player.sendMessage(list.toString());
    }
    private void handleSetSpectatorCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Invalid argument! Proper Argument: /dodgebolt arena setspectator <Arena Name>");
            return;
        }
        String arenaName = args[2];
        DodgeboltArena arena = dodge.getArenaByName(arenaName);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "The arena \"" + arenaName + "\" does not exist!");
            return;
        }

        arena.setSpectatorLocation(player.getLocation());
        player.sendMessage(ChatColor.GREEN+"The Spectator Location has been set!");
        arena.generateConfig(dodge);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }
    private void handleSetArrowCommand(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Invalid argument! Proper Argument: /dodgebolt arena setarrow <Arena Name> (red/blue)");
            return;
        }

        String arenaName = args[2];
        String teamName = args[3];
        DodgeboltArena arena = dodge.getArenaByName(arenaName);

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "The arena \"" + arenaName + "\" does not exist!");
            return;
        }

        if (!teamName.equalsIgnoreCase("red") && !teamName.equalsIgnoreCase("blue")) {
            player.sendMessage(ChatColor.RED + "Invalid team name! Use 'red' or 'blue'.");
            return;
        }

        Team team = arena.getTeamByName(teamName);
        Location playerLocation = player.getLocation();

        if (team == null) {
            player.sendMessage(ChatColor.RED + "The team \"" + teamName + "\" does not exist in the arena \"" + arenaName + "\"!");
            return;
        }

        if (teamName.equalsIgnoreCase("red")) {
            arena.setRedArrow(playerLocation);
            arena.addSpawnLocation(team, playerLocation);
            player.sendMessage(ChatColor.GREEN + "New Arrow Spawn Location has been added to team " + ChatColor.BOLD + ChatColor.RED + "Red" + ChatColor.RESET + ChatColor.GREEN + "!");
        } else {
            arena.setBlueArrow(playerLocation);
            player.sendMessage(ChatColor.GREEN + "New Arrow Spawn Location has been added to team " + ChatColor.BOLD + ChatColor.BLUE + "Blue" + ChatColor.RESET + ChatColor.GREEN + "!");
        }

        arena.generateConfig(dodge);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }
    private void handleSavePlaneCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Invalid argument! Proper Argument: /dodgebolt arena savearena <Arena Name>");
            return;
        }

        String arenaName = args[2];
        DodgeboltArena arena = dodge.getArenaByName(arenaName);

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "The arena \"" + arenaName + "\" does not exist!");
            return;
        }
        arena.savePlane();
        player.sendMessage(ChatColor.GREEN + "The arena state has now been saved.");
    }
    private void handleResetSpawnCommand(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Invalid argument! Proper Argument: /dodgebolt arena resetspawn <Arena Name> (red/blue)");
            return;
        }

        String arenaName = args[2];
        String teamName = args[3];
        DodgeboltArena arena = dodge.getArenaByName(arenaName);

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "The arena \"" + arenaName + "\" does not exist!");
            return;
        }

        if (!teamName.equalsIgnoreCase("red") && !teamName.equalsIgnoreCase("blue")) {
            player.sendMessage(ChatColor.RED + "Invalid team name! Use 'red' or 'blue'.");
            return;
        }

        Team team = arena.getTeamByName(teamName);

        if (team == null) {
            player.sendMessage(ChatColor.RED + "The team \"" + teamName + "\" does not exist in the arena \"" + arenaName + "\"!");
            return;
        }

        if (teamName.equalsIgnoreCase("red")) {
            arena.clearSpawnLocation(team);
            player.sendMessage(ChatColor.GREEN + "The spawn locations of team " + ChatColor.BOLD + ChatColor.RED + "Red" + ChatColor.RESET + ChatColor.GREEN + " are now cleared");
        } else {
            arena.clearSpawnLocation(team);
            player.sendMessage(ChatColor.GREEN + "The spawn locations of team " + ChatColor.BOLD + ChatColor.BLUE + "Blue" + ChatColor.RESET + ChatColor.GREEN + " are now cleared");
        }

        arena.generateConfig(dodge);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void handleAddSpawnCommand(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Invalid argument! Proper Argument: /dodgebolt arena addspawn <Arena Name> (red/blue)");
            return;
        }

        String arenaName = args[2];
        String teamName = args[3];
        DodgeboltArena arena = dodge.getArenaByName(arenaName);

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "The arena \"" + arenaName + "\" does not exist!");
            return;
        }

        if (!teamName.equalsIgnoreCase("red") && !teamName.equalsIgnoreCase("blue")) {
            player.sendMessage(ChatColor.RED + "Invalid team name! Use 'red' or 'blue'.");
            return;
        }

        Team team = arena.getTeamByName(teamName);

        if (team == null) {
            player.sendMessage(ChatColor.RED + "The team \"" + teamName + "\" does not exist in the arena \"" + arenaName + "\"!");
            return;
        }

        if (arena.getSpawnLocation(team).size() >= team.maxPlayer) {
            player.sendMessage(ChatColor.RED + "There are already enough spawn locations for this team!");
            return;
        }

        Location playerLocation = player.getLocation();
        arena.addSpawnLocation(team, playerLocation);
        player.sendMessage(ChatColor.GREEN + "New Spawn Location has been added to team " + ChatColor.BOLD + (teamName.equalsIgnoreCase("red") ? ChatColor.RED : ChatColor.BLUE) + teamName + ChatColor.RESET + ChatColor.GREEN + "!");

        arena.generateConfig(dodge);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void handleRemoveCommand(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(ChatColor.RED + "Invalid argument! Proper Argument: /dodgebolt arena remove <Arena Name>");
            return;
        }

        String arenaName = args[2];
        DodgeboltArena arena = dodge.getArenaByName(arenaName);

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "The arena \"" + arenaName + "\" does not exist!");
            return;
        }

        if (dodge.deleteArena(arena)) {
            File file = new File(dodge.getDataFolder(),
                    "/arenas/"+arena.getName()+".yml");
            if (file.exists()){
                try{
                    file.delete();
                } catch (Exception e){}
            }
            File file2 = new File(dodge.getDataFolder(),
                    "/arenas/"+arena.getName()+".dat");
            if (file2.exists()){
                try{
                    file2.delete();
                } catch (Exception e){}
            }
            dodge.arenas.remove(arena);
            player.sendMessage(ChatColor.GREEN + "The arena \"" + arenaName + "\" has been removed");
        } else {
            player.sendMessage(ChatColor.RED + "The arena \"" + arenaName + "\" does not exist!");
        }
    }

    private List<String> getReadyArenas() {
        List<String> readyArenas = new ArrayList<>();

        if (dodge.arenas != null) {
            for (DodgeboltArena arena : dodge.arenas) {
                if (arena.isArenaReady()) {
                    readyArenas.add(arena.getName());
                }
            }
        }

        return readyArenas;
    }

}
