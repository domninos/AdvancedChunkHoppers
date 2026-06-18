package net.omni.ach.listeners;

import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.chunkhopper.ChunkHopper;
import net.omni.ach.chunkhopper.ChunkHopperHolder;
import net.omni.ach.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChunkHopperListener implements Listener {

    private final AdvancedChunkHoppers plugin;

    public ChunkHopperListener(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemSpawnSpawnInChunk(ItemSpawnEvent event) {
        Item itemEntity = event.getEntity();
        Location location = event.getLocation();
        Chunk chunk = location.getChunk();

        if (!plugin.getChunkHopperManager().hasHopper(chunk))
            return;

        ChunkHopper hopper = plugin.getChunkHopperManager().getChunkHopper(chunk);
        if (hopper == null)
            return;

        plugin.getChunkHopperManager().addPendingItem(itemEntity);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopperPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();

        if (!plugin.getCustomCraftingHook().isCustomChunkHopper(event.getItemInHand()))
            return;

        Player player = event.getPlayer();
        Block against = event.getBlockAgainst();

        if (block.getY() - against.getY() != 1) {
            event.setCancelled(true);
            plugin.sendMessage(player, Messages.HOPPER_FACE_DOWN.toString());
            return;
        }

        if (plugin.getChunkHopperManager().hasHopper(block.getChunk())) {
            event.setCancelled(true);
            plugin.sendMessage(player, Messages.ONE_PER_CHUNK.toString());
            return;
        }

        int current = plugin.getChunkHopperManager().getHopperCount(player.getUniqueId());
        int maxHoppers = plugin.getChunkHopperManager().getMaxHoppers(player);

        if (maxHoppers != -1) {

            if (current >= maxHoppers) {
                event.setCancelled(true);
                plugin.sendMessage(player, Messages.MAX_HOPPERS_REACHED.toString());
                return;
            }
        }

        org.bukkit.block.data.type.Hopper hopperData = (org.bukkit.block.data.type.Hopper) block.getBlockData();

        hopperData.setEnabled(false);
        block.setBlockData(hopperData, false);

        Hopper hopper = (Hopper) block.getState();
        PersistentDataContainer pdc = hopper.getPersistentDataContainer();

        pdc.set(
                plugin.getChunkHopperManager().getOwnerKey(),
                PersistentDataType.STRING,
                player.getUniqueId().toString()
        );

        pdc.set(
                plugin.getChunkHopperManager().getAchKey(),
                PersistentDataType.BYTE,
                (byte) 1
        );

        hopper.update(true, false);

        ChunkHopper hopperObj = new ChunkHopper(block.getLocation(), player.getUniqueId(), plugin);
        plugin.getChunkHopperManager().recalculateLimit(hopperObj);
        Chunk chunk = block.getChunk();
        plugin.getChunkHopperManager().registerHopper(chunk, hopperObj);
        plugin.getCacheManager().putIfAbsent(block.getLocation(), hopperObj);
        plugin.getChunkHopperManager().addHopperCount(player.getUniqueId());

        int delay = plugin.getConfigUtil().getPullerIntervalTicks();
        if (delay > 0)
            Bukkit.getScheduler().runTaskLater(plugin, () -> scanChunkForExistingItems(chunk), delay);
        else
            scanChunkForExistingItems(chunk);

        if (maxHoppers != -1) {
            int updatedCount = plugin.getChunkHopperManager().getHopperCount(player.getUniqueId());

            plugin.sendMessage(player, Messages.HOPPERS_LEFT.replace(
                    "remaining", String.valueOf(maxHoppers - updatedCount),
                    "max", String.valueOf(maxHoppers)
            ));
        }
    }

    public void scanChunkForExistingItems(Chunk chunk) {
        if (!chunk.isLoaded() && !chunk.isEntitiesLoaded())
            return;

        ChunkHopper hopper = plugin.getChunkHopperManager().getChunkHopper(chunk);

        if (hopper == null)
            return;

        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof Item item))
                continue;

            if (item.isDead())
                continue;

            plugin.getChunkHopperManager().addPendingItem(item);
        }
    }

    @EventHandler
    public void onHopperBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (!plugin.getChunkHopperManager().isACHLocation(block.getLocation()))
            return;

        Player player = event.getPlayer();

        if (!plugin.getGuiManager().isOwner(player, block)) {
            event.setCancelled(true);
            plugin.sendMessage(player, Messages.NO_BREAK_PERMS.toString());
            return;
        }

        ChunkHopper hopper = plugin.getCacheManager().getCachedHopper(block.getLocation());
        Chunk chunk = block.getChunk();

        if (hopper != null) {
            int itemSlots = hopper.getMainInventory().getSize() - 9;
            Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);

            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < itemSlots; i++) {
                ItemStack item = hopper.getMainInventory().getItem(i);

                if (item != null && item.getType() != Material.AIR) {
                    items.add(item.clone());
                    hopper.getMainInventory().clear(i);
                }
            }

            // close the inventories for all viewers
            for (Player viewer : block.getLocation().getNearbyPlayers(10)) {
                if (viewer == null)
                    continue;

                Inventory top = viewer.getOpenInventory().getTopInventory();

                if (top.getHolder() instanceof ChunkHopperHolder holder && holder.hopper().equals(hopper))
                    viewer.closeInventory();
            }

            plugin.getChunkHopperManager().unregisterHopper(chunk);
            plugin.getCacheManager().discard(block.getLocation());
            plugin.getDatabaseManager().deleteLocation(block.getLocation());
            plugin.getChunkHopperManager().removeHopperCount(hopper.getOwnerUUID());

            for (ItemStack item : items)
                block.getWorld().dropItemNaturally(dropLoc, item);

            items.clear(); // garbage
        } else {
            plugin.getChunkHopperManager().unregisterHopper(chunk);

            UUID ownerUUID = plugin.getGuiManager().getOwnerUUID(block);
            if (ownerUUID != null)
                plugin.getChunkHopperManager().removeHopperCount(ownerUUID);
        }

        int max = plugin.getChunkHopperManager().getMaxHoppers(player);

        if (max != -1) {
            int currentCount = plugin.getChunkHopperManager().getHopperCount(player.getUniqueId());

            if (currentCount < 0) currentCount = 0;

            plugin.sendMessage(player, Messages.HOPPERS_NOW.replace("count", String.valueOf(currentCount)));
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block ->
                block.getType() == Material.HOPPER && plugin.getChunkHopperManager().isACHLocation(block.getLocation()));
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        int delay = plugin.getConfigUtil().getPullerIntervalTicks();
        plugin.getChunkHopperManager().loadFromChunkAsync(chunk, () ->
                Bukkit.getScheduler().runTaskLater(plugin, () -> scanChunkForExistingItems(chunk), delay));
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        if (!plugin.getChunkHopperManager().hasHopper(chunk))
            return;

        ChunkHopper hopper = plugin.getChunkHopperManager().getChunkHopper(chunk);

        if (hopper != null) {
            hopper.invalidateBottomContainerCache();
            hopper.save(plugin);
            plugin.getCacheManager().invalidate(hopper.getLocation());
        }

        plugin.getChunkHopperManager().unregisterHopper(chunk);
    }

    // to prevent the normal hopper functionality
    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (event.getInventory().getHolder(false) instanceof Hopper hopper
                && plugin.getChunkHopperManager().isACHLocation(hopper.getLocation()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getChunkHopperManager().loadHopperCount(event.getPlayer());
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
