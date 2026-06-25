package net.omni.ach.handlers;

import net.omni.ach.AdvancedChunkHoppers;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ACHItemHandler {

    private final AdvancedChunkHoppers plugin;
    private ItemStack hopperItem;

    public ACHItemHandler(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (plugin.getCustomCraftingHook().isEnabled())
            this.hopperItem = plugin.getCustomCraftingHook().loadCustomHopper();
        else {
            this.hopperItem = createItemStack(1);

            plugin.sendConsole("<yellow>CustomCrafting not found. Using ACH Item..</yellow>");
        }
    }

    private ItemStack createItemStack(int amount) {
        ItemStack item = new ItemStack(plugin.getConfigUtil().getHopperItemMaterial(), amount);

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        String displayName = plugin.getConfigUtil().getHopperItemDisplayName();
        if (displayName != null && !displayName.isEmpty())
            plugin.getChatRenderer().setDisplayName(meta, displayName);

        List<String> lore = plugin.getConfigUtil().getHopperItemLore();
        if (lore != null && !lore.isEmpty())
            plugin.getChatRenderer().setLore(meta, lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(plugin.getChunkHopperManager().getAchKey(), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isCustomChunkHopper(ItemStack item) {
        // check first if customcrafting is enabled
        if (plugin.getCustomCraftingHook().isEnabled())
            return plugin.getCustomCraftingHook().isCustomChunkHopper(item);

        if (item == null || item.getType() != Material.HOPPER)
            return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(plugin.getChunkHopperManager().getAchKey(), PersistentDataType.BYTE);
    }

    public boolean give(Player player, int amount) {
        if (player == null)
            return false;

        if (plugin.getCustomCraftingHook().isEnabled())
            return plugin.getCustomCraftingHook().give(player, amount);

        ItemStack item = getItemStack(amount);

        if (item == null || item.getType().isAir())
            return false;

        player.getInventory().addItem(item).values().forEach(overflow ->
                player.getWorld().dropItemNaturally(player.getLocation(), overflow));

        return true;
    }

    public ItemStack getItemStack(int amount) {
        if (hopperItem == null)
            return createItemStack(amount);

        ItemStack clone = hopperItem.clone();
        clone.setAmount(amount);
        return clone;
    }

    public void flush() {
        this.hopperItem = null;
    }
}
