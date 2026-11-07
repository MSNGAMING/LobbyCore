package com.voltic.lobby.util;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ultra-high performance text utility leveraging Adventure MiniMessage with seamless legacy & / § conversion.
 */
@UtilityClass
public class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6})");
    private static final Pattern LEGACY_SECTION_HEX_PATTERN = Pattern.compile("§x§([a-fA-F0-9])§([a-fA-F0-9])§([a-fA-F0-9])§([a-fA-F0-9])§([a-fA-F0-9])§([a-fA-F0-9])");

    private static final Map<Character, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry('0', "<black>"),
            Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"),
            Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"),
            Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"),
            Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"),
            Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"),
            Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"),
            Map.entry('f', "<white>"),
            Map.entry('k', "<obfuscated>"),
            Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"),
            Map.entry('n', "<underlined>"),
            Map.entry('o', "<italic>"),
            Map.entry('r', "<reset>")
    );

    private static volatile Boolean papiPresent = null;

    /**
     * Parse any string (MiniMessage format or Legacy & / § codes) into an Adventure Component.
     *
     * @param input Raw text
     * @return Formatted Component
     */
    @NotNull
    public static Component parse(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        // Fast path: if no legacy markers or hex prefixes are present, directly parse MiniMessage
        if (!input.contains("&") && !input.contains("§")) {
            return MINI_MESSAGE.deserialize(input);
        }

        return MINI_MESSAGE.deserialize(convertLegacyToMiniMessage(input));
    }

    /**
     * Converts legacy ampersand (&) and section (§) codes and hex formats into MiniMessage tags
     * without escaping existing MiniMessage tags.
     */
    @NotNull
    public static String convertLegacyToMiniMessage(@NotNull String input) {
        // Convert &#RRGGBB to <#RRGGBB>
        if (input.contains("&#")) {
            Matcher matcher = HEX_COLOR_PATTERN.matcher(input);
            input = matcher.replaceAll("<#$1>");
        }

        // Convert §x§R§R§G§G§B§B to <#RRGGBB>
        if (input.contains("§x")) {
            Matcher matcher = LEGACY_SECTION_HEX_PATTERN.matcher(input);
            input = matcher.replaceAll("<#$1$2$3$4$5$6>");
        }

        StringBuilder sb = new StringBuilder(input.length() + 16);
        int length = input.length();

        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < length) {
                char code = Character.toLowerCase(input.charAt(i + 1));
                String tag = LEGACY_TAGS.get(code);
                if (tag != null) {
                    sb.append(tag);
                    i++; // Skip the color code character
                    continue;
                }
            }
            sb.append(c);
        }

        return sb.toString();
    }

    /**
     * Parses a list of strings into a list of Components.
     *
     * @param lines Raw lines
     * @return List of formatted Components
     */
    @NotNull
    public static List<Component> parseLines(@Nullable List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        return lines.stream().map(TextUtil::parse).toList();
    }

    /**
     * Replaces standard placeholders for a player and parses to Component.
     *
     * @param player Target player
     * @param text   Raw text with placeholders
     * @return Formatted Component
     */
    @NotNull
    public static Component parsePlayer(@NotNull Player player, @Nullable String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return parse(replacePlaceholders(player, text));
    }

    /**
     * Checks if PlaceholderAPI is enabled on the server with cached lookup.
     */
    public static boolean isPapiPresent() {
        if (papiPresent == null) {
            papiPresent = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        }
        return papiPresent;
    }

    /**
     * Refresh the cached PlaceholderAPI state.
     */
    public static void updatePapiPresent() {
        papiPresent = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    /**
     * Replace common server and player placeholders in a string, including PlaceholderAPI if present.
     *
     * @param player Target player
     * @param text   Source string
     * @return String with replaced placeholders
     */
    @NotNull
    public static String replacePlaceholders(@NotNull Player player, @NotNull String text) {
        // Fast-path: If string doesn't contain '%', no placeholder replacement needed
        if (!text.contains("%")) {
            return text;
        }

        String result = text
                .replace("%player%", player.getName())
                .replace("%displayname%", PLAIN_SERIALIZER.serialize(player.displayName()))
                .replace("%ping%", String.valueOf(player.getPing()))
                .replace("%online%", String.valueOf(player.getServer().getOnlinePlayers().size()))
                .replace("%max_players%", String.valueOf(player.getServer().getMaxPlayers()))
                .replace("%world%", player.getWorld().getName())
                .replace("%health%", String.valueOf((int) player.getHealth()))
                .replace("%food%", String.valueOf(player.getFoodLevel()));

        if (isPapiPresent() && result.contains("%")) {
            try {
                result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result);
            } catch (Throwable ignored) {
            }
        }

        return result;
    }

    /**
     * Strip all color codes and tags from a string.
     *
     * @param input Raw text
     * @return Plain text
     */
    @NotNull
    public static String stripColor(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Component comp = parse(input);
        return PLAIN_SERIALIZER.serialize(comp);
    }
}
