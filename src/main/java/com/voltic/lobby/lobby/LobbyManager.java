package com.voltic.lobby.lobby;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.util.LocationUtil;
import com.voltic.lobby.util.TextUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages lobby registration, selection, balancing, and teleportation.
 */
public class LobbyManager {

    private final LobbyPlugin plugin;

    @Getter
    private final Map<String, LobbyData> lobbies = new ConcurrentHashMap<>();

    @Getter
    private String defaultLobbyId = "lobby-1";

    @Getter
    private boolean autoBalancing = true;

    public LobbyManager(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
        this.loadLobbies();
    }

    /**
     * Loads all lobbies from lobbies.yml.
     */
    public void loadLobbies() {
        lobbies.clear();
        FileConfiguration config = plugin.getConfigManager().getLobbies();

        this.defaultLobbyId = config.getString("settings.default-lobby", "lobby-1");
        this.autoBalancing = config.getBoolean("settings.auto-balance", true);

        ConfigurationSection section = config.getConfigurationSection("lobbies");
        if (section == null) {
            plugin.getLogger().info("No lobbies defined in lobbies.yml, initializing default fallback.");
            ensureDefaultLobby();
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection lobbySec = section.getConfigurationSection(id);
            if (lobbySec == null) continue;

            String displayName = lobbySec.getString("display-name", "<green>" + id + "</green>");
            Location loc = LocationUtil.readFromSection(lobbySec.getConfigurationSection("spawn"));
            int maxPlayers = lobbySec.getInt("max-players", 100);
            String permission = lobbySec.getString("permission", null);
            boolean enabled = lobbySec.getBoolean("enabled", true);

            if (loc == null) {
                plugin.getLogger().warning("Invalid spawn location for lobby: " + id);
                continue;
            }

            LobbyData data = LobbyData.builder()
                    .id(id)
                    .displayName(displayName)
                    .spawnLocation(loc)
                    .maxPlayers(maxPlayers)
                    .permission(permission)
                    .enabled(enabled)
                    .build();

            lobbies.put(id.toLowerCase(Locale.ROOT), data);
        }

        if (lobbies.isEmpty()) {
            ensureDefaultLobby();
        }
    }

    private void ensureDefaultLobby() {
        World defaultWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst();
        if (defaultWorld == null) return;

        Location spawnLoc = defaultWorld.getSpawnLocation().clone().add(0.5, 0.5, 0.5);
        LobbyData defaultData = LobbyData.builder()
                .id("lobby-1")
                .displayName("<gradient:#00f2fe:#4facfe>Main Lobby</gradient>")
                .spawnLocation(spawnLoc)
                .maxPlayers(150)
                .permission(null)
                .enabled(true)
                .build();

        lobbies.put("lobby-1", defaultData);
        saveLobby(defaultData);
    }

    /**
     * Finds the best lobby for the player (least populated if auto-balancing).
     *
     * @param player Target player
     * @return Selected LobbyData or null
     */
    @Nullable
    public LobbyData getBestLobby(@NotNull Player player) {
        if (lobbies.isEmpty()) {
            return null;
        }

        if (!autoBalancing) {
            LobbyData def = lobbies.get(defaultLobbyId.toLowerCase(Locale.ROOT));
            if (def != null && def.canAccess(player::hasPermission)) {
                return def;
            }
        }

        LobbyData best = null;
        int minCount = Integer.MAX_VALUE;

        for (LobbyData data : lobbies.values()) {
            if (!data.canAccess(player::hasPermission)) continue;

            int count = getPlayerCountInLobby(data);
            if (count < data.getMaxPlayers() && count < minCount) {
                minCount = count;
                best = data;
            }
        }

        // Fallback to default if all full
        if (best == null) {
            best = lobbies.get(defaultLobbyId.toLowerCase(Locale.ROOT));
        }

        // Fallback to any valid lobby
        if (best == null && !lobbies.isEmpty()) {
            best = lobbies.values().iterator().next();
        }

        return best;
    }

