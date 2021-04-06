/*
 * Copyright (C) 2021 M0rica
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package holeFiller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
/**
 *
 * @author M0rica
 */
public class HoleFiller extends JavaPlugin{
    
    Logger log = this.getLogger();
    Random rand = new Random();
    
    World world;
    
    List<Location> blocks;
    List<Location> newBlocks;
    int runID;
    
    int blocksPlaced;
    boolean isRunning = false;
    int allBlocksPlaced;
    
    Material[] notPlaceMaterials = new Material[]{Material.AIR, Material.CAVE_AIR};
    List<Material> materialsToReplace = Arrays.asList(new Material[]{Material.AIR, Material.CAVE_AIR, Material.WATER, Material.LAVA, Material.GRASS, Material.TALL_GRASS, Material.DEAD_BUSH,
                    Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM, Material.AZURE_BLUET, Material.OXEYE_DAISY, Material.CORNFLOWER, 
                    Material.LILY_OF_THE_VALLEY, Material.WITHER_ROSE, Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, 
                    Material.PEONY, Material.WHITE_TULIP, Material.ORANGE_TULIP, Material.RED_TULIP, Material.PINK_TULIP, Material.SEAGRASS, Material.KELP, Material.SNOW});
    List<Material> materialsToIgnore = Arrays.asList(new Material[]{Material.AIR, Material.CAVE_AIR, Material.BEDROCK, Material.GRASS, Material.TALL_GRASS, 
                    Material.DEAD_BUSH, Material.WATER, Material.LAVA, Material.CAVE_AIR, Material.DANDELION, Material.POPPY, 
                    Material.BLUE_ORCHID, Material.ALLIUM, Material.AZURE_BLUET, Material.OXEYE_DAISY, Material.CORNFLOWER, 
                    Material.LILY_OF_THE_VALLEY, Material.WITHER_ROSE, Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, 
                    Material.PEONY, Material.WHITE_TULIP, Material.ORANGE_TULIP, Material.RED_TULIP, Material.PINK_TULIP, Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG, 
                    Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.CRIMSON_HYPHAE, Material.WARPED_HYPHAE, 
                    Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES, Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES,
                    Material.DARK_OAK_LEAVES, Material.VINE, Material.SNOW, Material.SEAGRASS, Material.KELP});
    
    HashSet<Material> materialsToIgnoreFindingStart = new HashSet<>(materialsToIgnore);
    
    int maxDistance = 6;
    int blocksPerTick = 10;
    
    @Override
    public void onEnable(){
        world = Bukkit.getWorlds().get(0);
        log.info("Plugin enabled");
    }
    
