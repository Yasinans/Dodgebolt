package org.nightazure.dodgebolt.minigame.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class Config {
    private static File file;
    private static FileConfiguration config;
    private final String fileName;
    private final String path;
    private final Plugin plugin;


    public Config(String name, String path, Plugin plugin){
        if(name == null){
            throw new IllegalArgumentException("name must not be null");
        } else if(path == null){
            //lol
        }else if(path.startsWith("/") || path.endsWith("/")){
            throw new IllegalArgumentException("path must not start with / or ends with /");
        }
        this.fileName = name;
        this.path = path;
        this.plugin = plugin;
    }
    public void setup(){
        if(this.path == null){
            file = new File(Bukkit.getServer().getPluginManager().getPlugin(this.plugin.getName()).getDataFolder(),
                    fileName+".yml");
        } else{
        file = new File(Bukkit.getServer().getPluginManager().getPlugin(this.plugin.getName()).getDataFolder(),
                "/"+path+"/"+fileName+".yml");}
        if (!file.exists()){
            try{
                file.createNewFile();
            } catch (IOException e){}
        }
        config =  YamlConfiguration.loadConfiguration(file);
    }
    public static FileConfiguration get(){
        return config;
    }
    public static void save(){
        try {
            config.save(file);
        } catch (IOException e){
            System.out.println("Failed to save configuration");
        }
    }
    public static void reload(){
        config = YamlConfiguration.loadConfiguration(file);
    }
}
