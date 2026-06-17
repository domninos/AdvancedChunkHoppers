package net.omni.ach.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.chunkhopper.ChunkHopper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryView;

public class GUIListener implements Listener {
    private final AdvancedChunkHoppers plugin;

    public GUIListener(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        Location loc = plugin.getGuiManager().getOpenHopperLocation(player);
        if (loc == null)
            return;

        ChunkHopper hopper = plugin.getCacheManager().getCachedHopper(loc);
        if (hopper == null)
            return;

        InventoryView view = event.getView();
        Component title = view.title();

        String mainTitle = plugin.getConfigUtil().getHopperTitle();
        Component mainComp = MiniMessage.miniMessage().deserialize(mainTitle);

        if (title.equals(mainComp)) {
            event.setCancelled(true);

            int slot = event.getRawSlot();

            if (slot < 0 || slot >= hopper.mainInventory().getSize())
                return;

            if (!hopper.isButtonSlot(slot, plugin)) {
                InventoryAction action = event.getAction();
                if (action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_HALF
                        || action == InventoryAction.PICKUP_ONE || action == InventoryAction.PICKUP_SOME
                        || action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE
                        || action == InventoryAction.PLACE_SOME
                        || action == InventoryAction.SWAP_WITH_CURSOR) {
                    event.setCancelled(false);
                }
                return;
            }

            if (hopper.isWhitelistSlot(slot, plugin)) {
                player.closeInventory();
                plugin.getGuiManager().openWhitelist(player, hopper);
            } else if (hopper.isBlacklistSlot(slot, plugin)) {
                player.closeInventory();
                plugin.getGuiManager().openBlacklist(player, hopper);
            }
        } else {
            String whitelistTitle = plugin.getConfigUtil().getWhitelistInventoryTitle();
            String blacklistTitle = plugin.getConfigUtil().getBlacklistInventoryTitle();
            Component whitelistComp = MiniMessage.miniMessage().deserialize(whitelistTitle);
            Component blacklistComp = MiniMessage.miniMessage().deserialize(blacklistTitle);

            if (title.equals(whitelistComp) || title.equals(blacklistComp)) {
                int slot = event.getRawSlot();
                int backSlot = title.equals(whitelistComp)
                        ? plugin.getConfigUtil().getWhitelistInventoryBackSlot()
                        : plugin.getConfigUtil().getBlacklistInventoryBackSlot();

                if (slot == backSlot) {
                    event.setCancelled(true);
                    player.closeInventory();
                    hopper.openMainMenu(player, plugin);
                    return;
                }

                int size = title.equals(whitelistComp)
                        ? plugin.getConfigUtil().getWhitelistInventorySize()
                        : plugin.getConfigUtil().getBlacklistInventorySize();
                int bottomStart = size - 9;

                if (slot >= bottomStart && slot < size) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player))
            return;

        InventoryView view = event.getView();
        Component title = view.title();

        String whitelistTitle = plugin.getConfigUtil().getWhitelistInventoryTitle();
        String blacklistTitle = plugin.getConfigUtil().getBlacklistInventoryTitle();
        Component whitelistComp = MiniMessage.miniMessage().deserialize(whitelistTitle);
        Component blacklistComp = MiniMessage.miniMessage().deserialize(blacklistTitle);

        Location loc = plugin.getGuiManager().getOpenHopperLocation(player);

        if (title.equals(whitelistComp) || title.equals(blacklistComp)) {
            if (loc != null) {
                ChunkHopper hopper = plugin.getCacheManager().getCachedHopper(loc);
                if (hopper != null) {
                    if (title.equals(whitelistComp))
                        hopper.applyWhitelistChanges(view.getTopInventory());
                    else
                        hopper.applyBlacklistChanges(view.getTopInventory());
                    hopper.save(plugin);
                }
            }
        } else {
            String mainTitle = plugin.getConfigUtil().getHopperTitle();
            Component mainComp = MiniMessage.miniMessage().deserialize(mainTitle);

            if (title.equals(mainComp) && loc != null) {
                ChunkHopper hopper = plugin.getCacheManager().getCachedHopper(loc);
                if (hopper != null)
                    hopper.save(plugin);
            }
        }

        plugin.getGuiManager().removeOpenHopper(player);
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
