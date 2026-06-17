package net.omni.ach.hooks;

import me.wolfyscript.utilities.api.inventory.custom_items.CustomItem;
import me.wolfyscript.utilities.util.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class CustomCraftingHook {

    private static final String TARGET_KEY = "customcrafting:utilities/chunk_hopper";

    private boolean enabled = false;

    public void init() {
        try {
            Class.forName("me.wolfyscript.customcrafting.CustomCrafting");
            this.enabled = true;
        } catch (ClassNotFoundException e) {
            this.enabled = false;
        }
    }

    public boolean isCustomChunkHopper(ItemStack item) {
        if (!isEnabled() || item == null || item.getType() != Material.HOPPER)
            return false;

        CustomItem customItem = CustomItem.getByItemStack(item);

        if (customItem != null) {
            NamespacedKey key = customItem.getNamespacedKey();
            return key != null && TARGET_KEY.equals(key.toString());
        }

        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
