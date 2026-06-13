package net.omni.ach.managers;

import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.util.ItemSerializationUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public record StorageManager(NamespacedKey storageKey) {

    public StorageManager(AdvancedChunkHoppers storageKey) {
        this(new NamespacedKey(storageKey, "chunk_hopper_storage"));
    }

    public List<ItemStack> getItems(Block hopper) {
        if (!(hopper.getState() instanceof TileState tileState))
            return List.of();

        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        String data = pdc.get(storageKey, PersistentDataType.STRING);

        if (data == null)
            return List.of();

        return ItemSerializationUtil.fromBase64(data);
    }

    public void saveItems(Block hopper, List<ItemStack> items) {
        if (!(hopper.getState() instanceof TileState tileState))
            return;

        PersistentDataContainer pdc = tileState.getPersistentDataContainer();

        if (items.isEmpty())
            pdc.remove(storageKey);
        else
            pdc.set(storageKey, PersistentDataType.STRING, ItemSerializationUtil.toBase64(items));

        tileState.update();
    }
}
