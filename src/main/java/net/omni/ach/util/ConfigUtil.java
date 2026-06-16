package net.omni.ach.util;

import net.omni.ach.AdvancedChunkHoppers;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

public class ConfigUtil {

    private final AdvancedChunkHoppers plugin;
    private final Map<String, Integer> limitsMap = new HashMap<>();

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

    public ConfigUtil(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    public void load() {
        AtomicInteger savedDefaults = new AtomicInteger();

        this.hopper_size = getAndDefaultSlot("hopper.size", 54, savedDefaults::getAndAdd);
        if (hopper_size < 18) {
            this.hopper_size = 18;
            plugin.getConfig().set("hopper.size", 18);
            savedDefaults.getAndIncrement();
        }

        this.hopper_title = getAndDefaultString("hopper.title", "<green>%player%'s ChunkHopper</green>", savedDefaults::getAndAdd);

        this.filler_mat = getAndDefaultString("hopper.filler.mat", "GRAY_STAINED_GLASS_PANE", savedDefaults::getAndAdd);
        this.filler_display_name = getAndDefaultString("hopper.filler.display_name", " ", savedDefaults::getAndAdd);

        this.whitelist_mat = getAndDefaultString("hopper.whitelist.material", "LIGHT_GREEN_STAINED_GLASS_PANE", savedDefaults::getAndAdd).toUpperCase();
        this.whitelist_slot = getAndDefaultSlot("hopper.whitelist.slot", 47, savedDefaults::getAndAdd);
        this.whitelist_display_name = getAndDefaultString("hopper.whitelist.display_name", "<dark_green>Whitelist</dark_green>", savedDefaults::getAndAdd);
        this.whitelist_lore = plugin.getConfig().getStringList("hopper.whitelist.lore");

        this.back_button_mat = getAndDefaultString("hopper.back_button.material", "BARRIER", savedDefaults::getAndAdd).toUpperCase();
        this.back_button_slot = getAndDefaultSlot("hopper.back_button.slot", 49, savedDefaults::getAndAdd);
        this.back_button_display_name = getAndDefaultString("hopper.back_button.display_name", "<light_red><b>Exit</b></light_red>", savedDefaults::getAndAdd);
        this.back_button_lore = plugin.getConfig().getStringList("hopper.back_button.lore");

        this.blacklist_mat = getAndDefaultString("hopper.blacklist.material", "BARRIER", savedDefaults::getAndAdd).toUpperCase();
        this.blacklist_slot = getAndDefaultSlot("hopper.blacklist.slot", 51, savedDefaults::getAndAdd);
        this.blacklist_display_name = getAndDefaultString("hopper.blacklist.display_name", "<b><gradient:#F16262:#000000>FREEDOM</gradient><gradient:#000000:#000000> DROPS</gradient></b>", savedDefaults::getAndAdd);
        this.blacklist_lore = plugin.getConfig().getStringList("hopper.blacklist.lore");

        ConfigurationSection limitsSection = plugin.getConfig().getConfigurationSection("limits");

        if (limitsSection != null) {
            for (String group : limitsSection.getKeys(false)) {
                if (group == null || group.isEmpty())
                    continue;

                if (!limitsSection.isInt(group)) {
                    plugin.sendConsole("<yellow>Limits for '" + group + "' is not an integer. Skipping..</yellow>");
                    continue;
                }

                int limit = limitsSection.getInt(group);

                // the group settings
                limitsMap.put(group.toLowerCase(), limit);
            }
        }

        if (savedDefaults.get() > 0) {
            plugin.saveConfig();

            plugin.sendConsole("<green>Successfully loaded " + savedDefaults.get() + " default configuration(s)</green>");
        }

        plugin.sendConsole("<green>Successfully loaded config.yml</green>");
    }

    private int getAndDefaultSlot(String path, int defaultVal, IntConsumer consumer) {
        int temp = plugin.getConfig().getInt("hopper." + path);

        if (!plugin.getConfig().contains("hopper." + path) || temp == 0) {
            plugin.getConfig().set("hopper." + path, defaultVal);
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

    public List<String> getBackButtonLoreSize() {
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

    public void flush() {
        limitsMap.clear();
    }
}
