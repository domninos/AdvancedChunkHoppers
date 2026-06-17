package net.omni.ach.listeners;

import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.chunkhopper.ChunkHopper;
import net.omni.ach.chunkhopper.ChunkHopperHolder;
import net.omni.ach.chunkhopper.InventoryType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {
    private final AdvancedChunkHoppers plugin;

    public GUIListener(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        InventoryView view = event.getView();
        Inventory top = view.getTopInventory();

        if (!(top.getHolder() instanceof ChunkHopperHolder(ChunkHopper hopper, InventoryType type)))
            return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        boolean isTop = slot < top.getSize();

        switch (type) {
            case MAIN -> handleMainClick(player, hopper, slot, isTop, event);
            case WHITELIST -> handleFilterClick(player, hopper, InventoryType.WHITELIST, slot, isTop, event, view);
            case BLACKLIST -> handleFilterClick(player, hopper, InventoryType.BLACKLIST, slot, isTop, event, view);
        }
    }

    private void handleMainClick(Player player, ChunkHopper hopper, int slot, boolean isTop, InventoryClickEvent event) {
        if (!isTop) {
            event.setCancelled(false);
            hopper.markDirty();
            return;
        }

        if (hopper.isButtonSlot(slot)) {
            if (hopper.isBackButtonSlot(slot, plugin)) {
                player.closeInventory();
                return;
            }
            if (hopper.isWhitelistSlot(slot, plugin)) {
                player.closeInventory();
                player.openInventory(hopper.buildWhitelistGUI(plugin));
            } else if (hopper.isBlacklistSlot(slot, plugin)) {
                player.closeInventory();
                player.openInventory(hopper.buildBlacklistGUI(plugin));
            }
            return;
        }

        event.setCancelled(false);
        hopper.markDirty();
    }

    private void handleFilterClick(Player player, ChunkHopper hopper, InventoryType type,
                                   int slot, boolean isTop, InventoryClickEvent event, InventoryView view) {
        boolean isWhitelist = type == InventoryType.WHITELIST;
        int size = isWhitelist
                ? plugin.getConfigUtil().getWhitelistInventorySize()
                : plugin.getConfigUtil().getBlacklistInventorySize();
        int backSlot = isWhitelist
                ? plugin.getConfigUtil().getWhitelistInventoryBackSlot()
                : plugin.getConfigUtil().getBlacklistInventoryBackSlot();
        int bottomStart = size - 9;

        if (slot == backSlot) {
            player.closeInventory();
            hopper.openMainMenu(player, plugin);
            return;
        }

        if (slot >= bottomStart && slot < size)
            return;

        if (!isTop) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                ItemStack moved = event.getCurrentItem();
                if (moved != null && moved.getType() != Material.AIR) {
                    Inventory top = view.getTopInventory();
                    int emptySlot = findEmptySlot(top, size - 9);
                    if (emptySlot != -1) {
                        ItemStack clone = moved.clone();
                        clone.setAmount(1);
                        top.setItem(emptySlot, clone);
                        playAddSound(player, isWhitelist);
                    } else {
                        plugin.sendMessage(player, "<red>Filter is full!</red>");
                    }
                }
            }
            return;
        }

        ItemStack cursor = event.getCursor();

        if (cursor.getType() != Material.AIR) {
            ItemStack clone = cursor.clone();
            clone.setAmount(1);
            event.setCurrentItem(clone);
            playAddSound(player, isWhitelist);
            return;
        }

        event.setCancelled(false);
    }

    private int findEmptySlot(Inventory inv, int limit) {
        for (int i = 0; i < limit; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() == Material.AIR)
                return i;
        }
        return -1;
    }

    private void playAddSound(Player player, boolean isWhitelist) {
        Sound sound = isWhitelist
                ? plugin.getConfigUtil().getWhitelistAddSound()
                : plugin.getConfigUtil().getBlacklistAddSound();
        float volume = isWhitelist
                ? plugin.getConfigUtil().getWhitelistAddSoundVolume()
                : plugin.getConfigUtil().getBlacklistAddSoundVolume();
        float pitch = isWhitelist
                ? plugin.getConfigUtil().getWhitelistAddSoundPitch()
                : plugin.getConfigUtil().getBlacklistAddSoundPitch();

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof ChunkHopperHolder holder))
            return;

        if (holder.type() != InventoryType.MAIN) {
            event.setCancelled(true);
        } else {
            holder.hopper().markDirty();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;

        InventoryView view = event.getView();
        Inventory top = view.getTopInventory();

        if (!(top.getHolder() instanceof ChunkHopperHolder(ChunkHopper hopper, InventoryType type)))
            return;

        switch (type) {
            case WHITELIST -> hopper.applyWhitelistChanges(top);
            case BLACKLIST -> hopper.applyBlacklistChanges(top);
            case MAIN -> {
            }
        }

        hopper.save(plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.HOPPER) return;

        if (!plugin.getChunkHopperManager().isACH(block))
            return;

        event.setCancelled(true);

        plugin.getGuiManager().openMainMenu(event.getPlayer(), block);
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
