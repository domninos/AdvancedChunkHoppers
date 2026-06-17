package net.omni.ach.managers;

import net.brcdev.gangs.GangsPlusApi;
import net.brcdev.gangs.gang.Gang;
import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.UUID;

public class GUIManager {

    private final AdvancedChunkHoppers plugin;

    public GUIManager(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player, Block hopperBlock) {
        if (player == null || hopperBlock == null)
            return;

        if (!(plugin.getChunkHopperManager().isACH(hopperBlock)))
            return;

        UUID uuid = getOwnerUUID(hopperBlock);

        if (uuid == null)
            return;

        if (!isOwner(player, hopperBlock) && !isGangMember(player, uuid)) {
            plugin.sendMessage(player, Messages.NO_OPEN_PERMS.toString());
            return;
        }

        plugin.getCacheManager().getOrCreate(hopperBlock.getLocation()).whenComplete((hopper, err) -> {
            if (err != null) {
                plugin.getLogger().warning("An error has occurred while making GUI: " + err.getMessage());
                return;
            }

            if (hopper != null) {
                Bukkit.getScheduler().runTask(plugin, () -> hopper.openMainMenu(player, plugin));
            }
        });
    }

    public UUID getOwnerUUID(Block block) {
        if (block == null || !(block.getState() instanceof Hopper hopper))
            return null;

        String ownerUUID = hopper.getPersistentDataContainer().get(plugin.getChunkHopperManager().getOwnerKey(), PersistentDataType.STRING);
        return ownerUUID == null ? null : UUID.fromString(ownerUUID);
    }

    public boolean isOwner(Player player, Block block) {
        return getOwnerUUID(block) != null && getOwnerUUID(block).equals(player.getUniqueId());
    }

    private boolean isGangMember(Player player, UUID ownerUUID) {
        if (!plugin.getGangsPlusHook().isEnabled())
            return false;

        if (!GangsPlusApi.isInGang(player))
            return false;

        Gang gang = GangsPlusApi.getPlayersGang(player);
        if (gang == null)
            return false;

        return gang.getAllMembers().stream()
                .filter(Objects::nonNull)
                .anyMatch(member -> member.getUniqueId().equals(ownerUUID));
    }
}
