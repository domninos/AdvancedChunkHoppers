package net.omni.ach.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Arrays;

public class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Component PREFIX =
            MiniMessage.miniMessage().deserialize(
                    "<gray>[</gray><gradient:#00AAFF:#55FFFF>AdvancedChunkHoppers</gradient><gray>]</gray> "
            );

    private MessageUtil() {
    }

    /**
     * Color your text.
     * Note: Uses prefix
     *
     * @param msg String to deserialize
     * @return the deserialized colored text
     */
    public static Component color(String msg) {
        return PREFIX.append(MINI_MESSAGE.deserialize(msg));
    }

    /**
     * Color your text.
     * Note: Uses prefix
     *
     * @param msg       String to deserialize
     * @param resolvers parsed keys to values
     * @return the deserialized colored text
     */
    public static Component color(String msg, TagResolver... resolvers) {
        return PREFIX.append(MINI_MESSAGE.deserialize(msg, resolvers));
    }

    /**
     * Parses a string whether using the legacy '&' color codes or the new minimessage format.
     * Note: Doesn't use prefix.
     *
     * @param msg String to deserialize
     * @return the deserialized colored text
     */
    public static Component parse(String msg, TagResolver... resolvers) {
        if (msg.contains("<") && msg.contains(">"))
            return resolvers != null && resolvers.length > 0 ? MINI_MESSAGE.deserialize(msg, resolvers) : MINI_MESSAGE.deserialize(msg);
        else
            return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }

    /**
     * For OMC commands.
     * Appends commands its description to a {@link StringBuilder}.
     *
     * @param command     the command label and arguments (without `/`)
     * @param description description of the command
     * @param builder     the {@link StringBuilder} to append to
     */
    public static void append(String command, String description, StringBuilder builder) {
        builder.append(formatString(command, description));
    }

    /**
     * For OMC commands.
     * Creates the base unparsed {@link String} that contains command labels, description, and aliases.
     *
     * @param command     the command label and arguments (without `/`)
     * @param description description of the command
     * @param aliases     command aliases (if any)
     * @return the original unparsed {@link String}
     */
    public static String formatString(String command, String description, String... aliases) {
        StringBuilder builder = new StringBuilder();

        builder.append("  <#00AAFF>/")
                .append(command)
                .append("</#00AAFF> <dark_gray>-</dark_gray> <gray> ")
                .append(description)
                .append("</gray>")
                .append("\n");

        if (aliases != null && aliases.length > 0)
            builder.append("  <white> <italic>⤷ Aliases: ")
                    .append(Arrays.toString(aliases))
                    .append("</italic></white>");

        return builder.toString();
    }
}
