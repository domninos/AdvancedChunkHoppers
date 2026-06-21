package net.omni.ach;

import net.omni.ach.commands.ACHCommand;
import net.omni.ach.managers.DatabaseManager;
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
import net.omni.ach.util.*;
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

    private ChatRenderer chatRenderer;

    @Override
    public void onDisable() {
        chunkHopperManager.flush();
        cacheManager.invalidateAll();

        configUtil.flush();
        messagesManager.flush();

        if (luckPermsHook != null)
            luckPermsHook.unregister();

        databaseManager.closePool();

        sendConsole("<red>Successfully disabled.</red>");
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initChatRenderer();

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

        sendConsole("<green>Successfully started " + getDescription().getName() + "-v" + getDescription().getVersion() + " </green>");
    }

    private void initChatRenderer() {
        try {
            Class.forName("net.kyori.adventure.text.Component");
            this.chatRenderer = new PaperChatRenderer();
            sendConsole("<green>PaperMC detected. Using PaperChatRenderer.</green>");
        } catch (ClassNotFoundException e) {
            this.chatRenderer = new SpigotChatRenderer();
            sendConsole("<gray>Spigot detected. Using SpigotChatRenderer.</gray>");
        }

        MessageUtil.init(chatRenderer);
    }

    private void registerHooks() {
        this.roseStackerHook = new RoseStackerHook();
        this.gangsPlusHook = new GangsPlusHook();
        this.customCraftingHook = new CustomCraftingHook(this);
        this.luckPermsHook = new LuckPermsHook(this);

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
            this.luckPermsHook.init();
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

    public void sendConsole(String message) {
        chatRenderer.sendMessage(Bukkit.getConsoleSender(), chatRenderer.color(message));
    }

    public void sendMessage(CommandSender sender, String message) {
        chatRenderer.sendMessage(sender, chatRenderer.color(message));
    }

    public ChatRenderer getChatRenderer() {
        return chatRenderer;
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
