package net.omni.ach.util;

import net.omni.ach.AdvancedChunkHoppers;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

public class ConfigUtil {

    private final AdvancedChunkHoppers plugin;
    private final Map<String, Integer> limitsMap = new HashMap<>();
    private final Map<String, Integer> hoppersPlacedLimitsMap = new HashMap<>();

    private int hopper_size = 0;
    private String hopper_title;

    private String filler_mat;
    private String filler_display_name;

    private String whitelist_mat;
    private int whitelist_slot;
    private String whitelist_display_name;
    private List<String> whitelist_lore;

    private String back_button_mat;
    private int back_button_slot;
    private String back_button_display_name;
    private List<String> back_button_lore;

    private String blacklist_mat;
    private int blacklist_slot;
    private String blacklist_display_name;
    private List<String> blacklist_lore;

    private int whitelist_inventory_size;
    private String whitelist_inventory_title;
    private int whitelist_inventory_back_slot;

    private int blacklist_inventory_size;
    private String blacklist_inventory_title;
    private int blacklist_inventory_back_slot;

    private String whitelist_add_sound;
    private float whitelist_add_sound_volume;
    private float whitelist_add_sound_pitch;

    private String blacklist_add_sound;
    private float blacklist_add_sound_volume;
    private float blacklist_add_sound_pitch;

    private int puller_interval_ticks;

    private List<Material> container_materials;

