package net.omni.ach.commands;

import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.util.MessageUtil;
import net.omni.ach.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
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
                    plugin.sendMessage(sender, Messages.RELOADED
                            + "\n<gray>Note: Inventory size/title changes require a server restart for existing hoppers.</gray>");
                else
                    plugin.sendMessage(sender, Messages.RELOADED.toString());
            } else if (args[0].equalsIgnoreCase("about"))
                sender.sendMessage(MessageUtil.parse(getAboutText()));
            else if (args[0].equalsIgnoreCase("give"))
                plugin.sendMessage(sender, Messages.USAGE.replace("usage", "/ach give (player) [amount]"));
            else
                plugin.sendMessage(sender, Messages.UNKNOWN_COMMAND.toString());

            return true;
        } else if (args.length == 2 || args.length == 3) {
            if (!args[0].equalsIgnoreCase("give")) {
                plugin.sendMessage(sender, Messages.UNKNOWN_COMMAND.toString());
                return true;
            }

            if (!(sender.hasPermission("ach.give"))) {
                plugin.sendMessage(sender, Messages.NO_PERMS.toString());
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);

            if (target == null || !target.isOnline()) {
                plugin.sendMessage(sender, Messages.PLAYER_NOT_FOUND.replace("player", args[1]));
                return true;
            }

            int amount = 1;

            if (args.length == 3) {
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    plugin.sendMessage(sender, "<red>'" + args[2] + "' is not a number. </red>");
                    return true;
                }
            }

            boolean success = plugin.getCustomCraftingHook().give(target, "customcrafting:utilities/chunk_hopper", amount);

            if (success)
                plugin.sendMessage(sender, Messages.GIVE_SUCCESS.replace("player", target.getName()));
            else
                plugin.sendMessage(sender, Messages.GIVE_ERROR.replace("player", target.getName()));

        } else
            plugin.sendMessage(sender, Messages.UNKNOWN_COMMAND.toString());

        return true;
    }

    private void sendHelp(CommandSender sender) {
        StringBuilder helpBuilder = new StringBuilder();

        helpBuilder.append("\n<dark_gray>▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪</dark_gray>\n");
        helpBuilder.append("  <gradient:#00AAFF:#55FFFF><bold>AdvancedChunkHoppers</bold></gradient> <gray>\n\n");

        if (sender.hasPermission("ach.use")) {
            helpBuilder.append(MessageUtil.formatString("ach", "Base command for AdvancedChunkHoppers.", "ach"));
            helpBuilder.append(MessageUtil.formatString("ach <#55FFFF>help</#55FFFF>", "Shows this help menu."));

            if (sender.hasPermission("ach.reload"))
                helpBuilder.append(MessageUtil.formatString("ach <#55FFFF>reload</#55FFFF>", "Reloads config and messages."));

            helpBuilder.append(MessageUtil.formatString("ach <#55FFFF>about</#55FFFF>", "Shows basic information about this plugin."));

            if (sender.hasPermission("ach.give"))
                helpBuilder.append(MessageUtil.formatString("ach give <#55FFFF>(player) [amount]</#55FFFF>", "Gives chunk hopper to player."));
        }

        helpBuilder.append("<dark_gray>▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪▪</dark_gray>");

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
            List<String> subcommands = new ArrayList<>();
            List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                subcommands.add("help");
                subcommands.add("about");

                if (sender.hasPermission("ach.reload"))
                    subcommands.add("reload");

                if (sender.hasPermission("ach.give"))
                    subcommands.add("give");

                StringUtil.copyPartialMatches(args[0], subcommands, completions);

                return completions;
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("give") && sender.hasPermission("ach.give"))
                    return null;
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("give") && sender.hasPermission("ach.give"))
                    completions.add("[amount]");

                return completions;
            }

            return Collections.emptyList();
        });

        command.setExecutor(this);
    }
}
