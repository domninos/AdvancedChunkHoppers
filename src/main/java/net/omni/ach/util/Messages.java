package net.omni.ach.util;

import java.util.ArrayList;
import java.util.List;

public enum Messages {
    NO_PERMS("no_perms", "<red>You do not have permission to use this command.</red>"),
    ONLY_PLAYERS("only_players", "<red>Only players can use this command.</red>"),

    USAGE("usage", "<red>Invalid arguments. Usage: %usage%</red>"),
    UNKNOWN_COMMAND("unknown_cmd", "<red>Unknown command.</red>"),

    RELOADED("reloaded", "<green>config.yml and messages.yml have been reloaded.</green>"),

    HOPPER_FACE_DOWN("hopper_face_down", "<red>Chunk Hopper must be placed facing down.</red>"),
    ONE_PER_CHUNK("one_per_chunk", "<red>You can only place 1 <gold><b>Chunk Hopper</b></gold> in 1 chunk.</red>"),
    MAX_HOPPERS_REACHED("max_hoppers_reached", "<red>You have reached the maximum number of Chunk Hoppers.</red>"),
    HOPPERS_LEFT("hoppers_left", "<white>You have %remaining%(x) chunk hoppers left.</white>"),
    HOPPERS_NOW("hoppers_now", "<white>You now have %count%(x) chunk hoppers left.</white>"),

    NO_BREAK_PERMS("no_break_perms", "<red>You do not have permission to break this.</red>"),
    NO_OPEN_PERMS("no_open_perms", "<red>You do not have permission to open this hopper.</red>"),

    HOPPER_FULL("hopper_full", "<red>Your ChunkHopper at <yellow>%x%, %y%, %z%</yellow> is full!</red>"),
    FILTER_FULL("filter_full", "<red>Filter is full!</red>"),
    FILTER_CONFLICT("filter_conflict", "<red>That item is already in the other filter.</red>");

    private final String path;
    private final Object defaultVal;
    private Object cachedVal;

    Messages(String path, Object defaultVal) {
        this.path = path;
        this.defaultVal = defaultVal;
    }

    public String getPath() {
        return path;
    }

    public Object getDefaultVal() {
        return defaultVal;
    }

    public void setCachedVal(Object val) {
        this.cachedVal = val;
    }

    public String replace(String... pairs) {
        String result = this.toString();

        return replace(result, pairs);
    }

    @Override
    public String toString() {
        if (cachedVal instanceof List<?>)
            return "";

        return cachedVal instanceof String ? (String) cachedVal : (String) defaultVal;
    }

    private String replace(String result, String... pairs) {
        if (result.isEmpty())
            return "";

        for (int i = 0; i < pairs.length - 1; i += 2) {
            String key = pairs[i];
            String val = pairs[i + 1];

            if (key != null && val != null) {
                result = result.replace("%" + key + "%", val);
            }
        }

        return result;
    }

    public String replaceList(String... pairs) {
        List<String> originalList = this.asList();

        if (originalList.isEmpty())
            return "";

        List<String> modifiedList = new ArrayList<>();

        for (String line : originalList) {
            if (line != null)
                modifiedList.add(replace(line, pairs));
        }

        return String.join("\n", modifiedList);
    }

    @SuppressWarnings("unchecked")
    public List<String> asList() {
        return cachedVal instanceof List<?> ? (List<String>) cachedVal : (List<String>) defaultVal;
    }

    public void flush() {
        if (cachedVal instanceof List<?> cachedList)
            cachedList.clear();

        if (defaultVal instanceof List<?> defaultList)
            defaultList.clear();

        this.cachedVal = null;
    }
}
