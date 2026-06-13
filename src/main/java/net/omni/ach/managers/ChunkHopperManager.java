package net.omni.ach.managers;

import net.omni.ach.AdvancedChunkHoppers;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChunkHopperManager {

    private final AdvancedChunkHoppers plugin;

    private final Map<Long, Location> chunkHoppers = new HashMap<>();
    private final Map<UUID, Integer> hopperLimits = new HashMap<>();

    public ChunkHopperManager(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    public void init() {
        // TODO load from db
    }

    public void registerHopper(Chunk chunk, Location location) {
        chunkHoppers.put(chunk.getChunkKey(), location);
    }

    public void loadAsync(Location location) {
        loadFromChunkAsync(location.getChunk());
    }

    public void loadFromChunkAsync(Chunk chunk) {
        if (hasHopper(chunk)) {
            // TODO
            // already loaded

            return;
        }

        // TODO from database

    }

    public boolean hasHopper(Chunk chunk) {
        if (chunk == null)
            return false;

        if (!chunk.isLoaded() || !chunk.isEntitiesLoaded())
            return false;

        return chunkHoppers.containsKey(chunk.getChunkKey());
    }

    public boolean isACH(Block block) {
        if (!(block.getState() instanceof Hopper hopper))
            return false;

        // check pdc
        PersistentDataContainer pdc = hopper.getPersistentDataContainer();

        return pdc.has(plugin.getGuiManager().getOwnerKey(), PersistentDataType.STRING);
    }

    public Hopper getChunkHopper(Chunk chunk) {
        if (!hasHopper(chunk))
            return null;

        Location location = chunkHoppers.get(chunk.getChunkKey());

        if (location == null)
            return null;

        if (location.getBlock().getState() instanceof Hopper hopper)
            return hopper;

        unregisterHopper(chunk);
        return null;
    }

    public void unregisterHopper(Chunk chunk) {
        chunkHoppers.remove(chunk.getChunkKey());
    }

    public int getHopperLimit(Player player) {
        if (player.hasPermission("ach.limit.none"))
            return -1;

        // TODO update this if for example the limits get changed
        if (hopperLimits.containsKey(player.getUniqueId()))
            return hopperLimits.get(player.getUniqueId());

        // TODO max limit config (default max: 100)
        for (int i = 100; i > 0; i--) {
            if (player.hasPermission("ach.limit." + i)) {
                hopperLimits.put(player.getUniqueId(), i);
                return i;
            }
        }

        // TODO general limit config (default limit: 5)
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
        // TODO save to db

        hopperLimits.clear();
        chunkHoppers.clear();
    }
}
