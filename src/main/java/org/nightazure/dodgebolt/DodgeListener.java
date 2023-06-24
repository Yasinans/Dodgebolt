package org.nightazure.dodgebolt;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nightazure.dodgebolt.minigame.Team;
import org.nightazure.dodgebolt.minigame.events.ArenaEndEvent;
import org.nightazure.dodgebolt.minigame.utils.BroadcastUtil;
import org.nightazure.dodgebolt.minigame.Arena;
import org.nightazure.dodgebolt.minigame.events.ArenaStartEvent;
import org.nightazure.dodgebolt.minigame.utils.BlockUtil;
import org.nightazure.dodgebolt.minigame.utils.EntityUtil;
import org.nightazure.dodgebolt.minigame.utils.LocationUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("deprecation")
public class DodgeListener implements Listener {

    Dodgebolt dodge;
    public HashMap<Arena, DodgeboltStatus> dodgeboltStat;

    public DodgeListener(Dodgebolt dodge) {
        this.dodge = dodge;
        this.dodgeboltStat = new HashMap<>();
    }

    @EventHandler
    public void onGameEnd(ArenaEndEvent event) {
        DodgeboltArena arena = dodge.getArenaByName(event.getArena().getName());
        dodgeboltStat.remove(arena);
        List<Player> players = arena.getPlayers();
        for (Player player : players) {
            arena.getPlayerManager().getPlayerInfo(player).loadState(true);
            player.playSound(player.getLocation(),Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST,1f,1f);
        }
        EntityUtil.removeEntities(arena.getArenaPos().getKey(),arena.getArenaPos().getValue());
        BlockUtil.deserializeBlocks(arena.getPlaneData());
        if(event.getWinningTeam()!= null)arena.broadcastPlayer(ChatColor.GREEN + "The " + ChatColor.BOLD+event.getWinningTeam().getColor() + event.getWinningTeam().getName() + ChatColor.RESET+ChatColor.GREEN + " team has won the game!", null);
        arena.getPlayerManager().clearPlayers();

    }