    public ConfigUtil(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    public boolean reloadConfig() {
        int oldHopperSize = hopper_size;
        String oldHopperTitle = hopper_title;
        int oldWhitelistSize = whitelist_inventory_size;
        String oldWhitelistTitle = whitelist_inventory_title;
        int oldBlacklistSize = blacklist_inventory_size;
        String oldBlacklistTitle = blacklist_inventory_title;

        plugin.reloadConfig();
        load();

        return hopper_size != oldHopperSize
                || !hopper_title.equals(oldHopperTitle)
                || whitelist_inventory_size != oldWhitelistSize
                || !whitelist_inventory_title.equals(oldWhitelistTitle)
                || blacklist_inventory_size != oldBlacklistSize
                || !blacklist_inventory_title.equals(oldBlacklistTitle);
    }

    public void load() {
        AtomicInteger savedDefaults = new AtomicInteger();

        this.hopper_size = getAndDefaultSlot("hopper.size", 54, savedDefaults::getAndAdd);
        if (hopper_size < 18) {
            this.hopper_size = 18;
            plugin.getConfig().set("hopper.size", 18);
            savedDefaults.getAndIncrement();
        }

        this.hopper_title = getAndDefaultString("hopper.title", "<green>ChunkHopper</green>", savedDefaults::getAndAdd);

        this.filler_mat = getAndDefaultString("hopper.filler.material", "GRAY_STAINED_GLASS_PANE", savedDefaults::getAndAdd);
        this.filler_display_name = getAndDefaultString("hopper.filler.display_name", " ", savedDefaults::getAndAdd);

        this.whitelist_mat = getAndDefaultString("hopper.whitelist.material", "LIME_STAINED_GLASS_PANE", savedDefaults::getAndAdd).toUpperCase();
        this.whitelist_slot = getAndDefaultSlot("hopper.whitelist.slot", 47, savedDefaults::getAndAdd);
        this.whitelist_display_name = getAndDefaultString("hopper.whitelist.display_name", "<dark_green>Whitelist</dark_green>", savedDefaults::getAndAdd);
        this.whitelist_lore = plugin.getConfig().getStringList("hopper.whitelist.lore");

        this.back_button_mat = getAndDefaultString("hopper.back_button.material", "BARRIER", savedDefaults::getAndAdd).toUpperCase();
        this.back_button_slot = getAndDefaultSlot("hopper.back_button.slot", 49, savedDefaults::getAndAdd);
        this.back_button_display_name = getAndDefaultString("hopper.back_button.display_name", "<#FF6B6B><b>Exit</b></#FF6B6B>", savedDefaults::getAndAdd);
        this.back_button_lore = plugin.getConfig().getStringList("hopper.back_button.lore");

        this.blacklist_mat = getAndDefaultString("hopper.blacklist.material", "BARRIER", savedDefaults::getAndAdd).toUpperCase();
        this.blacklist_slot = getAndDefaultSlot("hopper.blacklist.slot", 51, savedDefaults::getAndAdd);
        this.blacklist_display_name = getAndDefaultString("hopper.blacklist.display_name", "<b><gradient:#F16262:#000000>FREEDOM</gradient><gradient:#000000:#000000> DROPS</gradient></b>", savedDefaults::getAndAdd);
        this.blacklist_lore = plugin.getConfig().getStringList("hopper.blacklist.lore");

        this.whitelist_inventory_size = getAndDefaultSlot("whitelist_inventory.size", 27, savedDefaults::getAndAdd);
        if (whitelist_inventory_size < 18) {
            this.whitelist_inventory_size = 18;
            plugin.getConfig().set("whitelist_inventory.size", 18);
            savedDefaults.getAndIncrement();
        }
        this.whitelist_inventory_title = getAndDefaultString("whitelist_inventory.title",
                "<green>ChunkHopper's Whitelist</green>", savedDefaults::getAndAdd);
        this.whitelist_inventory_back_slot = getAndDefaultSlot("whitelist_inventory.back_button_slot",
                22, savedDefaults::getAndAdd);

        this.blacklist_inventory_size = getAndDefaultSlot("blacklist_inventory.size", 27, savedDefaults::getAndAdd);
        if (blacklist_inventory_size < 18) {
            this.blacklist_inventory_size = 18;
            plugin.getConfig().set("blacklist_inventory.size", 18);
            savedDefaults.getAndIncrement();
        }
        this.blacklist_inventory_title = getAndDefaultString("blacklist_inventory.title",
                "<green>ChunkHopper's Blacklist</green>", savedDefaults::getAndAdd);
        this.blacklist_inventory_back_slot = getAndDefaultSlot("blacklist_inventory.back_button_slot",
                22, savedDefaults::getAndAdd);

        this.whitelist_add_sound = getAndDefaultString("whitelist_inventory.add_sound",
                "ENTITY_ITEM_PICKUP", savedDefaults::getAndAdd);
        this.whitelist_add_sound_volume = (float) plugin.getConfig().getDouble("whitelist_inventory.add_sound_volume", 0.5);
        this.whitelist_add_sound_pitch = (float) plugin.getConfig().getDouble("whitelist_inventory.add_sound_pitch", 1.0);

        this.blacklist_add_sound = getAndDefaultString("blacklist_inventory.add_sound",
                "ENTITY_ITEM_PICKUP", savedDefaults::getAndAdd);
        this.blacklist_add_sound_volume = (float) plugin.getConfig().getDouble("blacklist_inventory.add_sound_volume", 0.5);
        this.blacklist_add_sound_pitch = (float) plugin.getConfig().getDouble("blacklist_inventory.add_sound_pitch", 1.0);

        this.puller_interval_ticks = plugin.getConfig().getInt("puller.interval_ticks", 10);
        if (puller_interval_ticks < 1) {
            this.puller_interval_ticks = 1;
            plugin.getConfig().set("puller.interval_ticks", 1);
        }

        List<String> matStrings = plugin.getConfig().getStringList("container_materials");
        if (matStrings.isEmpty()) {
            matStrings = List.of(
                    "CHEST", "TRAPPED_CHEST", "BARREL", "HOPPER", "SHULKER_BOX",
                    "WHITE_SHULKER_BOX", "ORANGE_SHULKER_BOX", "MAGENTA_SHULKER_BOX",
                    "LIGHT_BLUE_SHULKER_BOX", "YELLOW_SHULKER_BOX", "LIME_SHULKER_BOX",
                    "PINK_SHULKER_BOX", "GRAY_SHULKER_BOX", "LIGHT_GRAY_SHULKER_BOX",
                    "CYAN_SHULKER_BOX", "PURPLE_SHULKER_BOX", "BLUE_SHULKER_BOX",
                    "BROWN_SHULKER_BOX", "GREEN_SHULKER_BOX", "RED_SHULKER_BOX", "BLACK_SHULKER_BOX"
            );

            plugin.getConfig().set("container_materials", matStrings);
            savedDefaults.getAndIncrement();
        }

        this.container_materials = new ArrayList<>();
        for (String s : matStrings) {
            Material m = Material.matchMaterial(s.toUpperCase());

            if (m != null)
                this.container_materials.add(m);
            else
                plugin.sendConsole("<red>Invalid container material in config: " + s + "</red>");
        }

        ConfigurationSection limitsSection = plugin.getConfig().getConfigurationSection("limits");

        if (limitsSection != null) {
            for (String group : limitsSection.getKeys(false)) {
                if (group == null || group.isEmpty())
                    continue;

                if (!limitsSection.isInt(group)) {
                    plugin.sendConsole("<yellow>Limit for '" + group + "' is not an integer. Skipping..</yellow>");
                    continue;
                }

                String lower = group.toLowerCase();
                if (limitsMap.containsKey(lower))
                    plugin.sendConsole("<yellow>Duplicate limit entry for group '" + group + "' in config.yml. Using value " + limitsSection.getInt(group) + ".</yellow>");

                limitsMap.put(lower, limitsSection.getInt(group));
            }
        }

        if (limitsMap.isEmpty()) {
            limitsMap.put("knight", 2);
            limitsMap.put("lord", 4);
            plugin.sendConsole("<yellow>No limits found in config.yml. Using defaults: knight=2, lord=4</yellow>");
        }

        ConfigurationSection hoppersSection = plugin.getConfig().getConfigurationSection("hoppers_placed_limits");

        if (hoppersSection != null) {
            for (String group : hoppersSection.getKeys(false)) {
                if (group == null || group.isEmpty())
                    continue;

                if (!hoppersSection.isInt(group)) {
                    plugin.sendConsole("<yellow>Hoppers placed limit for '" + group + "' is not an integer. Skipping..</yellow>");
                    continue;
                }

                String lower = group.toLowerCase();
                if (hoppersPlacedLimitsMap.containsKey(lower))
                    plugin.sendConsole("<yellow>Duplicate hoppers_placed_limits entry for group '" + group + "' in config.yml. Using value " + hoppersSection.getInt(group) + ".</yellow>");

                hoppersPlacedLimitsMap.put(lower, hoppersSection.getInt(group));
            }
        }

        if (hoppersPlacedLimitsMap.isEmpty()) {
            hoppersPlacedLimitsMap.put("knight", 5);
            hoppersPlacedLimitsMap.put("lord", 10);
            plugin.sendConsole("<yellow>No hoppers_placed_limits found in config.yml. Using defaults: knight=5, lord=10</yellow>");
        }

        if (savedDefaults.get() > 0) {
            plugin.saveConfig();

            plugin.sendConsole("<green>Successfully loaded " + savedDefaults.get() + " default configuration(s)</green>");
        }

        plugin.sendConsole("<green>Successfully loaded config.yml</green>");
    }

    private int getAndDefaultSlot(String path, int defaultVal, IntConsumer consumer) {
        int temp = plugin.getConfig().getInt(path);

        if (!plugin.getConfig().contains(path) || temp == 0) {
            plugin.getConfig().set(path, defaultVal);
            consumer.accept(1);
            return defaultVal;
        }

        return temp;
    }

    private String getAndDefaultString(String path, String defaultVal, IntConsumer consumer) {
        String temp = plugin.getConfig().getString(path);

        if (temp == null) {
            plugin.getConfig().set(path, defaultVal);
            consumer.accept(1);
            return defaultVal;
        }

        return temp;
    }

    public int getHopperSize() {
        return hopper_size;
    }

    public String getHopperTitle() {
        return hopper_title;
    }

    public Material getWhitelistMat() {
        return Material.matchMaterial(this.whitelist_mat);
    }

    public Material getFillerMat() {
        return Material.matchMaterial(this.filler_mat);
    }

    public String getFillerDisplayName() {
        return filler_display_name;
    }

    public int getWhitelistSlot() {
        return whitelist_slot;
    }

    public String getWhitelistDisplayName() {
        return whitelist_display_name;
    }

    public List<String> getWhitelistLore() {
        return whitelist_lore;
    }

    public Material getBackButtonMat() {
        return Material.matchMaterial(this.back_button_mat);
    }

    public int getBackButtonSlot() {
        return back_button_slot;
    }

    public String getBackButtonDisplayName() {
        return back_button_display_name;
    }

    public List<String> getBackButtonLore() {
        return back_button_lore;
    }

    public Material getBlacklistMat() {
        return Material.matchMaterial(this.blacklist_mat);
    }

    public int getBlacklistSlot() {
        return blacklist_slot;
    }

    public String getBlacklistDisplayName() {
        return blacklist_display_name;
    }

    public List<String> getBlacklistLore() {
        return blacklist_lore;
    }

    public Map<String, Integer> getLimitsMap() {
        return limitsMap;
    }

    public Map<String, Integer> getHoppersPlacedLimitsMap() {
        return hoppersPlacedLimitsMap;
    }

    public int getWhitelistInventorySize() {
        return whitelist_inventory_size;
    }

    public String getWhitelistInventoryTitle() {
        return whitelist_inventory_title;
    }

    public int getWhitelistInventoryBackSlot() {
        return whitelist_inventory_back_slot;
    }

    public int getBlacklistInventorySize() {
        return blacklist_inventory_size;
    }

    public String getBlacklistInventoryTitle() {
        return blacklist_inventory_title;
    }

    public int getBlacklistInventoryBackSlot() {
        return blacklist_inventory_back_slot;
    }

    public Sound getWhitelistAddSound() {
        try {
            return Sound.valueOf(whitelist_add_sound);
        } catch (IllegalArgumentException e) {
            return Sound.ENTITY_ITEM_PICKUP;
        }
    }

    public float getWhitelistAddSoundVolume() {
        return whitelist_add_sound_volume;
    }

    public float getWhitelistAddSoundPitch() {
        return whitelist_add_sound_pitch;
    }

    public Sound getBlacklistAddSound() {
        try {
            return Sound.valueOf(blacklist_add_sound);
        } catch (IllegalArgumentException e) {
            return Sound.ENTITY_ITEM_PICKUP;
        }
    }

    public float getBlacklistAddSoundVolume() {
        return blacklist_add_sound_volume;
    }

    public float getBlacklistAddSoundPitch() {
        return blacklist_add_sound_pitch;
    }

    public int getPullerIntervalTicks() {
        return puller_interval_ticks;
    }

    public List<Material> getContainerMaterials() {
        return container_materials;
    }

    public void flush() {
        limitsMap.clear();
        hoppersPlacedLimitsMap.clear();
        container_materials.clear();
    }
}