    /**
     * Counts how many players are in the world corresponding to a lobby.
     *
     * @param data LobbyData
     * @return Current player count
     */
    public int getPlayerCountInLobby(@NotNull LobbyData data) {
        World world = data.getSpawnLocation().getWorld();
        if (world == null) return 0;
        return world.getPlayerCount();
    }

    /**
     * Teleports a player to a lobby asynchronously and thread-safely.
     *
     * @param player  Player to teleport
     * @param lobbyId Identifier of the lobby
     */
    public void teleportToLobby(@NotNull Player player, @NotNull String lobbyId) {
        LobbyData lobby = lobbies.get(lobbyId.toLowerCase(Locale.ROOT));
        if (lobby == null || !lobby.isEnabled()) {
            player.sendMessage(TextUtil.parse("<red>That lobby does not exist or is disabled!</red>"));
            return;
        }

        if (!lobby.canAccess(player::hasPermission)) {
            player.sendMessage(TextUtil.parse("<red>You do not have permission to access " + lobby.getDisplayName() + "<red>!</red>"));
            return;
        }

        Location dest = lobby.getSpawnLocation();
        if (dest.getWorld() == null) {
            player.sendMessage(TextUtil.parse("<red>The world for this lobby is currently unloaded!</red>"));
            return;
        }

        player.teleportAsync(dest).thenAccept(success -> {
            player.getScheduler().run(plugin, task -> {
                if (success) {
                    player.sendMessage(TextUtil.parse("<green>Teleported to " + lobby.getDisplayName() + "<green>!</green>"));
                    player.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
                } else {
                    player.sendMessage(TextUtil.parse("<red>Failed to safely teleport to lobby!</red>"));
                }
            }, null);
        });
    }

    /**
     * Registers or updates a lobby and saves it to lobbies.yml.
     *
     * @param id          Lobby ID
     * @param location    Spawn location
     * @param displayName Custom display name
     * @param maxPlayers  Max player capacity
     */
    public void setLobby(@NotNull String id, @NotNull Location location, @NotNull String displayName, int maxPlayers) {
        String key = id.toLowerCase(Locale.ROOT);
        LobbyData lobby = LobbyData.builder()
                .id(key)
                .displayName(displayName)
                .spawnLocation(location)
                .maxPlayers(maxPlayers)
                .permission(null)
                .enabled(true)
                .build();

        lobbies.put(key, lobby);
        saveLobby(lobby);
    }

    /**
     * Saves a specific lobby into the lobbies.yml configuration.
     *
     * @param lobby Lobby to save
     */
    public void saveLobby(@NotNull LobbyData lobby) {
        FileConfiguration config = plugin.getConfigManager().getLobbies();
        String path = "lobbies." + lobby.getId();

        config.set(path + ".display-name", lobby.getDisplayName());
        config.set(path + ".max-players", lobby.getMaxPlayers());
        config.set(path + ".permission", lobby.getPermission());
        config.set(path + ".enabled", lobby.isEnabled());

        ConfigurationSection spawnSec = config.createSection(path + ".spawn");
        LocationUtil.writeToSection(spawnSec, lobby.getSpawnLocation());

        plugin.getConfigManager().getLobbiesConfig().saveAsync();
    }

    /**
     * Removes a lobby by ID.
     *
     * @param id Lobby ID
     * @return true if found and removed
     */
    public boolean deleteLobby(@NotNull String id) {
        String key = id.toLowerCase(Locale.ROOT);
        if (lobbies.remove(key) != null) {
            FileConfiguration config = plugin.getConfigManager().getLobbies();
            config.set("lobbies." + key, null);
            plugin.getConfigManager().getLobbiesConfig().saveAsync();
            return true;
        }
        return false;
    }

    @Nullable
    public LobbyData getLobby(@NotNull String id) {
        return lobbies.get(id.toLowerCase(Locale.ROOT));
    }
}
