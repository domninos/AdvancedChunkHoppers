package net.omni.ach.managers;

import net.kyori.adventure.text.Component;
import net.omni.ach.AdvancedChunkHoppers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

        Inventory mainGUI = createInventory(location, true, future);

        return future;
    }

    // should be sync
    // make sure to
    public Inventory createInventory(Location location, boolean loadFromDB, CompletableFuture<Inventory> future) {
        // TODO config size and title
        Inventory mainGUI = Bukkit.createInventory(null, 54, Component.text("Inventory"));

        // filler
        // TODO filter, whitelist, and blacklist item config
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");

        for (int i = 27; i < 54; i++) {
            if (i == 38) {
                mainGUI.setItem(38, createItem(Material.GREEN_WOOL, "Manage Whitelist"));
                continue;
            } else if (i == 42) {
                mainGUI.setItem(42, createItem(Material.REDSTONE_BLOCK, "Manage Blacklist"));
                continue;
            }

            mainGUI.setItem(i, filler);
        }

        if (loadFromDB) {
            // generate rows from the items stored in database
            plugin.getDatabaseManager().fetchItems(location).whenComplete((items, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);

                    Bukkit.getLogger().warning("An error has occurred while fetching items: " + throwable.getMessage());
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (int i = 0; i < Math.min(items.size(), 27); i++) {
                        mainGUI.setItem(i, items.get(i));
                    }

                    future.complete(mainGUI);
                });

            });
        }

        activeHopperCache.put(location, mainGUI);

        return mainGUI;
    }

    private ItemStack createItem(Material material, String name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(name));

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
        activeHopperCache.clear();
    }

}
