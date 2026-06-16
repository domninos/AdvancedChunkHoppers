package net.omni.ach;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.omni.ach.db.DatabaseManager;
import net.omni.ach.listeners.ChunkHopperListener;
import net.omni.ach.listeners.GUIListener;
import net.omni.ach.managers.CacheManager;
import net.omni.ach.managers.ChunkHopperManager;
import net.omni.ach.managers.FilterManager;
import net.omni.ach.managers.GUIManager;
import net.omni.ach.util.ConfigUtil;
import net.omni.ach.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdvancedChunkHoppers extends JavaPlugin {

    /*
    TODO:
        - support RoseStackers
        - use ItemPreStackEvent
        - use ItemSpawnEvent fallback
        -
        - support PDC for player ownership
        - use SQLITE for inventories
        -
        - implement auto save to database
        - check for block changes / block destruction
        -
        - When the Chunk Hopper Gets full it should pull all the drops to above it and let them stack there and then once its empty should resume normal function
        -
        - When you break the hopper if it has items in it they should just pop out just like if you broke a chest.
        -
        - For whitelist and blacklist players should drag and drop an item into the inventory it makes an audible que and the item appears in the menu.
        -
        - The chunk hopper should act as some sort of over flow chest for when it gets full
             and if it is full should message the player when a drop is unable to be picked up that way players know if that is the case.
     */

    private boolean gangsEnabled, roseStackerEnabled = false;

    private FilterManager filterManager;
    private ChunkHopperManager chunkHopperManager;

    private CacheManager cacheManager;
    private GUIManager guiManager;

    private DatabaseManager databaseManager;

    private ConfigUtil configUtil;

    @Override
    public void onDisable() {

        // before flushing everything, save and close the pool
        databaseManager.closePool();

        filterManager.flush();
        chunkHopperManager.flush();

        cacheManager.invalidateAll();

        configUtil.flush();

        sendConsole("<red>Successfully disabled.</red>");
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configUtil = new ConfigUtil(this);
        configUtil.load();

        this.databaseManager = new DatabaseManager(this);
        databaseManager.initDatabase();

        this.cacheManager = new CacheManager(this);
        this.guiManager = new GUIManager(this);

        this.filterManager = new FilterManager();
        filterManager.init();

        this.chunkHopperManager = new ChunkHopperManager(this);
        chunkHopperManager.init();

        registerHooks();

        registerCommands();

        registerListeners();

        // getDescription() is deprecated on paper, should keep for spigot support
        sendConsole("<green>Successfully started <name>-v<version> </green>",
                Placeholder.parsed("name", getDescription().getName()),
                Placeholder.parsed("version", getDescription().getVersion())
        );
    }

    // TODO
    private void registerHooks() {
        // check if GangsPlus is enabled
        if (Bukkit.getPluginManager().isPluginEnabled("GangsPlus")) {
            gangsEnabled = true;
            sendConsole("<green>GangsPlus is available!</green>");
        }

        // check if RoseStacker is enabled, if not, default to ChunkHopperListener
        if (Bukkit.getPluginManager().isPluginEnabled("RoseStacker")) {
            roseStackerEnabled = true;
            sendConsole("<green>RoseStacker is available!</green>");
        }
    }

    private void registerCommands() {

    }

    private void registerListeners() {
        new ChunkHopperListener(this).register();
        new GUIListener(this).register();
    }

    public void sendConsole(String message, TagResolver... resolvers) {
        sendMessage(Bukkit.getConsoleSender(), message, resolvers);
    }

    public void sendMessage(CommandSender sender, String message, TagResolver... resolvers) {
        sender.sendMessage(MessageUtil.color(message, resolvers));
    }

    public void sendConsole(String message) {
        sendMessage(Bukkit.getConsoleSender(), message);
    }

    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(MessageUtil.color(message));
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public FilterManager getFilterManager() {
        return filterManager;
    }

    public ChunkHopperManager getChunkHopperManager() {
        return chunkHopperManager;
    }

    public ConfigUtil getConfigUtil() {
        return configUtil;
    }

    public boolean isGangsEnabled() {
        return gangsEnabled;
    }

    public boolean isRoseStackerEnabled() {
        return roseStackerEnabled;
    }
}