    @EventHandler
    public void onGameStart(ArenaStartEvent event) {
        Arena arena = event.getArena();
        EntityUtil.removeEntities(arena.getArenaPos().getKey(),arena.getArenaPos().getValue());
        dodgeboltStat.put(arena, new DodgeboltStatus());
        for (Player player : arena.getPlayers()) {
            player.getInventory().setItem(0, new ItemStack(Material.BOW));
            dodgeboltStat.get(arena).inventories.add(new GameInventory(player, 0));
        }
        startTimer(dodge.getArenaByName(arena.getName()));
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if(event.getEntity() instanceof Player victim) {
            if (containsPlayer(victim) && getArena(victim).getStatus() >= 2) {
                event.setCancelled(true);
                if (event.getDamager() instanceof Arrow arrow) {
                    if (arrow.getShooter() instanceof Player shooter) {
                        if (containsPlayer(victim) && containsPlayer(shooter)) {
                            DodgeboltArena victimArena = getArena(victim);
                            if (victimArena.getPlayerTeam(victim) != victimArena.getPlayerTeam(shooter)) {
                                firework(victim);
                                //this doesnt support multiple team btw
                                if(victimArena.getPlayerTeam(victim).getPlayers().size() != 1)shrinkArena(victimArena);
                                victimArena.handleElimination(victim, 1, shooter);
                            }
                        }
                    }
                }
            }
        }
    }
    @EventHandler
    public void onPlayerShoot(ProjectileHitEvent event){
        if(event.getEntity() instanceof Arrow arrow){
            if(arrow.getShooter() instanceof Player shooter){
                if(containsPlayer(shooter)){
                    DodgeboltArena arena = getArena(shooter);
                    if(event.getHitBlock() != null){
                            new BukkitRunnable() {
                            int count = 4;

                            @Override
                            public void run() {
                                if (arena.getStatus() <= 1) cancel();
                                if (count <= 0) {
                                    if(!arrow.isDead()) {
                                        Team playerTeam = arena.getPlayerTeam(shooter);
                                        Team targetTeam = playerTeam.getName().equals("Red") ?
                                                arena.getTeamByName("Blue") : arena.getTeamByName("Red");
                                        arena.dropArrow(targetTeam);
                                        arrow.remove();
                                    }
                                    cancel();
                                }
                                count--;
                            }
                        }.runTaskTimer(dodge, 0, 20);
                    }
                }
            }
        }
    }
    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (containsPlayer(event.getPlayer())) {
            DodgeboltArena arena = getArena(player);
            if (arena.getStatus() == 2) event.setCancelled(true);
            else if (arena.getStatus() == 3) {
                if (event.getItemDrop().getItemStack().equals(new ItemStack(Material.BOW))) {
                    player.sendMessage(ChatColor.RED + "You can't drop your bow!");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f);
                    event.setCancelled(true);
                } else if(event.getItemDrop().getItemStack().equals(new ItemStack(Material.ARROW))){
                        new BukkitRunnable() {
                            int count = 5;
                            @Override
                            public void run() {
                                if (arena.getStatus() <= 1) cancel();
                                if (count <= 0) {
                                    event.getItemDrop().setUnlimitedLifetime(true);
                                    if(!event.getItemDrop().isDead()) {
                                        Team playerTeam = arena.getPlayerTeam(player);
                                        Team targetTeam = playerTeam.getName().equals("Red") ?
                                                arena.getTeamByName("Blue") : arena.getTeamByName("Red");
                                        arena.dropArrow(targetTeam);
                                        event.getItemDrop().remove();
                                    }
                                    cancel();
                                }
                                count--;
                            }
                        }.runTaskTimer(dodge, 0, 20);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (containsPlayer(event.getPlayer())) {
            DodgeboltArena arena = getArena(player);
            if (arena.getStatus() == 3) {
                Team playerTeam = arena.getPlayerTeam(player);
                GameInventory playerInventory = getPlayerInventory(arena, player);
                if((arena.planeArea.getKey().y()-4)>player.getLocation().y()){
                    if(arena.getPlayerTeam(player).getPlayers().size() != 1 && arena.getRemainingTeams().size()!=2)shrinkArena(arena);
                    arena.handleElimination(player, 2, null);
                } else if (dodge.allowTeamPass) {
                    if (playerTeam.getName().equalsIgnoreCase("Red")) {
                        if (LocationUtil.isInside(arena.blueTeam.getKey(), arena.blueTeam.getValue(), player.getLocation())) {
                            if (!playerInventory.getIsOver()) {
                                playerInventory.setIsOver(true);
                                playerInventory.setArrow(playerInventory.getArrowAmount());
                                player.getInventory().clear();
                                player.getInventory().setItem(0, new ItemStack(Material.ARROW, playerInventory.getArrow()));
                            }
                        } else if (playerInventory.getIsOver()) {
                            playerInventory.setArrow(playerInventory.getArrowAmount());
                            player.getInventory().clear();
                            player.getInventory().setItem(1, new ItemStack(Material.ARROW, playerInventory.getArrow()));
                            player.getInventory().setItem(0, new ItemStack(Material.BOW));
                            playerInventory.setIsOver(false);
                        }
                    } else {
                        if (LocationUtil.isInside(arena.redTeam.getKey(), arena.redTeam.getValue(), player.getLocation())) {
                            if (!playerInventory.getIsOver()) {
                                playerInventory.setIsOver(true);
                                playerInventory.setArrow(playerInventory.getArrowAmount());
                                player.getInventory().clear();
                                player.getInventory().setItem(0, new ItemStack(Material.ARROW, playerInventory.getArrow()));
                            }
                        } else if (playerInventory.getIsOver()) {
                            playerInventory.setArrow(playerInventory.getArrowAmount());
                            player.getInventory().clear();
                            player.getInventory().setItem(1, new ItemStack(Material.ARROW, playerInventory.getArrow()));
                            player.getInventory().setItem(0, new ItemStack(Material.BOW));
                            playerInventory.setIsOver(false);
                        }
                    }
                } else {
                    if (playerTeam.getName().equalsIgnoreCase("Red")) {
                        if (LocationUtil.isInside(arena.blueTeam.getKey(), arena.blueTeam.getValue(), player.getLocation())) {
                            Location from = event.getFrom();
                            Vector direction = from.getDirection().normalize();
                            Location adjustedLocation = from.clone().add(direction.multiply(-0.5));

                            event.setTo(adjustedLocation);
                        }
                    } else if (playerTeam.getName().equalsIgnoreCase("Blue")){
                        if (LocationUtil.isInside(arena.redTeam.getKey(), arena.redTeam.getValue(), player.getLocation())) {
                            Location from = event.getFrom();
                            Vector direction = from.getDirection().normalize();
                            Location adjustedLocation = from.clone().add(direction.multiply(-0.5));

                            event.setTo(adjustedLocation);
                        }
                    }
                }
            } else if (arena.getStatus() == 2) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onPlayerBreak(BlockBreakEvent event){
        if (containsPlayer(event.getPlayer())) {
            DodgeboltArena arena = getArena(event.getPlayer());
            if (arena.getStatus() >= 2) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event){
        if (containsPlayer(event.getPlayer())) {
            DodgeboltArena arena = getArena(event.getPlayer());
            if (arena.getStatus() >= 2) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED+"You can't execute this command while in-game!");
            }
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (containsPlayer(event.getPlayer())) {
            getArena(player).playerLeave(player);
        }
    }
    public void shrinkArena(DodgeboltArena arena){
        if(!dodge.disableShrink) {
            BroadcastUtil.titleBroadcast(ChatColor.RED + "" + ChatColor.BOLD + "The arena is shrinking!", "", arena.getPlayers());
            int offset = dodgeboltStat.get(arena).getEliminatedCount();
            dodgeboltStat.get(arena).setEliminatedCount(offset + 1);
            new BukkitRunnable() {
                int count = 15;

                @Override
                public void run() {
                    if (count <= 0) {
                        for (Player p : arena.getPlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                        }
                        List<Block> blocks = BlockUtil.changeBlockSides(arena.planeArea.getKey(), arena.planeArea.getValue(), Material.AIR, 0);
                        for (Block block : blocks) {
                            block.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, block.getLocation(), 0);
                        }
                        cancel();
                    } else if (count == 14) {
                        BlockUtil.changeBlockSides(arena.planeArea.getKey(), arena.planeArea.getValue(), Material.OBSIDIAN, 0);
                        for (Player p : arena.getPlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 1f);
                        }
                    } else {
                        for (Player p : arena.getPlayers()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
                        }
                    }
                    count--;
                }
            }.runTaskTimer(dodge, 0, 6);
        }
    }
    public void startTimer(DodgeboltArena arena) {
        new BukkitRunnable() {
            int count = 10;
            @Override
            public void run() {
                if (arena.getStatus() <= 1) cancel();
                if (count <= 0) {
                    for (Team team : arena.teams.keySet()) {
                        arena.dropArrow(team);
                    }
                    cancel();
                }
                count--;
            }
        }.runTaskTimer(dodge, 0, 20);
    }

    public boolean containsPlayer(Player player) {
        for (DodgeboltArena arena : dodge.arenas) {
            if (arena.getPlayers().contains(player)) {
                return true;
            }
        }
        return false;
    }

    public DodgeboltArena getArena(Player player) {
        for (DodgeboltArena arena : dodge.arenas) {
            if (arena.getPlayers().contains(player)) {
                return arena;
            }
        }
        return null;
    }
    public void firework(Player player) {
        Location location = player.getLocation();
        location.setY(location.getY() + 2);
        Firework firework = player.getWorld().spawn(location, Firework.class);
        FireworkMeta data = (FireworkMeta) firework.getFireworkMeta();
        data.addEffects(FireworkEffect.builder().withColor(Color.RED).withTrail().with(FireworkEffect.Type.BALL).build());
        data.setPower(0);
        firework.setFireworkMeta(data);

        // Set the fuse ticks to 0 to make the firework explode immediately
        firework.detonate();

    }
    public GameInventory getPlayerInventory(Arena arena, Player p) {
        for (GameInventory inventory : dodgeboltStat.get(arena).inventories) {
            if (inventory.getPlayer().equals(p)) {
                return inventory;
            }
        }
        return null;
    }

    private class GameInventory {
        int arrow;
        boolean isOver;
        Player player;

        GameInventory(Player player, int arrow) {
            this.player = player;
            this.isOver = false;
            this.arrow = arrow;
        }

        boolean getIsOver() {
            return isOver;
        }

        void setIsOver(boolean isOver) {
            this.isOver = isOver;
        }

        int getArrowAmount() {
            int amount = 0;
            for (int i = 0; i < 36; i++) {
                ItemStack slot = this.player.getInventory().getItem(i);
                if (slot == null || !slot.isSimilar(new ItemStack(Material.ARROW)))
                    continue;
                amount += slot.getAmount();
            }
            return amount;
        }

        Player getPlayer() {
            return player;
        }

        void setArrow(int arrow) {
            this.arrow = arrow;
        }

        int getArrow() {
            return arrow;
        }
    }
    public class DodgeboltStatus{
        public List<GameInventory> inventories;
        int eliminatedCount;
        public DodgeboltStatus(){
            this.inventories = new ArrayList<>();
            eliminatedCount = 0;
        }
        int getEliminatedCount(){
            return eliminatedCount;
        }
        void setEliminatedCount(int count) {
            this.eliminatedCount = count;
        }
    }
}
