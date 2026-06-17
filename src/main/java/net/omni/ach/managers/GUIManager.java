package net.omni.ach.managers;

import net.brcdev.gangs.GangsPlusApi;
import net.brcdev.gangs.gang.Gang;
import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.chunkhopper.ChunkHopper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class GUIManager {

    private final NamespacedKey ownerKey;
    private final AdvancedChunkHoppers plugin;

    private final Map<UUID, Location> openHoppers = new HashMap<>();

    public GUIManager(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "chunk_hopper_owner");
    }

    public void openMainMenu(Player player, Block hopperBlock) {
        if (player == null || hopperBlock == null)
            return;

        if (!(plugin.getChunkHopperManager().isACH(hopperBlock)))
            return;

        UUID uuid = getOwnerUUID(hopperBlock);

        if (uuid == null)
            return;

        if (!isOwner(player, hopperBlock)) {
            if (!isGangMember(player, uuid)) {
                plugin.sendMessage(player, "<red>You do not have permission to open this hopper.</red>");
                return;
            }
        }

        Location loc = hopperBlock.getLocation();
        openHoppers.put(player.getUniqueId(), loc);

        plugin.getCacheManager().getOrCreate(loc).whenComplete((hopper, err) -> {
            if (err != null) {
                plugin.getLogger().warning("An error has occurred while making GUI: " + err.getMessage());
                openHoppers.remove(player.getUniqueId());
                return;
            }

            if (hopper != null) {
                Bukkit.getScheduler().runTask(plugin, () -> hopper.openMainMenu(player, plugin));
            } else {
                openHoppers.remove(player.getUniqueId());
            }
        });
    }

    public void openWhitelist(Player player, ChunkHopper hopper) {
        openHoppers.put(player.getUniqueId(), hopper.location());
        hopper.openWhitelist(player, plugin);
    }

    public void openBlacklist(Player player, ChunkHopper hopper) {
        openHoppers.put(player.getUniqueId(), hopper.location());
        hopper.openBlacklist(player, plugin);
    }

    public Location getOpenHopperLocation(Player player) {
        return openHoppers.get(player.getUniqueId());
    }

    public void removeOpenHopper(Player player) {
        openHoppers.remove(player.getUniqueId());
    }

    private boolean isGangMember(Player player, UUID ownerUUID) {
        if (!plugin.isGangsEnabled())
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

    public UUID getOwnerUUID(Block block) {
        if (block == null || !(block.getState() instanceof Hopper hopper))
            return null;

        String ownerUUID = hopper.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        return ownerUUID == null ? null : UUID.fromString(ownerUUID);
    }

    public boolean isOwner(Player player, Block block) {
        return getOwnerUUID(block) != null && getOwnerUUID(block).equals(player.getUniqueId());
    }

    public NamespacedKey getOwnerKey() {
        return this.ownerKey;
    }
}
