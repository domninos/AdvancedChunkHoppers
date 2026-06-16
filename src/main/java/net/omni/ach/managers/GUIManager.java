package net.omni.ach.managers;

import net.brcdev.gangs.GangsPlusApi;
import net.brcdev.gangs.gang.Gang;
import net.omni.ach.AdvancedChunkHoppers;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.UUID;

public class GUIManager {

    private final NamespacedKey ownerKey;
    private final AdvancedChunkHoppers plugin;

    public GUIManager(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;

        this.ownerKey = new NamespacedKey(plugin, "chunk_hopper_owner");
    }

    public void openMainMenu(Player player, Block hopperBlock) {
        if (player == null || hopperBlock == null)
            return;

        // check if hopper
        if (!(plugin.getChunkHopperManager().isACH(hopperBlock)))
            return;

        UUID uuid = getOwnerUUID(hopperBlock);

        if (uuid == null)
            return;

        // check ownership
        if (!isOwner(player, hopperBlock)) {
            // TODO messages.yml
            plugin.sendMessage(player, "<red>You do not have permission to open this hopper.</red>");
            return;
        }

        // check gang
        if (plugin.isGangsEnabled()) {
            // check if team member of the owner
            if (GangsPlusApi.isInGang(player)) {
                Gang gang = GangsPlusApi.getPlayersGang(player);

                if (gang == null)
                    return;

                boolean isSameGang = gang.getAllMembers().stream().filter(Objects::nonNull).anyMatch(member -> member.getUniqueId().equals(uuid));

                if (!isSameGang) {
                    // TODO messages.yml
                    plugin.sendMessage(player, "<red>You do not have permission to open this hopper.</red>");
                    return;
                }
            }
        }

        // close inventory if somehow they have another inventory opened
        player.closeInventory();

        plugin.getCacheManager().getOrCreate(hopperBlock.getLocation()).whenComplete((mainGUI, err) -> {
            if (err != null)
                plugin.getLogger().warning("An error has occurred while making GUI: " + err.getMessage());
            else
                player.openInventory(mainGUI);
        });
    }

    public UUID getOwnerUUID(Block block) {
        if (block == null || !(block.getState() instanceof Hopper hopper))
            return null;

        String ownerUUID = hopper.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        return ownerUUID == null ? null : UUID.fromString(ownerUUID);
    }

    public boolean isOwner(Player player, Block block) {
        return getOwnerUUID(block) != null && getOwnerUUID(block).equals(player.getUniqueId());
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