    @Override
    public void onDisable(){
        log.info("Plugin disabled");
    }
    
    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] args) {
        if(args.length == 3){
            fill(new Location(world, Double.parseDouble(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2])));
            return true;
        } else if(args.length == 0){
            Player player = (Player) cs;
            Location loc = player.getTargetBlock(materialsToIgnoreFindingStart, 10).getLocation();
            loc.setY(loc.getY()+1);
            if(isRunning){
                stop();
            }
            fill(loc);
            return true;
        } else if(args[0].equalsIgnoreCase("stop")){
            if(isRunning){
                stop();
            }
            return true;
        }
        return false;
    }
    
    public void stop(){
        Bukkit.getScheduler().cancelTask(runID);
        isRunning = false;
        if(allBlocksPlaced == 0){
            broadcastMsg("No hole detected, nothing was filled.");
        } else {
            broadcastMsg(String.format("Done! (%d blocks filled)", allBlocksPlaced));
        }
    }
    
    
    public void broadcastMsg(String msg){
        Bukkit.getScheduler().runTask(this, new Runnable(){
            @Override
            public void run(){
                Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "[AutoFillHole] " + ChatColor.LIGHT_PURPLE + msg);
            }
        });
    }
    
    public void broadcastErr(String msg){
        Bukkit.getScheduler().runTask(this, new Runnable(){
            @Override
            public void run(){
                Bukkit.broadcastMessage(ChatColor.DARK_RED + "[AutoFillHole] " + ChatColor.RED + msg);
            }
        });
    }
    
    
    private void fill(Location start){
        //log.info(materialsToReplace.toString());
        isRunning = true;
        blocks = new ArrayList<>();
        blocks.add(start);
        allBlocksPlaced = 0;
        broadcastMsg("Starting to fill");
        this.runID = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
            @Override
            public void run(){
                blocksPlaced = 0;
                newBlocks = new ArrayList<>();
                if(!blocks.isEmpty()){
                    while(!blocks.isEmpty() && blocksPlaced < blocksPerTick){
                        fillBlock(getNextBlock());
                        blocksPlaced++;
                    }
                    blocks.addAll(newBlocks);
                } else {
                    stop();
                }
            }
        }, 20L, 1L);
    }
    
    private Location getNextBlock(){
        Location lowestBlock = blocks.get(0);
        int y = lowestBlock.getBlockY();
        
        for(int i=1; i<blocks.size(); i++){
            Location temp = blocks.get(i);
            if(temp.getBlockY() < y){
                lowestBlock = temp;
            }
        }
        blocks.remove(lowestBlock);
        return lowestBlock;
    }
    
    private void fillBlock(Location block){
        //log.info(block.toString());
        Material blockTyp = block.getBlock().getType();
        if(materialsToReplace.contains(blockTyp)){

            Location[] neighbours = getNeighbours(block);
            Material[] neighbourBlocks = new Material[6];

            for(int i=0; i<neighbours.length; i++){
                neighbourBlocks[i] = neighbours[i].getBlock().getType();
            }

            List<Material> mostCommonMaterial = findMostCommonMaterial(neighbourBlocks, block);
            if(mostCommonMaterial.isEmpty()){
                    mostCommonMaterial = findNearestBlocks(block);
                }
            //log.info("Block Material to fill with: " + mostCommonMaterial.toString());
            boolean placeBlock = true;
            if(mostCommonMaterial.isEmpty()){
                placeBlock = false;
            } else {
                for(Material m: notPlaceMaterials){
                    if(mostCommonMaterial.contains(m)){
                        placeBlock = false;
                    }
                }
            }
            if(placeBlock){
                block.getBlock().setType(mostCommonMaterial.get(rand.nextInt(mostCommonMaterial.size())));
                allBlocksPlaced++;
                for(int i=0; i<neighbourBlocks.length; i++){
                    if(materialsToReplace.contains(neighbourBlocks[i])){
                        newBlocks.add(neighbours[i]);
                    }
                }
            }
            //blocks.remove(0);
        } else {
            //blocks.remove(0);
            if(!blocks.isEmpty()){
                fillBlock(getNextBlock());
            }
        }
    }
    
    private Location[] getNeighbours(Location loc){
        Location[] neighbours = new Location[6];
        //direct neighbours
        neighbours[0] = loc.clone().add(1, 0, 0);
        neighbours[1] = loc.clone().add(-1, 0, 0);
        neighbours[2] = loc.clone().add(0, 1, 0);
        neighbours[3] = loc.clone().add(0, -1, 0);
        neighbours[4] = loc.clone().add(0, 0, 1);
        neighbours[5] = loc.clone().add(0, 0, -1);
        
        //indirect neighbours
        /*neighbours[6] = loc.clone().add(1, 0, 1);
        neighbours[7] = loc.clone().add(1, 0, -1);
        neighbours[8] = loc.clone().add(-1, 0, 1);
        neighbours[9] = loc.clone().add(-1, 0, -1);*/
        
        return neighbours;
    }
    
    private List<Material> findMostCommonMaterial(Material[] materials, Location loc){
        HashMap<Material, Integer> mostCommon = new HashMap<>();
        for(Material m: materials){
            if(materialsToIgnore.contains(m)){
                continue;
            }
            if(mostCommon.containsKey(m)){
                mostCommon.replace(m, mostCommon.get(m)+1);
            } else {
                mostCommon.put(m, 1);
            }
        }
        List<Material> mostCommonMaterials = new ArrayList<>();
        int max = 0;
        boolean shouldGetFilled = shouldFill(loc);
        //log.info("Should get filled: " + shouldGetFilled);
        for(Material m: mostCommon.keySet()){
            int temp = mostCommon.get(m);
            /*if(m == Material.AIR){
                if(temp >= 4){
                    mostCommonMaterials.clear();
                    mostCommonMaterials.add(m);
                    return mostCommonMaterials;
                }*/
            if(materialsToReplace.contains(m)){
                if(temp >= 4 && !shouldGetFilled){
                    mostCommonMaterials.clear();
                    //mostCommonMaterials.add(m);
                    return mostCommonMaterials;
                }
            } else if(shouldGetFilled){
                if(temp > max){
                    mostCommonMaterials.clear();
                    mostCommonMaterials.add(m);
                    max = temp;
                } else if(temp == max){
                    mostCommonMaterials.add(m);
                }
            }
        }
        return mostCommonMaterials;
    }
    
    private List<Material> findNearestBlocks(Location loc){
        //Location loc = blocks.get(0);
        Location temp;
        Material tempBlock;
        boolean foundBlock = false;
        List<Material> nearestBlocks = new ArrayList<>();
        for(int i=1; i<maxDistance+1; i++){
            for(int j=0; j<6; j++){
                temp = loc.clone();
                switch(j){
                    case 0:
                        temp.add(i, 0, 0);
                        break;
                    case 1:
                        temp.add(-i, 0, 0);
                        break;
                    case 2:
                        temp.add(0, i, 0);
                        break;
                    case 3:
                        temp.add(0, -i, 0);
                        break;
                    case 4:
                        temp.add(0, 0, i);
                        break;
                    case 5:
                        temp.add(0, 0, -i);
                        break;
                }
                tempBlock = temp.getBlock().getType();
                if(!materialsToIgnore.contains(tempBlock) && !materialsToReplace.contains(tempBlock)){
                    nearestBlocks.add(tempBlock);
                    foundBlock = true;
                }
            }
            if(foundBlock){
                break;
            }
        }
        return findMostCommonMaterial(nearestBlocks.toArray(new Material[nearestBlocks.size()]), loc);
    }
    
    private boolean shouldFill(Location loc){
        //Location loc = blocks.get(0);
        int numOfBlocks = 0;
        Location temp;
        int j;
        HashMap<Integer, Integer> directions = new HashMap<>();
        for(j=0; j<6; j++){
            directions.put(j, maxDistance);
            //log.info("J: " + String.valueOf(j));
            for(int i=1; i<maxDistance+1; i++){
                //log.info("I: " + String.valueOf(i));
                temp = loc.clone();
                switch(j){
                    case 0:
                        temp = temp.add(i, 0, 0);
                        break;
                    case 1:
                        temp = temp.subtract(i, 0, 0);
                        break;
                    case 2:
                        temp = temp.add(0, i, 0);
                        break;
                    case 3:
                        temp = temp.subtract(0, i, 0);
                        break;
                    case 4:
                        temp = temp.add(0, 0, i);
                        break;
                    case 5:
                        temp = temp.subtract(0, 0, i);
                        break;
                }
                Material tempBlock = temp.getBlock().getType();
                //log.info(String.format("Block %.0f %.0f %.0f %s", temp.getX(), temp.getY(), temp.getZ(), tempBlock.toString()));
                if(!materialsToReplace.contains(tempBlock) && !materialsToIgnore.contains(tempBlock)){
                    numOfBlocks++;
                    directions.replace(j, i);
                    break;
                }
            }
        }
        //log.info("Number of edges detected: " + String.valueOf(numOfBlocks));
        boolean fill = numOfBlocks >= 4;
        if(!fill){
            if(isOpposite(directions) || isHardCorner(directions, loc)){
                fill = true;
            }
        }
        return fill;
    }
    
    private boolean isOpposite(HashMap<Integer, Integer> directions){
        boolean x = directions.get(0) < maxDistance && directions.get(1) < maxDistance;
        boolean y = directions.get(2) < maxDistance && directions.get(3) < maxDistance;
        boolean z = directions.get(4) < maxDistance && directions.get(5) < maxDistance;
        
        return x || y || z;
    }
    
    private boolean isHardCorner(HashMap<Integer, Integer> directions, Location loc){
        
        //log.info(loc.toString());
        
        //boolean xPzP = !materialsToIgnore.contains(loc.clone().add(1, 0, 1).getBlock().getType());
        //boolean xPzN = !materialsToIgnore.contains(loc.clone().add(1, 0, -1).getBlock().getType());
        //boolean xNzP = !materialsToIgnore.contains(loc.clone().add(-1, 0, 1).getBlock().getType());
        //boolean xNzN = !materialsToIgnore.contains(loc.clone().add(-1, 0, -1).getBlock().getType());
        
        int i = rand.nextInt(2);
        int min_short = i;
        int min_long = 2 + i;
        
        boolean xP = directions.get(0) >= 3;
        boolean xN = directions.get(1) >= 3;
        boolean zP = directions.get(4) >= 3;
        boolean zN = directions.get(5) >= 3;
        
        int edgeXPLeft = getEdgeLength(loc, 0);
        int edgeXPRight = getEdgeLength(loc, 1);
        int edgeXNLeft = getEdgeLength(loc, 2); 
        int edgeXNRight = getEdgeLength(loc, 3);
        int edgeZPLeft = getEdgeLength(loc, 4);
        int edgeZPRight = getEdgeLength(loc, 5);
        int edgeZNLeft = getEdgeLength(loc, 6);
        int edgeZNRight = getEdgeLength(loc, 7);
        
        /*boolean e1 = xP && zP && directions.get(1) == 1 && directions.get(5) == 1 && xPzN && xNzP;
        boolean e2 = xP && zN && directions.get(1) == 1 && directions.get(4) == 1 && xPzP && xNzN;
        boolean e3 = xN && zP && directions.get(0) == 1 && directions.get(5) == 1 && xNzN && xPzP;
        boolean e4 = xN && zN && directions.get(0) == 1 && directions.get(4) == 1 && xNzP && xPzN;*/
        
        boolean e1 = xP && zP && directions.get(1) == 1 && directions.get(5) == 1;
        boolean e2 = xP && zN && directions.get(1) == 1 && directions.get(4) == 1;
        boolean e3 = xN && zP && directions.get(0) == 1 && directions.get(5) == 1;
        boolean e4 = xN && zN && directions.get(0) == 1 && directions.get(4) == 1;
        
        //log.info(String.format("BEFORE: E1: %b E2: %b E3: %b E4: %b", e1, e2, e3, e4));
        
        //log.info(String.format("XPLeft: %d, XPRight: %d XNLeft: %d XNRight: %d ZPLeft: %d ZPRight: %d ZNLeft: %d ZNRight: %d", edgeXPLeft, edgeXPRight, edgeXNLeft, edgeXNRight, edgeZPLeft, edgeZPRight, edgeZNLeft, edgeZNRight));
        
        if(e1){
            e1 = (edgeXPLeft >= min_short && edgeZPRight >= min_long) || (edgeXPLeft >= min_long && edgeZPRight >= min_short);
        } else if(e2){
            e2 = (edgeXPRight >= min_short && edgeZNLeft >= min_long) || (edgeXPRight >= min_long && edgeZNLeft >= min_short);
        } else if(e3){
            e3 = (edgeXNRight >= min_short && edgeZPLeft >= min_long) || (edgeXNRight >= min_long && edgeZPLeft >= min_short);
        } else if(e4){
            e4 = (edgeXNLeft >= min_short && edgeZNRight >= min_long) || (edgeXNLeft >= min_long && edgeZNRight >= min_short);
        }
        
        //log.info(String.format("AFTER: E1: %b E2: %b E3: %b E4: %b", e1, e2, e3, e4));
        
        return e1 || e2 || e3 || e4;
    }
    
    private int getEdgeLength(Location loc, int direction){
        int length = -1;
        Location temp;
        for(int i=0; i<maxDistance; i++){
            temp = loc.clone();
            switch(direction){
                case 0:
                    temp = temp.add(i, 0, -1);
                    break;
                case 1:
                    temp = temp.add(i, 0, 1);
                    break;
                case 2:
                    temp = temp.add(-i, 0, 1);
                    break;
                case 3:
                    temp = temp.add(-i, 0, -1);
                    break;
                case 4:
                    temp = temp.add(1, 0, i);
                    break;
                case 5:
                    temp = temp.add(-1, 0, i);
                    break;
                case 6:
                    temp = temp.add(-1, 0, -i);
                    break;
                case 7:
                    temp = temp.add(1, 0, -i);
                    break;
            }
            if(!materialsToIgnore.contains(temp.getBlock().getType())){
                length++;
            } else {
                break;
            }
        }
        return length;
    }
    
}
