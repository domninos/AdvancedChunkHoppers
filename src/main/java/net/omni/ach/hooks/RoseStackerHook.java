package net.omni.ach.hooks;

import dev.rosewood.rosestacker.api.RoseStackerAPI;
import dev.rosewood.rosestacker.stack.StackedItem;
import org.bukkit.entity.Item;

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

    public boolean isEnabled() {
        return enabled;
    }
}
