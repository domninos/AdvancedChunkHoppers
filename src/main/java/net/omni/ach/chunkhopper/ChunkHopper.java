package net.omni.ach.chunkhopper;

import net.kyori.adventure.text.Component;
import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.util.MessageUtil;
import net.omni.ach.util.Messages;
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

public class ChunkHopper {

    private static final long WARNING_COOLDOWN_MS = 5000;
    private final Location location;
    private final UUID ownerUUID;
    private final Inventory mainInventory;
    private final Inventory whitelistInventory;
    private final Inventory blacklistInventory;
    private long lastFullWarning = 0;
    private boolean dirty = false;

    public ChunkHopper(Location location, UUID ownerUUID, AdvancedChunkHoppers plugin) {
        this.location = location;
        this.ownerUUID = ownerUUID;

        int mainSize = plugin.getConfigUtil().getHopperSize();
        int whitelistSize = plugin.getConfigUtil().getWhitelistInventorySize();
        int blacklistSize = plugin.getConfigUtil().getBlacklistInventorySize();

        this.mainInventory = Bukkit.createInventory(
                new ChunkHopperHolder(this, InventoryType.MAIN),
                mainSize,
                MessageUtil.parse(plugin.getConfigUtil().getHopperTitle()));

        this.whitelistInventory = Bukkit.createInventory(
                new ChunkHopperHolder(this, InventoryType.WHITELIST),
                whitelistSize,
                MessageUtil.parse(plugin.getConfigUtil().getWhitelistInventoryTitle()));

        this.blacklistInventory = Bukkit.createInventory(
                new ChunkHopperHolder(this, InventoryType.BLACKLIST),
                blacklistSize,
                MessageUtil.parse(plugin.getConfigUtil().getBlacklistInventoryTitle()));

        setupMainButtons(plugin);
    }

