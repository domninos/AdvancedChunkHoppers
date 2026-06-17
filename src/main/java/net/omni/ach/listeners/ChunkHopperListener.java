package net.omni.ach.listeners;

import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.chunkhopper.ChunkHopper;
import net.omni.ach.chunkhopper.ChunkHopperHolder;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        ItemStack drop = itemEntity.getItemStack();

        if (!hopper.shouldCollect(drop))
            return;

        int realAmount;
        if (plugin.getRoseStackerHook().isEnabled())
            realAmount = plugin.getRoseStackerHook().getStackedAmount(itemEntity);
        else
            realAmount = drop.getAmount();

        if (realAmount != drop.getAmount()) {
            drop = drop.clone();
            drop.setAmount(realAmount);
        }

        Block hopperBlock = hopper.getLocation().getBlock();
        Container bottom = plugin.getChunkHopperManager()
                .getBottomContainer(hopperBlock, hopper.getOwnerUUID());

        if (bottom != null) {
            Inventory bottomInv = bottom.getInventory();
            Map<Integer, ItemStack> leftovers = bottomInv.addItem(drop);

            if (leftovers.isEmpty()) {
                event.setCancelled(true);
                return;
            }

            drop = leftovers.get(0);
        }

        if (!hopper.canFitItem(drop)) {
            event.setCancelled(true);
            Location above = hopper.getLocation().clone().add(0, 1, 0);
            itemEntity.teleport(above);
            hopper.notifyFull(plugin);
            return;
        }

        Inventory mainInv = hopper.getMainInventory();
        Map<Integer, ItemStack> leftovers = mainInv.addItem(drop);

        hopper.markDirty();

        if (leftovers.isEmpty())
            event.setCancelled(true);
        else
            event.getEntity().setItemStack(leftovers.get(0));
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
            // TODO messages.yml
            plugin.sendMessage(player, "<red>Chunk Hopper must be placed facing down.</red>");
            return;
        }

        if (plugin.getChunkHopperManager().hasHopper(block.getChunk())) {
            event.setCancelled(true);
            // TODO messages.yml
            plugin.sendMessage(player, "<red>You can only place 1 <gold><b>Chunk Hopper</b></gold> in 1 chunk.</red>");
            return;
        }

        int maxHoppers = plugin.getChunkHopperManager().getMaxHoppers(player);

        if (maxHoppers != -1) {
            int current = plugin.getChunkHopperManager().getHopperCount(player.getUniqueId());

            if (current >= maxHoppers) {
                event.setCancelled(true);
                // TODO messages.yml
                plugin.sendMessage(player, "<red>You have reached the maximum number of Chunk Hoppers.</red>");
                return;
            }
        }

        Hopper hopper = (Hopper) block.getState();

        PersistentDataContainer pdc = hopper.getPersistentDataContainer();

        pdc.set(
                plugin.getChunkHopperManager().getOwnerKey(),
                PersistentDataType.STRING,
                player.getUniqueId().toString()
        );

        pdc.set(
                plugin.getChunkHopperManager().getAchKey(),
                PersistentDataType.STRING,
                "true"
        );

        hopper.update();

        plugin.getChunkHopperManager().getContainerLimit(block, player.getUniqueId());

        ChunkHopper hopperObj = new ChunkHopper(block.getLocation(), player.getUniqueId(), plugin);
        Chunk chunk = block.getChunk();
        plugin.getChunkHopperManager().registerHopper(chunk, hopperObj);
        plugin.getCacheManager().putIfAbsent(block.getLocation(), hopperObj);
        plugin.getChunkHopperManager().addHopperCount(player.getUniqueId());

        int max = plugin.getChunkHopperManager().getMaxHoppers(player);

        if (max != -1) {
            int currentCount = plugin.getChunkHopperManager().getHopperCount(player.getUniqueId());

            // TODO messages.yml
            plugin.sendMessage(player, "<white>You have " + (max - currentCount) + "(x) chunk hoppers left.</white>");
        }
    }

    @EventHandler
    public void onHopperBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (!plugin.getChunkHopperManager().isACH(block))
            return;

        Player player = event.getPlayer();

        if (!plugin.getGuiManager().isOwner(player, block)) {
            event.setCancelled(true);
            // TODO messages.yml
            plugin.sendMessage(player, "<red>You do not have permission to break this.</red>");
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
            for (Player viewer : Bukkit.getOnlinePlayers()) {
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

            // TODO messages.yml
            plugin.sendMessage(player, "<white>You now have " + currentCount + "(x) chunk hoppers left.</white>");
        }
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
            plugin.getCacheManager().invalidate(hopper.getLocation());
        }

        plugin.getChunkHopperManager().unregisterHopper(chunk);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getChunkHopperManager().loadHopperCount(event.getPlayer());
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
