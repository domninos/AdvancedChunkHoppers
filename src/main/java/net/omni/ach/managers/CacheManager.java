package net.omni.ach.managers;

import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.chunkhopper.ChunkHopper;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CacheManager {

    private final Map<Location, ChunkHopper> activeHopperCache = new HashMap<>();
    private final AdvancedChunkHoppers plugin;

    public CacheManager(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<ChunkHopper> getOrCreate(Location location) {
        CompletableFuture<ChunkHopper> future = new CompletableFuture<>();

        if (activeHopperCache.containsKey(location)) {
            future.complete(activeHopperCache.get(location));
            return future;
        }

        plugin.getChunkHopperManager().load(location).whenComplete((hopper, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
                return;
            }

            if (hopper != null) {
                activeHopperCache.put(location, hopper);
                future.complete(hopper);
            } else {
                future.complete(null);
            }
        });

        return future;
    }

    public boolean hasCache(Location location) {
        return activeHopperCache.containsKey(location);
    }

    public ChunkHopper getCachedHopper(Location location) {
        return activeHopperCache.get(location);
    }

    public void putIfAbsent(Location location, ChunkHopper hopper) {
        if (!activeHopperCache.containsKey(location))
            activeHopperCache.put(location, hopper);
    }

    public void invalidate(Location location) {
        ChunkHopper hopper = activeHopperCache.remove(location);
        if (hopper != null) {
            hopper.save(plugin);
        }
    }

    public void discard(Location location) {
        activeHopperCache.remove(location);
    }

    public void invalidateAll() {
        for (ChunkHopper hopper : activeHopperCache.values()) {
            hopper.saveSync(plugin);
        }
        activeHopperCache.clear();
    }
}
