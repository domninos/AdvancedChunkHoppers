package net.omni.ach.managers;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class FilterManager {

    private static final Set<ItemStack> WHITELISTED = new HashSet<>();
    private static final Set<ItemStack> BLACKLISTED = new HashSet<>();

    private boolean useWhitelist = false;

    public void init() {
        // load useWhitelist

    }

    public boolean shouldCollect(Location hopperLocation, ItemStack itemStack) {
        // TODO check if the specific hopper is collecting any items
        return false;
    }

    public void addToWhitelist(ItemStack itemStack) {
        WHITELISTED.add(itemStack);
    }

    public void addToBlacklist(ItemStack itemStack) {
        BLACKLISTED.add(itemStack);
    }


    public void flush() {
        WHITELISTED.clear();
        BLACKLISTED.clear();
    }


}
