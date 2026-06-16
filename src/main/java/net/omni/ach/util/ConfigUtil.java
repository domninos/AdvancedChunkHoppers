package net.omni.ach.util;

import net.omni.ach.AdvancedChunkHoppers;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

public class ConfigUtil {

    private final AdvancedChunkHoppers plugin;
    private final Map<String, Integer> limitsMap = new HashMap<>();

    private int hopper_size = 0;
    private String hopper_title;

    private String whitelist_mat;
    private int whitelist_slot;
    private String whitelist_display_name;

    private String back_button_mat;
    private int back_button_slot;
    private String back_button_display_name;

    private String blacklist_mat;
    private int blacklist_slot;
    private String blacklist_display_name;

    public ConfigUtil(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    public void load() {
        AtomicInteger savedDefaults = new AtomicInteger();

        this.hopper_size = getAndDefaultSlot("size", 57, savedDefaults::getAndAdd);
        if (hopper_size < 18) {
            this.hopper_size = 18;
            plugin.getConfig().set("hopper.size", 18);
            savedDefaults.getAndIncrement();
        }

        this.hopper_title = getAndDefaultString("title", "<green>%player%'s ChunkHopper</green>", savedDefaults::getAndAdd);

        this.whitelist_mat = getAndDefaultString("whitelist.material", "LIGHT_GREEN_STAINED_GLASS_PANE", savedDefaults::getAndAdd).toUpperCase();
        this.whitelist_slot = getAndDefaultSlot("whitelist.slot", 47, savedDefaults::getAndAdd);
        this.whitelist_display_name = getAndDefaultString("whitelist.display_name", "<dark_green>Whitelist</dark_green>", savedDefaults::getAndAdd);

        this.back_button_mat = getAndDefaultString("back_button.material", "BARRIER", savedDefaults::getAndAdd).toUpperCase();
        this.back_button_slot = getAndDefaultSlot("back_button.slot", 49, savedDefaults::getAndAdd);
        this.back_button_display_name = getAndDefaultString("back_button.display_name", "<light_red><b>Exit</b></light_red>", savedDefaults::getAndAdd);

        this.blacklist_mat = getAndDefaultString("blacklist.material", "BARRIER", savedDefaults::getAndAdd).toUpperCase();
        this.blacklist_slot = getAndDefaultSlot("blacklist.slot", 51, savedDefaults::getAndAdd);
        this.blacklist_display_name = getAndDefaultString("blacklist.display_name", "<b><gradient:#F16262:#000000>FREEDOM</gradient><gradient:#000000:#000000> DROPS</gradient></b>", savedDefaults::getAndAdd);

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
        String temp = plugin.getConfig().getString("hopper." + path);

        if (temp == null) {
            plugin.getConfig().set("hopper." + path, defaultVal);
            consumer.accept(1);
            return defaultVal;
        }

        return temp;
    }

    public int getHopperSize() {
        return hopper_size;
    }

    public String getHopperTitle(String playerName) {
        return hopper_title.replace("%player%", playerName);
    }

    public String getWhitelistMat() {
        return whitelist_mat;
    }

    public int getWhitelistSlot() {
        return whitelist_slot;
    }

    public String getWhitelistDisplayName() {
        return whitelist_display_name;
    }

    public String getBackButtonMat() {
        return back_button_mat;
    }

    public int getBackButtonSlot() {
        return back_button_slot;
    }

    public String getBackButtonDisplayName() {
        return back_button_display_name;
    }

    public String getBlacklistMat() {
        return blacklist_mat;
    }

    public int getBlacklistSlot() {
        return blacklist_slot;
    }

    public String getBlacklistDisplayName() {
        return blacklist_display_name;
    }

    public Map<String, Integer> getLimitsMap() {
        return limitsMap;
    }

    public void flush() {
        limitsMap.clear();
    }
}
