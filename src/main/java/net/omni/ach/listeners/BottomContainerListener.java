package net.omni.ach.listeners;

import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.chunkhopper.ChunkHopper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;

public class BottomContainerListener implements Listener {

    private final AdvancedChunkHoppers plugin;

    public BottomContainerListener(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onContainerBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (!plugin.getChunkHopperManager().isContainerMat(block))
            return;

        ChunkHopper hopper = plugin.getChunkHopperManager().invalidateChainAbove(block);

        if (hopper != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Entity entity : block.getWorld().getEntities()) {
                    if (entity instanceof Item item && item.isValid() && !item.isDead())
                        plugin.getChunkHopperManager().addPendingItem(item);
                }
            });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onContainerPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();

        if (!plugin.getChunkHopperManager().isContainerMat(block))
            return;

        ChunkHopper hopper = plugin.getChunkHopperManager().invalidateChainAbove(block);

        if (hopper != null)
            plugin.getChunkHopperManager().collectItemsInChunk(hopper.getLocation().getChunk());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        BlockFace dir = event.getDirection();

        for (Block b : event.getBlocks()) {
            if (!plugin.getChunkHopperManager().isContainerMat(b))
                continue;

            plugin.getChunkHopperManager().invalidateChainAbove(b);
            plugin.getChunkHopperManager().invalidateChainAbove(b.getRelative(dir));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        BlockFace dir = event.getDirection().getOppositeFace();

        for (Block b : event.getBlocks()) {
            if (!plugin.getChunkHopperManager().isContainerMat(b))
                continue;

            plugin.getChunkHopperManager().invalidateChainAbove(b);
            plugin.getChunkHopperManager().invalidateChainAbove(b.getRelative(dir));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        if (plugin.getChunkHopperManager().isContainerMat(block))
            plugin.getChunkHopperManager().invalidateChainAbove(block);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Block block = event.getBlock();
        Material to = event.getTo();

        if (plugin.getChunkHopperManager().isContainerMat(block) || plugin.getConfigUtil().getContainerMaterials().contains(to))
            plugin.getChunkHopperManager().invalidateChainAbove(block);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> it = event.blockList().iterator();

        while (it.hasNext()) {
            Block block = it.next();

            if (block.getType() == Material.HOPPER && plugin.getChunkHopperManager().isACHLocation(block.getLocation()))
                it.remove();

            if (plugin.getChunkHopperManager().isContainerMat(block))
                plugin.getChunkHopperManager().invalidateChainAbove(block);
        }
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
