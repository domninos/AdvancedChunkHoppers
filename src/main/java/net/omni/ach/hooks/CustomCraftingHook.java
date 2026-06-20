package net.omni.ach.hooks;

import me.wolfyscript.customcrafting.CustomCrafting;
import me.wolfyscript.customcrafting.recipes.CustomRecipe;
import me.wolfyscript.customcrafting.recipes.items.Result;
import me.wolfyscript.utilities.api.inventory.custom_items.CustomItem;
import me.wolfyscript.utilities.util.NamespacedKey;
import net.omni.ach.AdvancedChunkHoppers;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class CustomCraftingHook implements Listener {

    private static final String TARGET_KEY = "customcrafting:utilities/chunk_hopper";
    private final AdvancedChunkHoppers plugin;
    private boolean enabled = false;

    public CustomCraftingHook(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    public void init() {
        this.enabled = true;
    }

    public boolean give(Player player, String recipeKeyString, int amount) {
        if (player == null)
            return false;

        NamespacedKey recipeKey = NamespacedKey.of(recipeKeyString);
        if (recipeKey == null) return false;

        CustomCrafting ccInstance = CustomCrafting.getPlugin(CustomCrafting.class);
        Optional<CustomRecipe<?>> recipeOptional = Optional.ofNullable(ccInstance.getRegistries().getRecipes().get(recipeKey));

        if (recipeOptional.isEmpty()) {
            plugin.sendConsole("[ERROR] Could not find a CustomCrafting recipe for key: " + recipeKeyString);
            return false;
        }

        Result recipeResult = recipeOptional.get().getResult();
        if (recipeResult == null) return false;

        ItemStack finalItem = recipeResult.getItemStack().clone();
        if (finalItem.getType().isAir()) return false;

        finalItem.setAmount(amount);

        ItemMeta meta = finalItem.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();

            pdc.set(
                    plugin.getChunkHopperManager().getAchKey(),
                    PersistentDataType.BYTE,
                    (byte) 1
            );

            finalItem.setItemMeta(meta);
        }

        player.getInventory().addItem(finalItem).values().forEach(overFlowItem ->
                player.getWorld().dropItemNaturally(player.getLocation(), overFlowItem)
        );

        return true;
    }

    public boolean isCustomChunkHopper(ItemStack item) {
        if (!isEnabled() || item == null || item.getType() != Material.HOPPER)
            return false;

        CustomItem customItem = CustomItem.getByItemStack(item);

        if (customItem != null) {
            NamespacedKey key = customItem.getNamespacedKey();

            if (key != null)
                return TARGET_KEY.equalsIgnoreCase(key.toString());
        }

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = String.valueOf(item.getItemMeta().displayName()).toLowerCase();

            return name.contains("chunk hopper");
        }

        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
