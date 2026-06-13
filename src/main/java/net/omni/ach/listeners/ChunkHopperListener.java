package net.omni.ach.listeners;

import net.omni.ach.AdvancedChunkHoppers;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ChunkHopperListener implements Listener {
    // TODO create custom event

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

        // find hopper
        Hopper hopper = plugin.getChunkHopperManager().getChunkHopper(chunk);
        if (hopper == null) return; // if somehow hopper is still null

        ItemStack drop = event.getEntity().getItemStack();

        // filter
        if (!plugin.getFilterManager().shouldCollect(hopper.getLocation(), drop))
            return;

        if (plugin.getCacheManager().hasCache(hopper.getLocation())) {
            Inventory inventory = plugin.getCacheManager().getCachedInventory(hopper.getLocation());

            if (plugin.getGuiManager().canFitItem(inventory, drop)) {
                Map<Integer, ItemStack> leftovers = inventory.addItem(drop);

                if (leftovers.isEmpty())
                    event.setCancelled(true);
                else
                    event.getEntity().setItemStack(leftovers.get(0));
            }
        } else {
            // fallback - shouldn't use async since hoppers automatically load on chunk load event
            plugin.getCacheManager().getOrCreate(hopper.getLocation()).whenComplete((inventory, throwable) -> {
                if (inventory == null || throwable != null) {
                    Bukkit.getLogger().warning("An error has occurred while getting GUI: " + throwable.getMessage());
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!plugin.getGuiManager().canFitItem(inventory, drop)) {
                        // inventory is full
                        return;
                    }

                    Map<Integer, ItemStack> leftovers = inventory.addItem(drop);

                    if (leftovers.isEmpty())
                        event.setCancelled(true);
                    else
                        event.getEntity().setItemStack(leftovers.get(0));
                });
            });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void oHopperPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();

        if (!plugin.getChunkHopperManager().isACH(block))
            return;

        Player player = event.getPlayer();

        // TODO check for permission (limits)
        int limit = plugin.getChunkHopperManager().getHopperLimit(player);

        if (limit != -1) {
            // if not admin or unlimited

        }

        // TODO create / register / load hopper

    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.getChunkHopperManager().loadFromChunkAsync(event.getChunk());
    }


    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
