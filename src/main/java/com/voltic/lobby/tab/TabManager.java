package com.voltic.lobby.tab;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.util.TextUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * High-performance, asynchronously updated Tablist manager using modern Paper API.
 * Employs delta caching to avoid sending redundant network packets when header/footer haven't changed.
 */
public class TabManager {

    private final LobbyPlugin plugin;

    @Getter
    private boolean enabled;
    @Getter
    private long updateIntervalTicks;
    private final List<String> headerLines = new ArrayList<>();
    private final List<String> footerLines = new ArrayList<>();

    private record TabPair(Component header, Component footer) {}
    private final Map<UUID, TabPair> lastSent = new ConcurrentHashMap<>();

    private ScheduledTask asyncTask;

    public TabManager(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
        this.loadConfig();
        this.startTask();
    }

    /**
     * Loads settings and lines from tab.yml.
     */
    public synchronized void loadConfig() {
        FileConfiguration config = plugin.getConfigManager().getTab();

        this.enabled = config.getBoolean("tab.enabled", true);
        this.updateIntervalTicks = config.getLong("tab.update-interval-ticks", 20L);

        this.headerLines.clear();
        this.headerLines.addAll(config.getStringList("tab.header"));

        this.footerLines.clear();
        this.footerLines.addAll(config.getStringList("tab.footer"));

        this.lastSent.clear();
    }

    /**
     * Starts the asynchronous tab update task.
     */
    public synchronized void startTask() {
        stopTask();
        if (!enabled) return;

        long intervalMillis = Math.max(500, updateIntervalTicks * 50);

        this.asyncTask = Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin,
                task -> updateAllPlayers(),
                200,
                intervalMillis,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stops the running update task.
     */
    public synchronized void stopTask() {
        if (asyncTask != null && !asyncTask.isCancelled()) {
            asyncTask.cancel();
            asyncTask = null;
        }
    }

    /**
     * Updates header and footer for all online players.
     */
    public void updateAllPlayers() {
        if (!enabled) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOnline()) {
                updatePlayer(player);
            }
        }
    }

    /**
     * Updates header and footer for a specific player with delta comparison.
     *
     * @param player Target player
     */
    public void updatePlayer(@NotNull Player player) {
        if (!enabled) {
            player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            lastSent.remove(player.getUniqueId());
            return;
        }

        Component header = buildComponent(player, headerLines);
        Component footer = buildComponent(player, footerLines);

        TabPair previous = lastSent.get(player.getUniqueId());
        if (previous != null && Objects.equals(previous.header(), header) && Objects.equals(previous.footer(), footer)) {
            return; // Skip packet transmission entirely
        }

        lastSent.put(player.getUniqueId(), new TabPair(header, footer));
        player.sendPlayerListHeaderAndFooter(header, footer);
    }

    private Component buildComponent(@NotNull Player player, @NotNull List<String> lines) {
        if (lines.isEmpty()) {
            return Component.empty();
        }

        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            String parsedLine = TextUtil.replacePlaceholders(player, lines.get(i));
            result = result.append(TextUtil.parse(parsedLine));
            if (i < lines.size() - 1) {
                result = result.append(Component.newline());
            }
        }
        return result;
    }

    /**
     * Cleans up tracking data when a player disconnects.
     *
     * @param uuid Player UUID
     */
    public void cleanupPlayer(@NotNull UUID uuid) {
        lastSent.remove(uuid);
    }
}
