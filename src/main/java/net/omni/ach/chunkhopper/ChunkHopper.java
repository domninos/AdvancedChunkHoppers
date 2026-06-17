package net.omni.ach.chunkhopper;

import net.kyori.adventure.text.Component;
import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ChunkHopper(Location location, UUID ownerUUID, Inventory mainInventory, Inventory whitelistInventory,
                          Inventory blacklistInventory) {

    public static final int ITEM_SLOTS = 36;

    public boolean isOwner(Player player) {
        return ownerUUID.equals(player.getUniqueId());
    }

    public boolean canFitItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return false;

        int quantity = item.getAmount();

        for (int slot = 0; slot < ITEM_SLOTS; slot++) {
            ItemStack current = mainInventory.getItem(slot);

            if (current == null || current.getType() == Material.AIR)
                return true;

            if (current.isSimilar(item)) {
                int space = current.getMaxStackSize() - current.getAmount();

                if (space > 0) {
                    quantity -= space;

                    if (quantity <= 0)
                        return true;
                }
            }
        }

        return false;
    }

    public boolean shouldCollect(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return false;

        boolean hasWhitelist = hasItems(whitelistInventory);
        boolean hasBlacklist = hasItems(blacklistInventory);

        if (hasWhitelist)
            return isInFilter(whitelistInventory, item);

        if (hasBlacklist)
            return !isInFilter(blacklistInventory, item);

        return true;
    }

    private boolean hasItems(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR)
                return true;
        }
        return false;
    }

    private boolean isInFilter(Inventory filter, ItemStack item) {
        for (ItemStack filterItem : filter.getContents()) {
            if (filterItem != null && filterItem.isSimilar(item))
                return true;
        }
        return false;
    }

    public void save(AdvancedChunkHoppers plugin) {
        List<ItemStack> items = extractItems(mainInventory, ITEM_SLOTS);
        List<ItemStack> whitelist = extractItems(whitelistInventory, whitelistInventory.getSize());
        List<ItemStack> blacklist = extractItems(blacklistInventory, blacklistInventory.getSize());

        plugin.getDatabaseManager().saveFull(location, ownerUUID.toString(), items, whitelist, blacklist);
    }

    private List<ItemStack> extractItems(Inventory inv, int limit) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, inv.getSize()); i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() != Material.AIR)
                result.add(it);
        }
        return result;
    }

    public void openMainMenu(Player player, AdvancedChunkHoppers plugin) {
        player.closeInventory();
        setupMainButtons(plugin);
        player.openInventory(mainInventory);
    }

    private void setupMainButtons(AdvancedChunkHoppers plugin) {
        int size = plugin.getConfigUtil().getHopperSize();

        for (int i = size - 1; i >= (size - 9); i--)
            mainInventory.setItem(i, createItem(plugin.getConfigUtil().getFillerMat(),
                    plugin.getConfigUtil().getFillerDisplayName()));

        mainInventory.setItem(plugin.getConfigUtil().getWhitelistSlot(),
                createItem(plugin.getConfigUtil().getWhitelistMat(),
                        plugin.getConfigUtil().getWhitelistDisplayName(),
                        plugin.getConfigUtil().getWhitelistLore().stream()
                                .map(MessageUtil::parse).toArray(Component[]::new)));

        mainInventory.setItem(plugin.getConfigUtil().getBackButtonSlot(),
                createItem(plugin.getConfigUtil().getBackButtonMat(),
                        plugin.getConfigUtil().getBackButtonDisplayName(),
                        plugin.getConfigUtil().getBackButtonLore().stream()
                                .map(MessageUtil::parse).toArray(Component[]::new)));

        mainInventory.setItem(plugin.getConfigUtil().getBlacklistSlot(),
                createItem(plugin.getConfigUtil().getBlacklistMat(),
                        plugin.getConfigUtil().getBlacklistDisplayName(),
                        plugin.getConfigUtil().getBlacklistLore().stream()
                                .map(MessageUtil::parse).toArray(Component[]::new)));
    }

    private ItemStack createItem(Material material, String name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.customName(MessageUtil.parse(name));

        if (lore != null && lore.length > 0)
            meta.lore(List.of(lore));

        item.setItemMeta(meta);
        return item;
    }

    public void openWhitelist(Player player, AdvancedChunkHoppers plugin) {
        Inventory gui = buildFilterGUI(
                plugin.getConfigUtil().getWhitelistInventorySize(),
                plugin.getConfigUtil().getWhitelistInventoryTitle(),
                plugin.getConfigUtil().getWhitelistInventoryBackSlot(),
                whitelistInventory,
                plugin
        );
        player.openInventory(gui);
    }

    private Inventory buildFilterGUI(int size, String title, int backSlot,
                                     Inventory filterSource, AdvancedChunkHoppers plugin) {
        Inventory gui = Bukkit.createInventory(null, size, MessageUtil.parse(title));

        for (int i = size - 1; i >= (size - 9); i--)
            gui.setItem(i, createItem(plugin.getConfigUtil().getFillerMat(),
                    plugin.getConfigUtil().getFillerDisplayName()));

        gui.setItem(backSlot,
                createItem(plugin.getConfigUtil().getBackButtonMat(),
                        plugin.getConfigUtil().getBackButtonDisplayName(),
                        plugin.getConfigUtil().getBackButtonLore().stream()
                                .map(MessageUtil::parse).toArray(Component[]::new)));

        for (int i = 0; i < Math.min(size - 9, filterSource.getSize()); i++) {
            ItemStack it = filterSource.getItem(i);
            if (it != null && it.getType() != Material.AIR)
                gui.setItem(i, it.clone());
        }

        return gui;
    }

    public void openBlacklist(Player player, AdvancedChunkHoppers plugin) {
        Inventory gui = buildFilterGUI(
                plugin.getConfigUtil().getBlacklistInventorySize(),
                plugin.getConfigUtil().getBlacklistInventoryTitle(),
                plugin.getConfigUtil().getBlacklistInventoryBackSlot(),
                blacklistInventory,
                plugin
        );
        player.openInventory(gui);
    }

    public boolean isButtonSlot(int slot, AdvancedChunkHoppers plugin) {
        int size = plugin.getConfigUtil().getHopperSize();
        int bottomStart = size - 9;

        return slot >= bottomStart;
    }

    public boolean isWhitelistSlot(int slot, AdvancedChunkHoppers plugin) {
        return slot == plugin.getConfigUtil().getWhitelistSlot();
    }

    public boolean isBlacklistSlot(int slot, AdvancedChunkHoppers plugin) {
        return slot == plugin.getConfigUtil().getBlacklistSlot();
    }

    public void applyWhitelistChanges(Inventory view) {
        for (int i = 0; i < Math.min(view.getSize(), whitelistInventory.getSize()); i++)
            whitelistInventory.setItem(i, view.getItem(i));
    }

    public void applyBlacklistChanges(Inventory view) {
        for (int i = 0; i < Math.min(view.getSize(), blacklistInventory.getSize()); i++)
            blacklistInventory.setItem(i, view.getItem(i));
    }
}
