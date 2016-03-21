package org.galaxycraft.thezoq2;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.World;

import java.io.*;
import java.util.logging.Level;


/**
 * Main class of the project. Loads all config files required and sets up all base objects for later use.
 */

public class PregeneratorMain extends JavaPlugin implements Listener
{
    static String STATUS_FILENAME = "genStatus";
    int radius;
    int currentRadius;
    int maxRadius = 15000;
    int centerX = 0;
    int centerY = 0;

    int taskID;

    World world;

    @Override
    public void onEnable()
    {
        super.onEnable();

        getLogger().info("Loading pregenerator");

        BukkitScheduler scheduler = this.getServer().getScheduler();
        //scheduler.scheduleSyncRepeatingTask(this, new RPGUpdateTask(this), 0L, 1L);
        //Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
        
        world = this.getServer().getWorld("world");

        //Nobody is on, start generating chunks
        if(getServer().getOnlinePlayers().size() == 0)
        {
            startGenerator();
        }
    }

    /*
     * The generation will follow this pattern
     *
     *             currentRadius
     *          __________________
     *
     *      ####******************####                       |
     *      #                        #                       |
     *      #                        #                       |
     *      #                        #                       |
     *      *                        *     |                 |
     *      *                        *     |                 |
     *      *           *            *     | currentRadius   | radius
     *      *                        *     |                 |
     *      *                        *     |                 |
     *      #                        #                       |
     *      #                        #                       |
     *      #                        #                       |
     *      ####******************####                       |
     */
    void startGenerator()
    {
        this.getLogger().info("Starting generator");

        taskID = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                int currX = radius;
                int currY = currentRadius;

                generateChunk(currX, currY);
                generateChunk(-currX, currY);
                generateChunk(currY, currX);
                generateChunk(currY, -currX);

                currentRadius++;
                if(currentRadius > radius)
                {
                    radius++;
                    currentRadius = 0;
                }

                getLogger().info("Generating: " + currX + " " + currY);
            }
        }, 4L, 4L);
    }

    void stopGenerator()
    {
        this.getServer().getScheduler().cancelTask(taskID);

        writeStatus();
    }


    void writeStatus()
    {
        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(STATUS_FILENAME));

            writer.append(Integer.toString(radius));
            writer.append(Integer.toString(currentRadius));

            writer.close();
        }
        catch(IOException e)
        {
            this.getLogger().warning("Failed to read old generation status");
        }
    }

    void readStatus()
    {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(STATUS_FILENAME));

            radius = Integer.parseInt(reader.readLine());
            currentRadius = Integer.parseInt(reader.readLine());
            reader.close();
        }
        catch(IOException e)
        {
            this.getLogger().info("No previous pregenerator file found, starting from 0,0");
            radius = 0;
            currentRadius = 0;
        }
    }

    void generateChunk(int chunkX, int chunkY)
    {
        if(!world.isChunkLoaded(chunkX, chunkY))
        {
            world.loadChunk(chunkX, chunkY);
            world.unloadChunk(chunkX, chunkY);
        }
    }
}
