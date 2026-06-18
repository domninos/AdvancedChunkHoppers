package net.omni.ach;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.omni.ach.commands.ACHCommand;
import net.omni.ach.db.DatabaseManager;
import net.omni.ach.hooks.CustomCraftingHook;
import net.omni.ach.hooks.GangsPlusHook;
import net.omni.ach.hooks.LuckPermsHook;
import net.omni.ach.hooks.RoseStackerHook;
import net.omni.ach.listeners.BottomContainerListener;
import net.omni.ach.listeners.ChunkHopperListener;
import net.omni.ach.listeners.GUIListener;
import net.omni.ach.managers.CacheManager;
import net.omni.ach.managers.ChunkHopperManager;
import net.omni.ach.managers.GUIManager;
import net.omni.ach.managers.MessagesManager;
import net.omni.ach.util.ACHConfig;
import net.omni.ach.util.ConfigUtil;
import net.omni.ach.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdvancedChunkHoppers extends JavaPlugin {

    private RoseStackerHook roseStackerHook;
    private GangsPlusHook gangsPlusHook;
    private CustomCraftingHook customCraftingHook;
    private LuckPermsHook luckPermsHook;

    private ChunkHopperManager chunkHopperManager;

    private CacheManager cacheManager;
    private GUIManager guiManager;

    private DatabaseManager databaseManager;

    private MessagesManager messagesManager;
    private ACHConfig messagesConfig;

    private ConfigUtil configUtil;

    @Override
    public void onDisable() {
        chunkHopperManager.flush();
        cacheManager.invalidateAll();

        configUtil.flush();
        messagesManager.flush();

        luckPermsHook.unregister();

        // close pool after all saves are done
        databaseManager.closePool();

        sendConsole("<red>Successfully disabled.</red>");
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messagesConfig = new ACHConfig(this, "messages.yml");
        this.messagesManager = new MessagesManager(this);
        messagesManager.loadMessages();

        this.configUtil = new ConfigUtil(this);
        configUtil.load();

        this.databaseManager = new DatabaseManager(this);
        databaseManager.initDatabase();

        this.cacheManager = new CacheManager(this);
        this.guiManager = new GUIManager(this);

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

    private void registerHooks() {
        this.roseStackerHook = new RoseStackerHook();
        this.gangsPlusHook = new GangsPlusHook();
        this.customCraftingHook = new CustomCraftingHook();

        if (Bukkit.getPluginManager().isPluginEnabled("GangsPlus")) {
            this.gangsPlusHook.init();
            sendConsole("<green>Successfully hooked into Gangs+!</green>");
        }

        if (Bukkit.getPluginManager().isPluginEnabled("RoseStacker")) {
            this.roseStackerHook.init();
            sendConsole("<green>Successfully hooked into RoseStacker!</green>");
        }

        if (Bukkit.getPluginManager().isPluginEnabled("CustomCrafting")) {
            this.customCraftingHook.init();
            sendConsole("<green>Successfully hooked into CustomCrafting!</green>");
        }

        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            this.luckPermsHook = new LuckPermsHook(this);
            sendConsole("<green>Successfully hooked into LuckPerms!</green>");
        }
    }

    private void registerCommands() {
        new ACHCommand(this).register();
    }

    private void registerListeners() {
        new ChunkHopperListener(this).register();
        new BottomContainerListener(this).register();
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

    public ChunkHopperManager getChunkHopperManager() {
        return chunkHopperManager;
    }

    public ConfigUtil getConfigUtil() {
        return configUtil;
    }

    public RoseStackerHook getRoseStackerHook() {
        return roseStackerHook;
    }

    public GangsPlusHook getGangsPlusHook() {
        return gangsPlusHook;
    }

    public CustomCraftingHook getCustomCraftingHook() {
        return customCraftingHook;
    }

    public ACHConfig getMessagesConfig() {
        return messagesConfig;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
}
