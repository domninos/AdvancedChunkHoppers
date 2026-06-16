package net.omni.ach.listeners;

import net.omni.ach.AdvancedChunkHoppers;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class GUIListener implements Listener {
    private final AdvancedChunkHoppers plugin;

    public GUIListener(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryCLose(InventoryCloseEvent event) {
        if (!event.getView().title().equals("a")) {
        }
        // TODO instead of comparing title, use InventoryHolder

        // TODO check first if item contents changed

        // TODO save to cache when closing a bulk anvil
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.HOPPER) return;

        if (!plugin.getChunkHopperManager().isACH(block))
            return;

        // safe to open chunk hopper inventory
        event.setCancelled(true);

        plugin.getGuiManager().openMainMenu(event.getPlayer(), block);
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