    private void setupMainButtons(AdvancedChunkHoppers plugin) {
        int size = mainInventory.getSize();

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

    public void loadItems(List<ItemStack> items,
                          List<ItemStack> whitelist, List<ItemStack> blacklist) {
        int mainItemSlots = mainInventory.getSize() - 9;
        int whitelistItemSlots = whitelistInventory.getSize() - 9;
        int blacklistItemSlots = blacklistInventory.getSize() - 9;

        fillSlots(mainInventory, items, mainItemSlots);
        fillSlots(whitelistInventory, whitelist, whitelistItemSlots);
        fillSlots(blacklistInventory, blacklist, blacklistItemSlots);
    }

    private void fillSlots(Inventory inv, List<ItemStack> items, int limit) {
        for (int i = 0; i < Math.min(items.size(), limit); i++) {
            ItemStack it = items.get(i);
            if (it != null && it.getType() != Material.AIR)
                inv.setItem(i, it);
        }
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public Inventory getMainInventory() {
        return mainInventory;
    }

    public Inventory getWhitelistInventory() {
        return whitelistInventory;
    }

    public Inventory getBlacklistInventory() {
        return blacklistInventory;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean canFitItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return false;

        int quantity = item.getAmount();
        int itemSlots = mainInventory.getSize() - 9;

        for (int slot = 0; slot < itemSlots; slot++) {
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
        int itemSlots = inventory.getSize() - 9;

        for (int i = 0; i < itemSlots; i++) {
            ItemStack it = inventory.getItem(i);

            if (it != null && it.getType() != Material.AIR)
                return true;
        }

        return false;
    }

    private boolean isInFilter(Inventory filter, ItemStack item) {
        int itemSlots = filter.getSize() - 9;
        for (int i = 0; i < itemSlots; i++) {
            ItemStack filterItem = filter.getItem(i);
            if (filterItem != null && filterItem.isSimilar(item))
                return true;
        }
        return false;
    }

    public void save(AdvancedChunkHoppers plugin) {
        if (!dirty)
            return;

        List<ItemStack> items = extractItems(mainInventory, mainInventory.getSize() - 9);
        List<ItemStack> whitelist = extractItems(whitelistInventory, whitelistInventory.getSize() - 9);
        List<ItemStack> blacklist = extractItems(blacklistInventory, blacklistInventory.getSize() - 9);

        plugin.getDatabaseManager().saveFull(location, ownerUUID.toString(), items, whitelist, blacklist);
        dirty = false;
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

    public void saveSync(AdvancedChunkHoppers plugin) {
        if (!dirty)
            return;

        List<ItemStack> items = extractItems(mainInventory, mainInventory.getSize() - 9);
        List<ItemStack> whitelist = extractItems(whitelistInventory, whitelistInventory.getSize() - 9);
        List<ItemStack> blacklist = extractItems(blacklistInventory, blacklistInventory.getSize() - 9);

        plugin.getDatabaseManager().saveFullSync(location, ownerUUID.toString(), items, whitelist, blacklist);
        dirty = false;
    }

    public void notifyFull(AdvancedChunkHoppers plugin) {
        long now = System.currentTimeMillis();

        if (now - lastFullWarning < WARNING_COOLDOWN_MS)
            return;

        lastFullWarning = now;

        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null && owner.isOnline()) {
            plugin.sendMessage(owner, Messages.HOPPER_FULL.replace(
                    "x", String.valueOf(location.getBlockX()),
                    "y", String.valueOf(location.getBlockY()),
                    "z", String.valueOf(location.getBlockZ())));
        }
    }

    public void openMainMenu(Player player, AdvancedChunkHoppers plugin) {
        player.closeInventory();
        setupMainButtons(plugin);
        player.openInventory(mainInventory);
    }

    public Inventory buildWhitelistGUI(AdvancedChunkHoppers plugin) {
        return buildFilterGUI(
                plugin.getConfigUtil().getWhitelistInventorySize(),
                plugin.getConfigUtil().getWhitelistInventoryTitle(),
                plugin.getConfigUtil().getWhitelistInventoryBackSlot(),
                whitelistInventory,
                InventoryType.WHITELIST,
                plugin);
    }

    private Inventory buildFilterGUI(int size, String title, int backSlot,
                                     Inventory filterSource, InventoryType type,
                                     AdvancedChunkHoppers plugin) {
        Inventory gui = Bukkit.createInventory(
                new ChunkHopperHolder(this, type),
                size,
                MessageUtil.parse(title));

        for (int i = size - 1; i >= (size - 9); i--)
            gui.setItem(i, createItem(plugin.getConfigUtil().getFillerMat(),
                    plugin.getConfigUtil().getFillerDisplayName()));

        gui.setItem(backSlot,
                createItem(plugin.getConfigUtil().getBackButtonMat(),
                        plugin.getConfigUtil().getBackButtonDisplayName(),
                        plugin.getConfigUtil().getBackButtonLore().stream()
                                .map(MessageUtil::parse).toArray(Component[]::new)));

        int itemSlots = size - 9;

        for (int i = 0; i < Math.min(itemSlots, filterSource.getSize() - 9); i++) {
            ItemStack it = filterSource.getItem(i);

            if (it != null && it.getType() != Material.AIR)
                gui.setItem(i, it.clone());
        }

        return gui;
    }

    public Inventory buildBlacklistGUI(AdvancedChunkHoppers plugin) {
        return buildFilterGUI(
                plugin.getConfigUtil().getBlacklistInventorySize(),
                plugin.getConfigUtil().getBlacklistInventoryTitle(),
                plugin.getConfigUtil().getBlacklistInventoryBackSlot(),
                blacklistInventory,
                InventoryType.BLACKLIST,
                plugin);
    }

    public void applyWhitelistChanges(Inventory view) {
        int limit = Math.min(view.getSize() - 9, whitelistInventory.getSize() - 9);

        for (int i = 0; i < limit; i++)
            whitelistInventory.setItem(i, view.getItem(i));

        for (int i = limit; i < whitelistInventory.getSize() - 9; i++)
            whitelistInventory.setItem(i, null);

        this.dirty = true;
    }

    public void applyBlacklistChanges(Inventory view) {
        int limit = Math.min(view.getSize() - 9, blacklistInventory.getSize() - 9);

        for (int i = 0; i < limit; i++)
            blacklistInventory.setItem(i, view.getItem(i));

        for (int i = limit; i < blacklistInventory.getSize() - 9; i++)
            blacklistInventory.setItem(i, null);

        this.dirty = true;
    }

    public boolean isButtonSlot(int slot) {
        int size = mainInventory.getSize();
        return slot >= size - 9;
    }

    public boolean isWhitelistSlot(int slot, AdvancedChunkHoppers plugin) {
        return slot == plugin.getConfigUtil().getWhitelistSlot();
    }

    public boolean isBlacklistSlot(int slot, AdvancedChunkHoppers plugin) {
        return slot == plugin.getConfigUtil().getBlacklistSlot();
    }

    public boolean isBackButtonSlot(int slot, AdvancedChunkHoppers plugin) {
        return slot == plugin.getConfigUtil().getBackButtonSlot();
    }
}
