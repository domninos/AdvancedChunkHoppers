package net.omni.ach.managers;

import net.omni.ach.chunkhopper.ChunkHopper;
import org.bukkit.inventory.ItemStack;

public class FilterManager {

    public boolean shouldCollect(ChunkHopper hopper, ItemStack itemStack) {
        if (hopper == null || itemStack == null)
            return false;

        return hopper.shouldCollect(itemStack);
    }

    public void init() {
    }

    public void flush() {
    }
}
