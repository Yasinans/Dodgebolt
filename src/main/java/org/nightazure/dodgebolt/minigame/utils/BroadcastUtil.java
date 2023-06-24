package org.nightazure.dodgebolt.minigame.utils;

import org.bukkit.entity.Player;

import java.util.List;

public class BroadcastUtil {

    public static void titleBroadcast(String title, String subtitle, List<Player> players){
        for(Player p : players){
            p.sendTitle(title, subtitle);
        }
    }
}
