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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        int interval = plugin.getConfigUtil().getPullerIntervalTicks();

        pullerTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (ChunkHopper hopper : chunkHoppers.values()) {
                    if (hopper == null)
                        continue;

                    collectFromAbove(hopper);
                }
            }
        };

        pullerTask.runTaskTimer(plugin, interval, interval);
    }

    private void collectFromAbove(ChunkHopper hopper) {
        Location hopperLoc = hopper.getLocation();
        if (hopperLoc.getWorld() == null) return;

        pushItemsDown(hopper);

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
            List<Container> bottoms = getBottomContainers(hopperBlock, hopper.getOwnerUUID());

            ItemStack remaining = drop;
            for (Container bottom : bottoms) {
                Map<Integer, ItemStack> leftovers = bottom.getInventory().addItem(remaining);
                if (leftovers.isEmpty()) {
                    item.remove();
                    hopper.markDirty();
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

    public void pushItemsDown(ChunkHopper hopper) {
        Inventory mainInv = hopper.getMainInventory();
        int itemSlots = mainInv.getSize() - 9;

        List<Container> bottoms = getBottomContainers(hopper.getLocation().getBlock(), hopper.getOwnerUUID());
        if (bottoms.isEmpty()) return;

        boolean changed = false;
        for (int i = 0; i < itemSlots; i++) {
            ItemStack item = mainInv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            ItemStack remaining = item.clone();
            for (Container bottom : bottoms) {
                Map<Integer, ItemStack> leftovers = bottom.getInventory().addItem(remaining);
                if (leftovers.isEmpty()) {
                    remaining = null;
                    break;
                } else {
                    remaining = leftovers.get(0);
                }
            }

            if (remaining == null || remaining.getAmount() < item.getAmount()) {
                mainInv.setItem(i, remaining);
                changed = true;
            }
        }
        if (changed) hopper.markDirty();
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

    public void reloadPullerTask() {
        if (pullerTask != null)
            pullerTask.cancel();

        startPullingTask();
    }

    public void collectNearbyItems(ChunkHopper hopper) {
        Location loc = hopper.getLocation();
        if (loc.getWorld() == null) return;

        Chunk chunk = loc.getChunk();

        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof Item item)) continue;
            if (item.isDead()) continue;

            ItemStack drop = item.getItemStack();
            if (!hopper.shouldCollect(drop)) continue;

            int realAmount;
            if (plugin.getRoseStackerHook().isEnabled())
                realAmount = plugin.getRoseStackerHook().getStackedAmount(item);
            else
                realAmount = drop.getAmount();

            if (realAmount != drop.getAmount()) {
                drop = drop.clone();
                drop.setAmount(realAmount);
            }

            Block hopperBlock = loc.getBlock();
            List<Container> bottoms = getBottomContainers(hopperBlock, hopper.getOwnerUUID());
            ItemStack remaining = drop;

            for (Container bottom : bottoms) {
                Map<Integer, ItemStack> leftovers = bottom.getInventory().addItem(remaining);
                if (leftovers.isEmpty()) {
                    item.remove();
                    hopper.markDirty();
                    continue;
                }
                remaining = leftovers.get(0);
            }

            if (!hopper.canFitItem(remaining)) {
                item.teleport(loc.clone().add(0, 1, 0));
                hopper.notifyFull(plugin);
                continue;
            }

            Map<Integer, ItemStack> leftovers = hopper.getMainInventory().addItem(remaining);

            hopper.markDirty();

            if (leftovers.isEmpty())
                item.remove();
            else {
                item.setItemStack(leftovers.get(0));
                hopper.notifyFull(plugin);
            }
        }
    }

    public List<Container> getBottomContainers(Block hopper, UUID ownerUUID) {
        int limit = getContainerLimit(hopper, ownerUUID);

        List<Container> containers = new ArrayList<>();
        Block current = hopper.getRelative(0, -1, 0);
        int scanned = 0;

        while (current.getState() instanceof Container container && (limit == -1 || scanned < limit)) {
            containers.add(container);
            current = current.getRelative(0, -1, 0);
            scanned++;
        }

        return containers;
    }

    public int getContainerLimit(Block hopperBlock, UUID ownerUUID) {
        Player owner = Bukkit.getPlayer(ownerUUID);

        if (owner != null && owner.isOnline()) {
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

            storeContainerLimit(hopperBlock, maxLimit);
            return maxLimit;
        }

        return readContainerLimit(hopperBlock);
    }

    private void storeContainerLimit(Block hopperBlock, int maxLimit) {
        if (!(hopperBlock.getState() instanceof Hopper hopper))
            return;

        hopper.getPersistentDataContainer().set(containerLimitKey, PersistentDataType.INTEGER, maxLimit);
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

    public void flush() {
        for (ChunkHopper hopper : chunkHoppers.values())
            hopper.saveSync(plugin);

        hopperCounts.clear();
        chunkHoppers.clear();

        if (pullerTask != null)
            pullerTask.cancel();
    }
}
