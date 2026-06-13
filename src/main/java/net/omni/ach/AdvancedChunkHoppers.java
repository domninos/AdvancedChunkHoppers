package net.omni.ach;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.omni.ach.db.DatabaseManager;
import net.omni.ach.gui.GUIManager;
import net.omni.ach.managers.CacheManager;
import net.omni.ach.managers.ChunkHopperManager;
import net.omni.ach.managers.FilterManager;
import net.omni.ach.managers.StorageManager;
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
        - config
        - add holograms support (picked up <item> x<amount>)
        -
        - support PDC for player ownership
        - use SQLITE for inventories
        -
        - implement auto save from pdc to database
        - check for block changes / block destruction
     */

    private FilterManager filterManager;
    private ChunkHopperManager chunkHopperManager;

    private CacheManager cacheManager;
    private StorageManager storageManager;
    private GUIManager guiManager;

    private DatabaseManager databaseManager;

    @Override
    public void onDisable() {

        filterManager.flush();
        chunkHopperManager.flush();

        cacheManager.invalidateAll();

        databaseManager.closePool();

        sendConsole("<red>Successfully disabled.</red>");
    }

    @Override
    public void onEnable() {

        this.databaseManager = new DatabaseManager(this);
        databaseManager.initDatabase();

        this.storageManager = new StorageManager(this);
        this.cacheManager = new CacheManager(this);
        this.guiManager = new GUIManager(this);

        this.filterManager = new FilterManager();
        filterManager.init();

        this.chunkHopperManager = new ChunkHopperManager(this);
        chunkHopperManager.init();

        // check if RoseStacker is enabled, if not, default to ChunkHopperListener

        // getDescription() is deprecated on paper, should keep for spigot support
        sendConsole("<green>Successfully started <name>-v<version> </green>",
                Placeholder.parsed("name", getDescription().getName()),
                Placeholder.parsed("version", getDescription().getVersion())
        );
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

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public FilterManager getFilterManager() {
        return filterManager;
    }

    public ChunkHopperManager getChunkHopperManager() {
        return chunkHopperManager;
    }


    private void registerListeners() {

    }

    private void registerCommands() {

    }
}
