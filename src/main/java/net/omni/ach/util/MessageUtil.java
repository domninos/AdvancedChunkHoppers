package net.omni.ach.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

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
     * Parses a string whether using the legacy '&' color codes or the new minimessage format
     *
     * @param msg String to deserialize
     * @return the deserialized colored text
     */
    public static Component parse(String msg) {
        if (msg.contains("<") && msg.contains(">"))
            return MINI_MESSAGE.deserialize(msg);
        else
            return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }
}
