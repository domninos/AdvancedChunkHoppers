package net.omni.ach.listeners;

import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.chunkhopper.ChunkHopper;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ChunkHopperListener implements Listener {

    private final AdvancedChunkHoppers plugin;

    public ChunkHopperListener(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawnSpawnInChunk(ItemSpawnEvent event) {
        Location location = event.getLocation();
        Chunk chunk = location.getChunk();

        if (!plugin.getChunkHopperManager().hasHopper(chunk))
            return;

        ChunkHopper hopper = plugin.getChunkHopperManager().getChunkHopper(chunk);
        if (hopper == null)
            return;

        ItemStack drop = event.getEntity().getItemStack();

        if (!plugin.getFilterManager().shouldCollect(hopper, drop))
            return;

        if (hopper.canFitItem(drop)) {
            Inventory inventory = hopper.mainInventory();
            Map<Integer, ItemStack> leftovers = inventory.addItem(drop);

            if (leftovers.isEmpty())
                event.setCancelled(true);
            else
                event.getEntity().setItemStack(leftovers.get(0));
        }
    }

    /*

     */

    @EventHandler(ignoreCancelled = true)
    public void onHopperPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();

        if (!plugin.getChunkHopperManager().isACH(block))
            return;

        Player player = event.getPlayer();

        int limit = plugin.getChunkHopperManager().getHopperLimit(player);

        if (limit != -1) {
            // TODO check current count against limit
        }

        if (block.getState() instanceof Hopper hopper) {
            hopper.getPersistentDataContainer().set(
                    plugin.getGuiManager().getOwnerKey(),
                    PersistentDataType.STRING,
                    player.getUniqueId().toString()
            );

            hopper.update();

            // register hopper
        }
    }

    @EventHandler
    public void onHopperBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (!plugin.getChunkHopperManager().isACH(block))
            return;

        Player player = event.getPlayer();

        if (plugin.getGuiManager().getOwnerUUID(block) != null
                && !plugin.getGuiManager().isOwner(player, block)) {
            event.setCancelled(true);
            plugin.sendMessage(player, "<red>You do not have permission to break this.</red>");
            return;
        }

        ChunkHopper hopper = plugin.getCacheManager().getCachedHopper(block.getLocation());

        if (hopper != null) {
            for (int i = 0; i < ChunkHopper.ITEM_SLOTS; i++) {
                ItemStack item = hopper.mainInventory().getItem(i);

                if (item != null && item.getType() != Material.AIR) {
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), item);
                    hopper.mainInventory().clear(i);
                }
            }

            hopper.save(plugin);
            plugin.getCacheManager().invalidate(block.getLocation());
        }

        Chunk chunk = block.getChunk();
        plugin.getChunkHopperManager().unregisterHopper(chunk);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.getChunkHopperManager().loadFromChunkAsync(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        if (!plugin.getChunkHopperManager().hasHopper(chunk))
            return;

        ChunkHopper hopper = plugin.getChunkHopperManager().getChunkHopper(chunk);

        if (hopper != null) {
            hopper.save(plugin);
            plugin.getCacheManager().invalidate(hopper.location());
        }

        plugin.getChunkHopperManager().unregisterHopper(chunk);
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
