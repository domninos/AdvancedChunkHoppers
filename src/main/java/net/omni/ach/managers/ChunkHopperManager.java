package net.omni.ach.managers;

import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.chunkhopper.ChunkHopper;
import net.omni.ach.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ChunkHopperManager {
    private static final NamespacedKey ACH_KEY = new NamespacedKey("customcrafting", "utilities/chunk_hopper");

    private final AdvancedChunkHoppers plugin;
    private final Map<Long, ChunkHopper> chunkHoppers = new HashMap<>();
    private final Map<UUID, Integer> hopperLimits = new HashMap<>();

    public ChunkHopperManager(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    public void init() {
        // TODO load from db on startup
    }

    public void loadFromChunkAsync(Chunk chunk) {
        if (hasHopper(chunk))
            return;

        Location location = findHopperInChunk(chunk);

        if (location == null)
            return;

        load(location).thenAccept(hopper -> {
            if (hopper != null) {
                registerHopper(chunk, hopper);
                plugin.getCacheManager().putIfAbsent(location, hopper);
            }
        });
    }

    public boolean hasHopper(Chunk chunk) {
        if (chunk == null)
            return false;

        if (!chunk.isLoaded() || !chunk.isEntitiesLoaded())
            return false;

        return chunkHoppers.containsKey(chunk.getChunkKey());
    }

    @Nullable
    private Location findHopperInChunk(Chunk chunk) {
        int minX = chunk.getX() << 4;
        int minZ = chunk.getZ() << 4;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getWorld().getMinHeight(); y < chunk.getWorld().getMaxHeight(); y++) {
                    Block block = chunk.getBlock(x, y, z);

                    if (block.getType() == Material.HOPPER && isACH(block))
                        return block.getLocation();
                }
            }
        }

        return null;
    }

    public CompletableFuture<ChunkHopper> load(Location location) {
        int mainSize = plugin.getConfigUtil().getHopperSize();
        int whitelistSize = plugin.getConfigUtil().getWhitelistInventorySize();
        int blacklistSize = plugin.getConfigUtil().getBlacklistInventorySize();

        Block block = location.getBlock();

        if (!(block.getState() instanceof Hopper hopper))
            return CompletableFuture.completedFuture(null);

        String ownerStr = hopper.getPersistentDataContainer().get(plugin.getGuiManager().getOwnerKey(), PersistentDataType.STRING);

        if (ownerStr == null)
            return CompletableFuture.completedFuture(null);

        UUID ownerUUID = UUID.fromString(ownerStr);

        Inventory mainInv = Bukkit.createInventory(null, mainSize,
                MessageUtil.parse(plugin.getConfigUtil().getHopperTitle()));
        Inventory whitelistInv = Bukkit.createInventory(null, whitelistSize,
                MessageUtil.parse(plugin.getConfigUtil().getWhitelistInventoryTitle()));
        Inventory blacklistInv = Bukkit.createInventory(null, blacklistSize,
                MessageUtil.parse(plugin.getConfigUtil().getBlacklistInventoryTitle()));

        CompletableFuture<ChunkHopper> future = new CompletableFuture<>();

        CompletableFuture.allOf(
                plugin.getDatabaseManager().fetchItems(location)
                        .thenAccept(items -> fillInventory(mainInv, items, 36)),
                plugin.getDatabaseManager().fetchWhitelist(location)
                        .thenAccept(items -> fillInventory(whitelistInv, items, whitelistSize)),
                plugin.getDatabaseManager().fetchBlacklist(location)
                        .thenAccept(items -> fillInventory(blacklistInv, items, blacklistSize))
        ).thenRun(() -> {
            ChunkHopper hopperObj = new ChunkHopper(location, ownerUUID, mainInv, whitelistInv, blacklistInv);
            future.complete(hopperObj);
        }).exceptionally(throwable -> {
            future.completeExceptionally(throwable);
            return null;
        });

        return future;
    }

    public void registerHopper(Chunk chunk, ChunkHopper hopper) {
        chunkHoppers.put(chunk.getChunkKey(), hopper);
    }

    public boolean isACH(Block block) {
        if (!(block.getState() instanceof Hopper hopper))
            return false;

        PersistentDataContainer pdc = hopper.getPersistentDataContainer();
        return pdc.has(ACH_KEY);
    }

    private void fillInventory(Inventory inv, List<ItemStack> items, int limit) {
        for (int i = 0; i < Math.min(items.size(), limit); i++) {
            ItemStack it = items.get(i);
            if (it != null && it.getType() != Material.AIR)
                inv.setItem(i, it);
        }
    }

    @Nullable
    public ChunkHopper getChunkHopper(Chunk chunk) {
        if (!hasHopper(chunk))
            return null;

        ChunkHopper hopper = chunkHoppers.get(chunk.getChunkKey());

        if (hopper == null) {
            unregisterHopper(chunk);
            return null;
        }

        if (!(chunk.getWorld().getBlockAt(hopper.location()).getState() instanceof Hopper)) {
            unregisterHopper(chunk);
            return null;
        }

        return hopper;
    }

    public void unregisterHopper(Chunk chunk) {
        chunkHoppers.remove(chunk.getChunkKey());
    }

    public int getHopperLimit(Player player) {
        if (player.hasPermission("ach.limit.none"))
            return -1;

        if (hopperLimits.containsKey(player.getUniqueId()))
            return hopperLimits.get(player.getUniqueId());

        for (int i = 100; i > 0; i--) {
            if (player.hasPermission("ach.limit." + i)) {
                hopperLimits.put(player.getUniqueId(), i);
                return i;
            }
        }

        return 5;
    }

    public Container getBottomContainer(Block hopper) {
        Block blockBelow = hopper.getRelative(0, -1, 0);
        Container lowest = null;

        while (blockBelow.getState() instanceof Container container) {
            lowest = container;
            blockBelow = blockBelow.getRelative(0, -1, 0);
        }

        return lowest;
    }

    public void flush() {
        for (ChunkHopper hopper : chunkHoppers.values()) {
            hopper.save(plugin);
        }

        hopperLimits.clear();
        chunkHoppers.clear();
    }
}
