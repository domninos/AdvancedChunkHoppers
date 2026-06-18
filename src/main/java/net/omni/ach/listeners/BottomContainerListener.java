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
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public class BottomContainerListener implements Listener {

    private static final int MAX_CHAIN_HEIGHT = 20;

    private final AdvancedChunkHoppers plugin;

    public BottomContainerListener(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onContainerBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (!isContainerMat(block))
            return;

        ChunkHopper hopper = invalidateChainAbove(block);

        if (hopper != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Entity entity : block.getWorld().getEntities()) {
                    if (entity instanceof Item item && item.isValid() && !item.isDead())
                        plugin.getChunkHopperManager().addPendingItem(item);
                }
            });
        }
    }

    private boolean isContainerMat(Block block) {
        return plugin.getConfigUtil().getContainerMaterials().contains(block.getType());
    }

    @Nullable
    private ChunkHopper invalidateChainAbove(Block block) {
        if (!isContainerMat(block))
            return null;

        Block current = block.getRelative(0, 1, 0);

        for (int i = 0; i < MAX_CHAIN_HEIGHT; i++) {
            ChunkHopper result = tryInvalidateACH(current);

            if (result != null)
                return result;

            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                result = tryInvalidateACH(current.getRelative(o[0], 0, o[1]));

                if (result != null)
                    return result;
            }

            if (!isContainerMat(current))
                return null;

            current = current.getRelative(0, 1, 0);
        }

        return null;
    }

    @Nullable
    private ChunkHopper tryInvalidateACH(Block block) {
        if (!plugin.getChunkHopperManager().isACHLocation(block.getLocation()))
            return null;

        ChunkHopper hopper = plugin.getCacheManager().getCachedHopper(block.getLocation());

        if (hopper != null) {
            hopper.invalidateBottomContainerCache();
            plugin.getChunkHopperManager().pushItemsDown(hopper);
        }

        return hopper;
    }

    @EventHandler(ignoreCancelled = true)
    public void onContainerPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();

        if (!isContainerMat(block))
            return;

        ChunkHopper hopper = invalidateChainAbove(block);

        if (hopper != null)
            plugin.getChunkHopperManager().collectItemsInChunk(hopper.getLocation().getChunk());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        BlockFace dir = event.getDirection();

        for (Block b : event.getBlocks()) {
            if (!isContainerMat(b))
                continue;

            invalidateChainAbove(b);
            invalidateChainAbove(b.getRelative(dir));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        BlockFace dir = event.getDirection().getOppositeFace();

        for (Block b : event.getBlocks()) {
            if (!isContainerMat(b))
                continue;

            invalidateChainAbove(b);
            invalidateChainAbove(b.getRelative(dir));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        if (isContainerMat(block))
            invalidateChainAbove(block);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Block block = event.getBlock();
        Material to = event.getTo();

        if (isContainerMat(block) || plugin.getConfigUtil().getContainerMaterials().contains(to))
            invalidateChainAbove(block);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> it = event.blockList().iterator();

        while (it.hasNext()) {
            Block block = it.next();

            if (block.getType() == Material.HOPPER && plugin.getChunkHopperManager().isACHLocation(block.getLocation()))
                it.remove();

            if (isContainerMat(block))
                invalidateChainAbove(block);
        }
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
