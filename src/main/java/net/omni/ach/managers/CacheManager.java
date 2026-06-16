package net.omni.ach.managers;

import net.kyori.adventure.text.Component;
import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CacheManager {

    private final Map<Location, Inventory> activeHopperCache = new HashMap<>();

    private final AdvancedChunkHoppers plugin;

    public CacheManager(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Inventory> getOrCreate(Location location) {
        CompletableFuture<Inventory> future = new CompletableFuture<>();

        // check
        if (activeHopperCache.containsKey(location)) {
            future.complete(activeHopperCache.get(location));

            return future;
        }

        createInventory(location, future);

        return future;
    }

    public void createInventory(Location location, @Nullable CompletableFuture<Inventory> future) {
        int hopperSize = plugin.getConfigUtil().getHopperSize();

        Inventory mainGUI = Bukkit.createInventory(null, hopperSize, MessageUtil.parse(plugin.getConfigUtil().getHopperTitle()));

        // filler
        for (int i = hopperSize - 1; i >= (hopperSize - 9); i--)
            mainGUI.setItem(i, createItem(plugin.getConfigUtil().getFillerMat(), plugin.getConfigUtil().getFillerDisplayName()));

        // whitelist
        mainGUI.setItem(plugin.getConfigUtil().getWhitelistSlot(),
                createItem(plugin.getConfigUtil().getWhitelistMat(),
                        plugin.getConfigUtil().getWhitelistDisplayName()));

        // back button
        mainGUI.setItem(plugin.getConfigUtil().getBackButtonSlot(),
                createItem(plugin.getConfigUtil().getBackButtonMat(),
                        plugin.getConfigUtil().getBackButtonDisplayName()));

        // blacklist
        mainGUI.setItem(plugin.getConfigUtil().getBlacklistSlot(),
                createItem(plugin.getConfigUtil().getBlacklistMat(),
                        plugin.getConfigUtil().getBlacklistDisplayName()));

        // TODO only store in inventory if the chest underneath is null or are all full

        if (future != null) {
            // generate rows from the items stored in database
            plugin.getDatabaseManager().fetchItems(location).whenComplete((items, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);

                    plugin.getLogger().warning("An error has occurred while fetching items: " + throwable.getMessage());
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (int i = 0; i < Math.min(items.size(), 27); i++)
                        mainGUI.setItem(i, items.get(i));

                    future.complete(mainGUI);
                });

            });
        }

        // TODO use ChunkHopper
        // TODO load whitelist and blacklist

        activeHopperCache.put(location, mainGUI);
    }

    private ItemStack createItem(Material material, String name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.customName(MessageUtil.parse(name));

        if (lore != null && lore.length > 0)
            meta.lore(List.of(lore));

        item.setItemMeta(meta);
        return item;
    }

    public boolean hasCache(Location location) {
        return activeHopperCache.containsKey(location);
    }

    public Inventory getCachedInventory(Location location) {
        return activeHopperCache.get(location);
    }

    public void invalidate(Location location) {
        activeHopperCache.remove(location);
    }

    public Map<Location, Inventory> getActiveHopperCache() {
        return this.activeHopperCache;
    }

    // TODO make sure everything is saved before invalidating cache
    public void invalidateAll() {
        // TODO db

        activeHopperCache.clear();
    }

}
