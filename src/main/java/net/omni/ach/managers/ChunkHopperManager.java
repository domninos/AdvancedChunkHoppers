package net.omni.ach.managers;

import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.chunkhopper.ChunkHopper;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ChunkHopperManager {
    private final NamespacedKey ach_key;
    private final NamespacedKey containerLimitKey;
    private final NamespacedKey ownerKey;

    private final AdvancedChunkHoppers plugin;
    private final Map<Long, ChunkHopper> chunkHoppers = new HashMap<>();
    private final Map<UUID, Integer> hopperLimits = new HashMap<>();

    public ChunkHopperManager(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;

        this.ach_key = new NamespacedKey(plugin, "chunk_hopper");
        this.containerLimitKey = new NamespacedKey(plugin, "chunk_hopper_container_limit");
        this.ownerKey = new NamespacedKey(plugin, "chunk_hopper_owner");
    }

    public void init() {
        startPullingTask();
    }

    private void startPullingTask() {
        int interval = plugin.getConfigUtil().getPullerIntervalTicks();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (ChunkHopper hopper : chunkHoppers.values()) {
                    if (hopper == null) continue;

                    collectFromAbove(hopper);
                }
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    private void collectFromAbove(ChunkHopper hopper) {
        Location hopperLoc = hopper.getLocation();
        if (hopperLoc.getWorld() == null) return;

        Location scanCenter = hopperLoc.clone().add(0.5, 1.5, 0.5);
        for (Entity entity : hopperLoc.getWorld().getNearbyEntities(scanCenter, 0.5, 1.0, 0.5,
                e -> e instanceof Item it && it.isOnGround() && !it.isDead())) {

            Item item = (Item) entity;
            ItemStack drop = item.getItemStack();

            int realAmount;
            if (plugin.getRoseStackerHook().isEnabled())
                realAmount = plugin.getRoseStackerHook().getStackedAmount(item);
            else
                realAmount = drop.getAmount();

            if (realAmount != drop.getAmount()) {
                drop = drop.clone();
                drop.setAmount(realAmount);
            }

            if (!hopper.shouldCollect(drop))
                continue;

            Block hopperBlock = hopperLoc.getBlock();
            Container bottom = getBottomContainer(hopperBlock, hopper.getOwnerUUID());

            ItemStack remaining = drop;
            if (bottom != null) {
                Map<Integer, ItemStack> leftovers = bottom.getInventory().addItem(remaining);

                if (leftovers.isEmpty()) {
                    item.remove();
                    continue;
                }

                remaining = leftovers.get(0);
            }

            if (!hopper.canFitItem(remaining))
                continue;

            Map<Integer, ItemStack> leftovers = hopper.getMainInventory().addItem(remaining);

            if (leftovers.isEmpty())
                item.remove();
            else {
                item.setItemStack(leftovers.get(0));
                hopper.notifyFull(plugin);
            }

            hopper.markDirty();
        }
    }

    // TODO should get the permission for lp.groups
    @Nullable
    public Container getBottomContainer(Block hopper, UUID ownerUUID) {
        int limit = getContainerLimit(hopper, ownerUUID);
        Block current = hopper.getRelative(0, -1, 0);
        Container deepest = null;
        int scanned = 0;

        while (current.getState() instanceof Container container && scanned < limit) {
            deepest = container;
            current = current.getRelative(0, -1, 0);
            scanned++;
        }

        return deepest;
    }

    public int getContainerLimit(Block hopperBlock, UUID ownerUUID) {
        Player owner = Bukkit.getPlayer(ownerUUID);

        if (owner != null && owner.isOnline()) {
            int maxLimit = 0;

            for (Map.Entry<String, Integer> entry : plugin.getConfigUtil().getLimitsMap().entrySet()) {
                if (owner.hasPermission("lp.group." + entry.getKey())) {
                    if (entry.getValue() > maxLimit)
                        maxLimit = entry.getValue();
                }
            }

            int limit = Math.max(maxLimit, 1);
            storeContainerLimit(hopperBlock, limit);
            return limit;
        }

        return readContainerLimit(hopperBlock);
    }

    private void storeContainerLimit(Block hopperBlock, int limit) {
        if (!(hopperBlock.getState() instanceof Hopper hopper))
            return;
        hopper.getPersistentDataContainer().set(containerLimitKey, PersistentDataType.INTEGER, limit);
        hopper.update();
    }

    private int readContainerLimit(Block hopperBlock) {
        if (!(hopperBlock.getState() instanceof Hopper hopper))
            return 1;

        return hopper.getPersistentDataContainer().getOrDefault(containerLimitKey, PersistentDataType.INTEGER, 1);
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
        Block block = location.getBlock();

        if (!(block.getState() instanceof Hopper hopper))
            return CompletableFuture.completedFuture(null);

        String ownerStr = hopper.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);

        if (ownerStr == null)
            return CompletableFuture.completedFuture(null);

        UUID ownerUUID = UUID.fromString(ownerStr);

        ChunkHopper hopperObj = new ChunkHopper(location, ownerUUID, plugin);

        CompletableFuture<ChunkHopper> future = new CompletableFuture<>();

        CompletableFuture<List<ItemStack>> itemsFuture = plugin.getDatabaseManager().fetchItems(location);
        CompletableFuture<List<ItemStack>> whitelistFuture = plugin.getDatabaseManager().fetchWhitelist(location);
        CompletableFuture<List<ItemStack>> blacklistFuture = plugin.getDatabaseManager().fetchBlacklist(location);

        CompletableFuture.allOf(itemsFuture, whitelistFuture, blacklistFuture)
                .thenAccept(ignored -> {
                    hopperObj.loadItems(
                            itemsFuture.join(),
                            whitelistFuture.join(),
                            blacklistFuture.join());
                    future.complete(hopperObj);
                })
                .exceptionally(throwable -> {
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

        return pdc.has(ach_key);
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

        if (!(chunk.getWorld().getBlockAt(hopper.getLocation()).getState() instanceof Hopper)) {
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

    public NamespacedKey getAchKey() {
        return this.ach_key;
    }

    public NamespacedKey getOwnerKey() {
        return this.ownerKey;
    }

    public void flush() {
        for (ChunkHopper hopper : chunkHoppers.values())
            hopper.saveSync(plugin);

        hopperLimits.clear();
        chunkHoppers.clear();
    }
}
