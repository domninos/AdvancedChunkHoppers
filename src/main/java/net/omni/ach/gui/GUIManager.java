package net.omni.ach.gui;

import net.omni.ach.AdvancedChunkHoppers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class GUIManager {

    private final NamespacedKey ownerKey;
    private final AdvancedChunkHoppers plugin;

    public GUIManager(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;

        this.ownerKey = new NamespacedKey(plugin, "chunk_hopper_owner");
    }

    public void openMainMenu(Player player, Block hopperBlock) {
        if (player == null)
            return;

        // check if hopper
        if (!(plugin.getChunkHopperManager().isACH(hopperBlock)))
            return;

        Hopper hopper = (Hopper) hopperBlock.getState();

        // check ownership
        String ownerUUID = hopper.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);

        if (ownerUUID == null || !ownerUUID.equals(player.getUniqueId().toString())) {
            // TODO messages.yml
            plugin.sendMessage(player, "<red>You do not have permission to open this hopper. </red>");
            return;
        }

        // close inventory if somehow they have another inventory opened
        player.closeInventory();

        // TODO config title
        plugin.getCacheManager().getOrCreate(hopperBlock.getLocation()).whenComplete((mainGUI, err) -> {
            if (err != null)
                Bukkit.getLogger().warning("An error has occurred while making GUI: " + err.getMessage());
            else
                player.openInventory(mainGUI);
        });
    }

    public boolean canFitItem(Inventory inventory, ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return false;

        int quantity = item.getAmount();

        for (int slot = 0; slot < 36; slot++) {
            ItemStack current = inventory.getItem(slot);

            // if empty
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

    public NamespacedKey getOwnerKey() {
        return this.ownerKey;
    }
}
