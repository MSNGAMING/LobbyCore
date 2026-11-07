package com.voltic.lobby.scoreboard;

import com.voltic.lobby.LobbyPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages side scoreboards for all online players with async updates and zero flicker.
 */
public class ScoreboardManager {

    private final LobbyPlugin plugin;

    @Getter
    private boolean enabled;
    @Getter
    private String title;
    @Getter
    private long updateIntervalTicks;
    @Getter
    private final List<String> lines = new ArrayList<>();

    private final Map<UUID, ScoreboardBoard> boards = new ConcurrentHashMap<>();
    private ScheduledTask updateTask;

    public ScoreboardManager(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
        this.loadConfig();
        this.startTask();
    }

    /**
     * Loads settings from scoreboard.yml.
     */
    public void loadConfig() {
        FileConfiguration config = plugin.getConfigManager().getScoreboard();

        this.enabled = config.getBoolean("scoreboard.enabled", true);
        this.title = config.getString("scoreboard.title", "<bold><gradient:#00f2fe:#4facfe>VOLTIC LOBBY</gradient></bold>");
        this.updateIntervalTicks = config.getLong("scoreboard.update-interval-ticks", 20L);

        this.lines.clear();
        this.lines.addAll(config.getStringList("scoreboard.lines"));
    }

    /**
     * Starts the repeating update task.
     */
    public synchronized void startTask() {
        stopTask();
        if (!enabled) return;

        long intervalMillis = Math.max(250, updateIntervalTicks * 50);

        this.updateTask = Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin,
                task -> updateAll(),
                200,
                intervalMillis,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stops the running update task.
     */
    public synchronized void stopTask() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    /**
     * Initializes a scoreboard for a player upon joining.
     *
     * @param player Joining player
     */
    public void registerPlayer(@NotNull Player player) {
        if (!enabled) return;

        player.getScheduler().run(plugin, task -> {
            if (player.isOnline()) {
                ScoreboardBoard board = new ScoreboardBoard(player);
                boards.put(player.getUniqueId(), board);
                board.update(title, lines);
            }
        }, null);
    }

    /**
     * Unregisters and destroys a player's scoreboard upon quitting.
     *
     * @param player Quitting player
     */
    public void unregisterPlayer(@NotNull Player player) {
        ScoreboardBoard board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.destroy();
        }
    }

    /**
     * Updates scoreboards for all registered online players.
     */
    public void updateAll() {
        if (!enabled) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) continue;

            ScoreboardBoard board = boards.get(player.getUniqueId());
            if (board == null) {
                registerPlayer(player);
                continue;
            }

            // Execute on player's regional/entity thread for thread safety
            player.getScheduler().run(plugin, task -> {
                if (player.isOnline()) {
                    board.update(title, lines);
                }
            }, null);
        }
    }

    /**
     * Cleans up all boards when disabling the plugin.
     */
    public void destroyAll() {
        stopTask();
        for (ScoreboardBoard board : boards.values()) {
            board.destroy();
        }
        boards.clear();
    }
}
