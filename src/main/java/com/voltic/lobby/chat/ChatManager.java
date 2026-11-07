package com.voltic.lobby.chat;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.util.TextUtil;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance chat manager with MiniMessage formatting, single-pass anti-swear, and anti-spam.
 */
public class ChatManager {

    private final LobbyPlugin plugin;

    @Getter
    private String formatTemplate;
    @Getter
    private boolean antiSwearEnabled;
    @Getter
    private String antiSwearAction; // "BLOCK" or "MASK"
    @Getter
    private final List<String> blockedWords = new ArrayList<>();
    private volatile Pattern combinedBlockedPattern;

    @Getter
    private boolean antiSpamEnabled;
    @Getter
    private long spamCooldownMillis;
    @Getter
    private boolean blockDuplicates;

    private final Map<UUID, Long> lastMessageTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastMessageContents = new ConcurrentHashMap<>();

    public ChatManager(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
        this.loadConfig();
    }

    /**
     * Loads settings from chat.yml.
     */
    public synchronized void loadConfig() {
        FileConfiguration config = plugin.getConfigManager().getChat();

        this.formatTemplate = config.getString("chat.format", "<gray>[</gray><gradient:#00f2fe:#4facfe>Lobby</gradient><gray>]</gray> <white>%player%</white><dark_gray> »</dark_gray> <gray>%message%</gray>");

        this.antiSwearEnabled = config.getBoolean("anti-swear.enabled", true);
        this.antiSwearAction = config.getString("anti-swear.action", "MASK").toUpperCase(Locale.ROOT);
        this.blockedWords.clear();

        List<String> wordsFromConfig = config.getStringList("anti-swear.blocked-words");
        if (!wordsFromConfig.isEmpty()) {
            StringBuilder patternBuilder = new StringBuilder("(?i)\\b(?:");
            for (int i = 0; i < wordsFromConfig.size(); i++) {
                String word = wordsFromConfig.get(i);
                this.blockedWords.add(word.toLowerCase(Locale.ROOT));
                if (i > 0) patternBuilder.append("|");
                patternBuilder.append(Pattern.quote(word));
            }
            patternBuilder.append(")\\b");
            this.combinedBlockedPattern = Pattern.compile(patternBuilder.toString());
        } else {
            this.combinedBlockedPattern = null;
        }

        this.antiSpamEnabled = config.getBoolean("anti-spam.enabled", true);
        this.spamCooldownMillis = (long) (config.getDouble("anti-spam.cooldown-seconds", 1.5) * 1000);
        this.blockDuplicates = config.getBoolean("anti-spam.block-duplicates", true);
    }

    /**
     * Checks whether a player message is considered spam.
     *
     * @param player  Sending player
     * @param message Message text
     * @return true if blocked by anti-spam
     */
    public boolean checkSpam(@NotNull Player player, @NotNull String message) {
        if (!antiSpamEnabled || player.hasPermission("lobby.chat.bypass")) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastTime = lastMessageTimestamps.get(uuid);

        if (lastTime != null && (now - lastTime) < spamCooldownMillis) {
            double waitSec = (spamCooldownMillis - (now - lastTime)) / 1000.0;
            player.sendMessage(TextUtil.parse(String.format(Locale.ROOT, "<red>Please wait %.1fs before sending another message!</red>", waitSec)));
            return true;
        }

        if (blockDuplicates) {
            String lastText = lastMessageContents.get(uuid);
            if (lastText != null && lastText.equalsIgnoreCase(message.trim())) {
                player.sendMessage(TextUtil.parse("<red>Please do not repeat the exact same message!</red>"));
                return true;
            }
        }

        lastMessageTimestamps.put(uuid, now);
        lastMessageContents.put(uuid, message.trim());
        return false;
    }

    /**
     * Filters swearing from a message using a single-pass regex evaluation.
     *
     * @param player  Sending player
     * @param message Original message text
     * @return Filtered message or null if the message should be blocked
     */
    public String filterSwearWords(@NotNull Player player, @NotNull String message) {
        if (!antiSwearEnabled || combinedBlockedPattern == null || player.hasPermission("lobby.chat.bypass")) {
            return message;
        }

        Matcher matcher = combinedBlockedPattern.matcher(message);
        if (matcher.find()) {
            if ("BLOCK".equals(antiSwearAction)) {
                player.sendMessage(TextUtil.parse("<red>Your message was blocked because it contains inappropriate language!</red>"));
                return null;
            }
            return matcher.replaceAll("***");
        }

        return message;
    }

    /**
     * Cleans up tracking data when a player disconnects.
     *
     * @param uuid Player UUID
     */
    public void cleanupPlayer(@NotNull UUID uuid) {
        lastMessageTimestamps.remove(uuid);
        lastMessageContents.remove(uuid);
    }
}
