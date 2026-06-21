package net.omni.ach.hooks;

import com.wolfyscript.utilities.bukkit.world.items.reference.StackReference;
import me.wolfyscript.customcrafting.CustomCrafting;
import me.wolfyscript.customcrafting.listeners.customevents.CustomPreCraftEvent;
import me.wolfyscript.customcrafting.recipes.CustomRecipe;
import me.wolfyscript.customcrafting.recipes.items.Result;
import me.wolfyscript.utilities.util.NamespacedKey;
import net.omni.ach.AdvancedChunkHoppers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class CustomCraftingHook implements Listener {

    private static final String TARGET_KEY = "customcrafting:utilities/chunk_hopper";
    private final AdvancedChunkHoppers plugin;
    private ItemStack custom_hopper;
    private boolean enabled = false;

    public CustomCraftingHook(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    public void loadCustomHopper() {
        NamespacedKey recipeKey = NamespacedKey.of(TARGET_KEY);

        if (recipeKey == null)
            return;

        Optional<CustomRecipe<?>> recipeOptional = Optional.ofNullable(CustomCrafting.inst().getRegistries().getRecipes().get(recipeKey));

        if (recipeOptional.isEmpty()) {
            plugin.sendConsole("[ERROR] Could not find a CustomCrafting recipe for key: " + TARGET_KEY);
            return;
        }

        this.custom_hopper = recipeOptional.get().getResult().getItemStack();
        plugin.sendConsole("<yellow>Found " + TARGET_KEY + "!</yellow>");
    }

    public void init() {
        this.enabled = true;
        loadCustomHopper();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPreCraft(CustomPreCraftEvent event) {
        if (event.getRecipe() == null || event.getRecipe().getResult() == null)
            return;

        ItemStack resultItem = event.getResult().getItemStack();

        if (custom_hopper != null && custom_hopper.isSimilar(resultItem)) {
            ItemMeta meta = resultItem.getItemMeta();

            if (meta != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(plugin.getChunkHopperManager().getAchKey(), PersistentDataType.BYTE, (byte) 1);
                resultItem.setItemMeta(meta);
                Result updatedResult = new Result(StackReference.of(resultItem));

                event.setResult(updatedResult);
            }
        }
    }

    public boolean isCustomChunkHopper(ItemStack item) {
        if (!isEnabled() || item == null || item.getType() != Material.HOPPER)
            return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (pdc.getKeys().isEmpty())
            return false;

        if (pdc.has(plugin.getChunkHopperManager().getAchKey(), PersistentDataType.BYTE))
            return true;

        return custom_hopper != null && custom_hopper.isSimilar(item);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ItemStack getCustomHopper() {
        return custom_hopper;
    }

    public boolean give(Player player, int amount) {
        if (player == null || custom_hopper == null)
            return false;

        ItemStack finalItem = custom_hopper.clone();

        if (finalItem.getType().isAir())
            return false;

        finalItem.setAmount(amount);

        ItemMeta meta = finalItem.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(plugin.getChunkHopperManager().getAchKey(), PersistentDataType.BYTE, (byte) 1);
            finalItem.setItemMeta(meta);
        }

        player.getInventory().addItem(finalItem).values().forEach(overFlowItem ->
                player.getWorld().dropItemNaturally(player.getLocation(), overFlowItem)
        );

        return true;
    }
}