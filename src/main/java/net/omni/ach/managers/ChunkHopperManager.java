package net.omni.ach.managers;

import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.chunkhopper.ChunkHopper;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkHopperManager {
    private final NamespacedKey ach_key;
    private final NamespacedKey containerLimitKey;
    private final NamespacedKey ownerKey;

    private final AdvancedChunkHoppers plugin;
    private final Map<Long, ChunkHopper> chunkHoppers = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> hopperCounts = new ConcurrentHashMap<>();
    private final Map<Location, UUID> filterViewers = new ConcurrentHashMap<>();

    private final Set<Location> achHopperLocations = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final int MAX_ITEMS_PER_TICK = 100;
    private final Queue<Item> pendingItems = new ArrayDeque<>();
    private BukkitRunnable pullerTask;

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
        if (pullerTask != null)
            pullerTask.cancel();

        int interval = plugin.getConfigUtil().getPullerIntervalTicks();

        pullerTask = new BukkitRunnable() {
            @Override
            public void run() {
                int processed = 0;

                while (processed < MAX_ITEMS_PER_TICK) {
                    Item item = pendingItems.poll();

                    if (item == null)
                        return;

                    if (item.isValid())
                        collect(item);

                    processed++;
                }
            }
        };

        pullerTask.runTaskTimer(plugin, interval, interval);
    }

    public void reloadPullerTask() {
        startPullingTask();
    }

    public void addPendingItem(Item item) {
        if (item != null)
            pendingItems.add(item);
    }

    public boolean isFilterViewer(Location location, UUID viewerUUID) {
        UUID current = filterViewers.get(location);
        return current != null && !current.equals(viewerUUID);
    }

    public void setFilterViewer(Location location, UUID viewerUUID) {
        filterViewers.put(location, viewerUUID);
    }

    public void removeFilterViewer(Location location, UUID viewerUUID) {
        filterViewers.remove(location, viewerUUID);
    }

    public void collectNearbyItems(ChunkHopper hopper) {
        Location loc = hopper.getLocation();

        if (loc.getWorld() == null)
            return;

        for (Entity entity : loc.getChunk().getEntities()) {
            if (!(entity instanceof Item item))
                continue;

            if (item.isDead())
                continue;

            addPendingItem(item);
        }
    }

    private void collect(Item itemEntity) {
        ItemStack drop = itemEntity.getItemStack();

        Location location = itemEntity.getLocation();

        ChunkHopper hopper = getChunkHopper(location.getChunk());

        if (hopper == null)
            return;

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

        List<Container> bottoms = hopper.getBottomContainers(plugin);

        if (bottoms.isEmpty())
            return;

        ItemStack remaining = drop;

        for (Container bottom : bottoms) {
            if (!hasSpaceFor(bottom.getInventory(), remaining))
                continue;

            Map<Integer, ItemStack> leftovers = bottom.getInventory().addItem(remaining);

            if (leftovers.isEmpty()) {
                itemEntity.remove();
                return;
            }

            remaining = leftovers.get(0);
        }

        int beforeAmount = remaining.getAmount();
        Map<Integer, ItemStack> leftovers = hopper.getMainInventory().addItem(remaining);

        if (leftovers.isEmpty()) {
            itemEntity.remove();
            hopper.markDirty();
            return;
        }

        ItemStack leftover = leftovers.get(0);
        if (leftover.getAmount() == beforeAmount) {
            hopper.notifyFull(plugin);
            return;
        }

        itemEntity.setItemStack(leftover);
        hopper.markDirty();
    }

    private static boolean hasSpaceFor(Inventory inv, ItemStack item) {
        int needed = item.getAmount();
        int maxStack = item.getMaxStackSize();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack slot = inv.getItem(i);

            if (slot == null || slot.getType() == Material.AIR)
                return true;

            if (slot.isSimilar(item)) {
                int space = maxStack - slot.getAmount();

                if (space > 0) {
                    needed -= space;

                    if (needed <= 0)
                        return true;
                }
            }
        }

        return false;
    }

    public void loadFromChunkAsync(Chunk chunk, Runnable afterRegister) {
        if (hasHopper(chunk))
            return;

        Location location = findHopperInChunk(chunk);

        if (location == null)
            return;

        load(location).thenAccept(hopper -> {
            if (hopper != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    recalculateLimit(hopper);
                    registerHopper(chunk, hopper);
                    plugin.getCacheManager().putIfAbsent(location, hopper);
                    if (afterRegister != null)
                        afterRegister.run();
                });
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
        for (BlockState state : chunk.getTileEntities()) {
            if (state.getType() == Material.HOPPER && isACH(state.getBlock()))
                return state.getBlock().getLocation();
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

    public void recalculateLimit(ChunkHopper hopper) {
        Player owner = Bukkit.getPlayer(hopper.getOwnerUUID());
        if (owner == null || !owner.isOnline()) return;

        int maxLimit = 1;
        for (Map.Entry<String, Integer> entry : plugin.getConfigUtil().getLimitsMap().entrySet()) {
            if (owner.hasPermission("group." + entry.getKey())) {
                int val = entry.getValue();

                if (val == -1) {
                    maxLimit = -1;
                    break;
                }

                if (val > maxLimit)
                    maxLimit = val;
            }
        }

        if (maxLimit != hopper.getContainerLimit()) {
            hopper.setContainerLimit(maxLimit);
            storeContainerLimit(hopper.getLocation().getBlock(), maxLimit);
        }
    }

    public void registerHopper(Chunk chunk, ChunkHopper hopper) {
        chunkHoppers.put(chunk.getChunkKey(), hopper);
        achHopperLocations.add(hopper.getLocation());
    }

    public boolean isACH(Block block) {
        if (!(block.getState() instanceof Hopper hopper))
            return false;

        PersistentDataContainer pdc = hopper.getPersistentDataContainer();

        return pdc.has(ach_key);
    }

    private void storeContainerLimit(Block hopperBlock, int maxLimit) {
        if (!(hopperBlock.getState() instanceof Hopper hopper))
            return;

        hopper.getPersistentDataContainer().set(containerLimitKey, PersistentDataType.INTEGER, maxLimit);
        hopper.update();
    }

    public int readContainerLimitSync(Block block) {
        if (!(block.getState() instanceof Hopper hopper))
            return 0;

        PersistentDataContainer pdc = hopper.getPersistentDataContainer();

        Integer limit = pdc.get(containerLimitKey, PersistentDataType.INTEGER);

        return limit != null ? limit : 0;
    }

    @Nullable
    public ChunkHopper getChunkHopper(Chunk chunk) {
        ChunkHopper hopper = chunkHoppers.get(chunk.getChunkKey());

        if (hopper == null) {
            unregisterHopper(chunk);
            return null;
        }

        return hopper;
    }

    public void unregisterHopper(Chunk chunk) {
        ChunkHopper hopper = chunkHoppers.remove(chunk.getChunkKey());

        if (hopper != null)
            achHopperLocations.remove(hopper.getLocation());
    }

    public boolean isACHLocation(Location location) {
        return achHopperLocations.contains(location);
    }

    public int getMaxHoppers(Player player) {
        int max = -1;

        for (Map.Entry<String, Integer> entry : plugin.getConfigUtil().getHoppersPlacedLimitsMap().entrySet()) {
            if (player.hasPermission("group." + entry.getKey())) {
                if (entry.getValue() > max)
                    max = entry.getValue();
            }
        }

        return max;
    }

    public void loadHopperCount(Player player) {
        int count = plugin.getDatabaseManager().countHoppersSync(player.getUniqueId());
        hopperCounts.put(player.getUniqueId(), count);
    }

    public void addHopperCount(UUID ownerUUID) {
        hopperCounts.merge(ownerUUID, 1, Integer::sum);
    }

    public void removeHopperCount(UUID ownerUUID) {
        hopperCounts.computeIfPresent(ownerUUID, (uuid, count) -> count > 1 ? count - 1 : null);
    }

    public int getHopperCount(UUID ownerUUID) {
        return hopperCounts.getOrDefault(ownerUUID, -1);
    }

    public NamespacedKey getAchKey() {
        return this.ach_key;
    }

    public NamespacedKey getOwnerKey() {
        return this.ownerKey;
    }

    public Map<Long, ChunkHopper> getChunkHoppers() {
        return chunkHoppers;
    }

    public void flush() {
        for (ChunkHopper hopper : chunkHoppers.values()) {
            hopper.saveSync(plugin);
            hopper.flush();
        }

        hopperCounts.clear();
        chunkHoppers.clear();

        filterViewers.clear();
        achHopperLocations.clear();

        if (pullerTask != null)
            pullerTask.cancel();

        pendingItems.clear();
    }
}
