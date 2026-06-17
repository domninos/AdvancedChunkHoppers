package net.omni.ach.hooks;

import dev.rosewood.rosestacker.api.RoseStackerAPI;
import dev.rosewood.rosestacker.stack.StackedItem;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public class RoseStackerHook {

    private boolean enabled = false;

    public void init() {
        this.enabled = true;
    }

    public int getStackedAmount(Item item) {
        if (!enabled || item == null)
            return item != null ? item.getItemStack().getAmount() : 1;

        try {
            StackedItem stackedItem = RoseStackerAPI.getInstance().getStackedItem(item);

            if (stackedItem != null)
                return stackedItem.getStackSize();

        } catch (Exception ignored) {
        }

        return item.getItemStack().getAmount();
    }

    public void dropStackedItem(ItemStack itemStack, Location location) {
        if (enabled) {
            RoseStackerAPI.getInstance().dropItemStack(itemStack, itemStack.getAmount(), location, true);
        } else
            location.getWorld().dropItemNaturally(location, itemStack);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
