package net.omni.ach.listeners;

import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.chunkhopper.ChunkHopper;
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
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

public class ChunkHopperListener implements Listener {

    private final AdvancedChunkHoppers plugin;

    public ChunkHopperListener(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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

        int realAmount;
        if (plugin.getRoseStackerHook().isEnabled())
            realAmount = plugin.getRoseStackerHook().getStackedAmount(itemEntity);
        else
            realAmount = drop.getAmount();

        if (realAmount != drop.getAmount()) {
            drop = drop.clone();
            drop.setAmount(realAmount);
        }

        if (!hopper.shouldCollect(drop))
            return;

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

        int limit = plugin.getChunkHopperManager().getHopperLimit(player);

        if (limit != -1) {
            // TODO check current count against limit
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

        if (hopper != null) {
            int itemSlots = hopper.getMainInventory().getSize() - 9;
            Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);

            for (int i = 0; i < itemSlots; i++) {
                ItemStack item = hopper.getMainInventory().getItem(i);

                if (item != null && item.getType() != Material.AIR) {
                    block.getWorld().dropItemNaturally(dropLoc, item);
                    hopper.getMainInventory().clear(i);
                }
            }

            hopper.markDirty();
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
            plugin.getCacheManager().invalidate(hopper.getLocation());
        }

        plugin.getChunkHopperManager().unregisterHopper(chunk);
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
