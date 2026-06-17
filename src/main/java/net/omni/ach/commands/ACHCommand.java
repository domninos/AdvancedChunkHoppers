package net.omni.ach.commands;

import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.util.MessageUtil;
import net.omni.ach.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.util.StringUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ACHCommand implements CommandExecutor {

    private final AdvancedChunkHoppers plugin;

    public ACHCommand(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender.hasPermission("ach.use"))) {
            plugin.sendMessage(sender, Messages.NO_PERMS.toString());
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("help")) {
                sendHelp(sender);
                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (!(sender.hasPermission("ach.reload"))) {
                    plugin.sendMessage(sender, Messages.NO_PERMS.toString());
                    return true;
                }

                boolean restartNeeded = plugin.getConfigUtil().reloadConfig();
                plugin.getMessagesManager().loadMessages();
                plugin.getChunkHopperManager().reloadPullerTask();

                if (restartNeeded)
                    plugin.sendMessage(sender, "<green>config.yml and messages.yml have been reloaded.</green>\n<gray>Note: Inventory size/title changes require a server restart for existing hoppers.</gray>");
                else
                    plugin.sendMessage(sender, "<green>config.yml and messages.yml have been reloaded.</green>");
            } else if (args[0].equalsIgnoreCase("about"))
                sender.sendMessage(MessageUtil.parse(getAboutText()));
            else
                plugin.sendMessage(sender, Messages.UNKNOWN_COMMAND.toString());

            return true;
        } else
            plugin.sendMessage(sender, Messages.UNKNOWN_COMMAND.toString());

        return true;
    }

    private void sendHelp(CommandSender sender) {
        StringBuilder helpBuilder = new StringBuilder();

        // header
        helpBuilder.append("\n<dark_gray>▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪</dark_gray>\n");
        helpBuilder.append("  <gradient:#00AAFF:#55FFFF><bold>AdvancedChunkHoppers</bold></gradient> <gray>\n");

        if (sender.hasPermission("ach.use")) {
            helpBuilder.append(MessageUtil.formatString("ach", "Base command for AdvancedChunkHoppers.", "ach"));
            helpBuilder.append(MessageUtil.formatString("ach <#55FFFF>help</#55FFFF>", "Shows this help menu."));

            if (sender.hasPermission("ach.reload"))
                helpBuilder.append(MessageUtil.formatString("ach <#55FFFF>reload</#55FFFF>", "Reloads config and messages."));

            helpBuilder.append(MessageUtil.formatString("ach <#55FFFF>about</#55FFFF>", "Shows basic information about this plugin."));
        }

        // footer
        helpBuilder.append("<dark_gray>▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪</dark_gray>");

        sender.sendMessage(MessageUtil.parse(helpBuilder.toString()));
    }

    private @NonNull String getAboutText() {
        String pluginName = plugin.getDescription().getName();
        String version = plugin.getDescription().getVersion();
        String author = plugin.getDescription().getAuthors().getFirst();
        String githubUrl = "https://github.com/domninos/AdvancedChunkHoppers";
        String discordUrl = "https://discord.gg/7CuCtDHmQ3";

        return "<dark_gray>▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪</dark_gray>\n" +
                "  <gradient:#00AAFF:#55FFFF><bold>" + pluginName + "</bold></gradient>\n\n" +
                "  <yellow>Version:</yellow> <white>" + version + "</white>\n" +
                "  <yellow>Author:</yellow> <aqua>" + author + "</aqua>\n\n" +
                "  <white>Links: </white>" +
                "<click:open_url:'" + githubUrl + "'><hover:show_text:'<gray>Click to view open-source code'><dark_purple>[GitHub]</dark_purple></hover></click> " +
                "<click:open_url:'" + discordUrl + "'><hover:show_text:'<gray>Click to join support community'><blue>[Discord]</blue></hover></click>\n" +
                "<dark_gray>▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪</dark_gray>";

    }

    public void register() {
        PluginCommand command = plugin.getCommand("advancedchunkhopper");

        if (command == null) {
            plugin.sendConsole("<red>/advancedchunkhopper is not a command.</red>");
            return;
        }

        command.setTabCompleter((sender, command1, label, args) -> {
            if (args.length == 1) {
                List<String> subcommands = new ArrayList<>();

                subcommands.add("help");
                subcommands.add("about");

                if (sender.hasPermission("ach.reload"))
                    subcommands.add("reload");

                List<String> completions = new ArrayList<>();
                StringUtil.copyPartialMatches(args[0], subcommands, completions);

                return completions;
            }

            return Collections.emptyList();
        });

        command.setExecutor(this);
    }
}
