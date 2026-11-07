package com.voltic.lobby.listener;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.lobby.LobbyData;
import com.voltic.lobby.util.TextUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Handles player join, quit, and respawn lifecycle events.
 */
public class PlayerConnectionListener implements Listener {

    private final LobbyPlugin plugin;

    public PlayerConnectionListener(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FileConfiguration mainConfig = plugin.getConfigManager().getMain();

        // Custom join message
        if (mainConfig.getBoolean("join.custom-message-enabled", false)) {
            String msg = mainConfig.getString("join.message", "<gray>[<green>+</green>]</gray> <yellow>%player%</yellow>");
            event.joinMessage(TextUtil.parsePlayer(player, msg));
        }

        // Teleport to lobby on join
        if (mainConfig.getBoolean("join.teleport-to-lobby", true)) {
            LobbyData bestLobby = plugin.getLobbyManager().getBestLobby(player);
            if (bestLobby != null) {
                player.teleportAsync(bestLobby.getSpawnLocation());
            }
        }

        // Set default lobby gamemode & attributes
        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) return;

            if (mainConfig.getBoolean("join.force-adventure-mode", true)) {
                player.setGameMode(GameMode.ADVENTURE);
            }
            if (mainConfig.getBoolean("join.heal-on-join", true)) {
                player.setHealth(20.0);
                player.setFoodLevel(20);
                player.setSaturation(20.0f);
                player.setFireTicks(0);
            }

            // Welcome sound
            if (mainConfig.getBoolean("join.play-sound", true)) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            }

            // Welcome chat lines
            List<String> welcomeMotd = mainConfig.getStringList("join.welcome-text");
            for (String line : welcomeMotd) {
                player.sendMessage(TextUtil.parsePlayer(player, line));
            }

            // Give lobby hotbar items
            plugin.getHotbarManager().giveHotbarItems(player);
        }, null);

        // Hide joined player from players who have visibility turned off
        plugin.getHotbarManager().handlePlayerJoinVisibility(player);

        // Initialize Scoreboard & Tab
        plugin.getScoreboardManager().registerPlayer(player);
        plugin.getTabManager().updatePlayer(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FileConfiguration mainConfig = plugin.getConfigManager().getMain();

        // Custom quit message
        if (mainConfig.getBoolean("quit.custom-message-enabled", false)) {
            String msg = mainConfig.getString("quit.message", "<gray>[<red>-</red>]</gray> <yellow>%player%</yellow>");
            event.quitMessage(TextUtil.parsePlayer(player, msg));
        }

        plugin.getScoreboardManager().unregisterPlayer(player);
        plugin.getTabManager().cleanupPlayer(player.getUniqueId());
        plugin.getChatManager().cleanupPlayer(player.getUniqueId());
        plugin.getHotbarManager().cleanupPlayer(player.getUniqueId());
        if (plugin.getWorldProtectionListener() != null) {
            plugin.getWorldProtectionListener().cleanupPlayer(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        LobbyData bestLobby = plugin.getLobbyManager().getBestLobby(player);
        if (bestLobby != null) {
            event.setRespawnLocation(bestLobby.getSpawnLocation());
        }
    }
}
