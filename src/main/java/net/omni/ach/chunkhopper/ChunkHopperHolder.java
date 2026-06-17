package net.omni.ach.chunkhopper;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public record ChunkHopperHolder(ChunkHopper hopper, InventoryType type) implements InventoryHolder {
    @Override
    public @NotNull Inventory getInventory() {
        return switch (type) {
            case MAIN -> hopper.getMainInventory();
            case WHITELIST -> hopper.getWhitelistInventory();
            case BLACKLIST -> hopper.getBlacklistInventory();
        };
    }
}
