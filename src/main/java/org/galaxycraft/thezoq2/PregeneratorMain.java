package org.galaxycraft.thezoq2;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.World;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.*;
import java.util.logging.Level;


/**
 * Main class of the project. Loads all config files required and sets up all base objects for later use.
 */

public class PregeneratorMain extends JavaPlugin implements Listener
{
    static String PLUGIN_FOLDER = "plugins/pregenerator/";
    static String STATUS_FILENAME = PLUGIN_FOLDER + "genStatus";
    int radius;
    int currentRadius;
    int maxRadius = 430;
    int centerX = 0;
    int centerY = 0;

    boolean running;
    int taskID;

    World world;

    @Override
    public void onEnable()
    {
        super.onEnable();
        this.getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("Loading pregenerator");

        new File(PLUGIN_FOLDER).mkdirs();

        readStatus();

        BukkitScheduler scheduler = this.getServer().getScheduler();
        //scheduler.scheduleSyncRepeatingTask(this, new RPGUpdateTask(this), 0L, 1L);
        //Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
        
        world = this.getServer().getWorld("world");

        checkGeneratorShouldRun(getServer().getOnlinePlayers().size());
    }
    @Override
    public void onDisable()
    {
        super.onDisable();

        writeStatus();
    }

    /*
     * The generation will follow this pattern. 
        *
        *             currentRadius
        *            ________________
        *
        *      #####+++++++++++++++++####                        
        *      #           *            #                        
        *      #           *            #                        
        *      +           *            +     |                  
        *      +           *            +     |                  
        *      +           *            +     |                  
        *      +************************+     | currentRadius * 2  | radius
        *      +           *            +     |                    |
        *      +           *            +     |                    |
        *      +           *            +                          |
        *      #           *            #                          |
        *      #           *            #                          |
        *      ######++++++++++++++######                          |
        *            ______________
        *
        *            currentRadius*2
     */
    private void startGenerator()
    {
        this.getLogger().info("Starting generator");
        running = true;

        //This variable is needed by the repeating task in order to turn of the generation when it is done
        PregeneratorMain thisPlugin = this;
        taskID = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                int currX = radius;
                int currY = currentRadius;

                generateChunk(radius, -currentRadius);
                generateChunk(radius, currentRadius);

                generateChunk(-currentRadius, -radius);
                generateChunk(currentRadius, -radius);

                generateChunk(-radius, currentRadius);
                generateChunk(-radius, -currentRadius);

                generateChunk(currentRadius, radius);
                generateChunk(-currentRadius, radius);

                currentRadius++;
                if(currentRadius > radius)
                {
                    radius++;
                    currentRadius = 0;

                    if(radius % 10 == 0)
                    {
                        Bukkit.getLogger().info("Generation radius: " + radius);
                    }
                }

                if(radius > maxRadius)
                {
                    Bukkit.getServer().getLogger().info("Max radius reached. Stopping generation");
                    thisPlugin.pauseGenerator();
                }
            }
        }, 4L, 4L);
    }

    private void pauseGenerator()
    {
        this.getServer().getScheduler().cancelTask(taskID);

        this.getLogger().info("Pausing generator");

        running = false;
        writeStatus();
    }


    private void writeStatus()
    {
        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(STATUS_FILENAME));

            writer.append(Integer.toString(radius));
            writer.append("\n");
            writer.append(Integer.toString(currentRadius));

            writer.close();
        }
        catch(IOException e)
        {
            this.getLogger().warning("Failed to read old generation status");
        }
    }

    private void readStatus()
    {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(STATUS_FILENAME));

            radius = Integer.parseInt(reader.readLine());
            currentRadius = Integer.parseInt(reader.readLine());
            reader.close();

            getServer().getLogger().info("Pregenerator starting from radius " + radius);
        }
        catch(IOException e)
        {
            this.getLogger().info("No previous pregenerator file found, starting from 0,0");
            radius = 0;
            currentRadius = 0;
        }
    }

    private void generateChunk(int chunkX, int chunkY)
    {
        if(!world.isChunkLoaded(chunkX, chunkY))
        {
            world.loadChunk(chunkX, chunkY);
            world.unloadChunk(chunkX, chunkY);
        }
    }

    private void checkGeneratorShouldRun(int playerAmount)
    {
        if(playerAmount == 0)
        {
            if(!running)
            {
                startGenerator();
            }
        }
        else
        {
            pauseGenerator();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e)
    {
        checkGeneratorShouldRun(this.getServer().getOnlinePlayers().size());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e)
    {
        checkGeneratorShouldRun(this.getServer().getOnlinePlayers().size() - 1);
    }
}
