package net.omni.ach.listeners;

import net.omni.ach.AdvancedChunkHoppers;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class GUIListener implements Listener {
    private final AdvancedChunkHoppers plugin;

    public GUIListener(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryCLose(InventoryCloseEvent event) {
        if (!event.getView().title().equals("a")) return;

        // TODO save to cache when closing a bulk anvil
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
