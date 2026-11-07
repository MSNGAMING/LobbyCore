package com.voltic.lobby.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Manages modular configuration files for the Lobby plugin.
 */
@Getter
public class ConfigManager {

    private final Plugin plugin;

    private final ConfigFile mainConfig;
    private final ConfigFile hologramsConfig;
    private final ConfigFile npcsConfig;
    private final ConfigFile chatConfig;
    private final ConfigFile tabConfig;
    private final ConfigFile scoreboardConfig;
    private final ConfigFile motdConfig;
    private final ConfigFile lobbiesConfig;

    public ConfigManager(@NotNull Plugin plugin) {
        this.plugin = plugin;

        this.mainConfig = new ConfigFile(plugin, "config.yml");
        this.hologramsConfig = new ConfigFile(plugin, "holograms.yml");
        this.npcsConfig = new ConfigFile(plugin, "npcs.yml");
        this.chatConfig = new ConfigFile(plugin, "chat.yml");
        this.tabConfig = new ConfigFile(plugin, "tab.yml");
        this.scoreboardConfig = new ConfigFile(plugin, "scoreboard.yml");
        this.motdConfig = new ConfigFile(plugin, "motd.yml");
        this.lobbiesConfig = new ConfigFile(plugin, "lobbies.yml");
    }

    /**
     * Reloads all configuration files from disk.
     */
    public void reloadAll() {
        mainConfig.reload();
        hologramsConfig.reload();
        npcsConfig.reload();
        chatConfig.reload();
        tabConfig.reload();
        scoreboardConfig.reload();
        motdConfig.reload();
        lobbiesConfig.reload();
    }

    /**
     * Asynchronously saves all configurations.
     */
    @NotNull
    public CompletableFuture<Void> saveAllAsync() {
        return CompletableFuture.allOf(
                mainConfig.saveAsync(),
                hologramsConfig.saveAsync(),
                npcsConfig.saveAsync(),
                chatConfig.saveAsync(),
                tabConfig.saveAsync(),
                scoreboardConfig.saveAsync(),
                motdConfig.saveAsync(),
                lobbiesConfig.saveAsync()
        );
    }

    // Direct access to FileConfigurations
    @NotNull
    public FileConfiguration getMain() {
        return mainConfig.getConfig();
    }

    @NotNull
    public FileConfiguration getHolograms() {
        return hologramsConfig.getConfig();
    }

    @NotNull
    public FileConfiguration getNpcs() {
        return npcsConfig.getConfig();
    }

    @NotNull
    public FileConfiguration getChat() {
        return chatConfig.getConfig();
    }

    @NotNull
    public FileConfiguration getTab() {
        return tabConfig.getConfig();
    }

    @NotNull
    public FileConfiguration getScoreboard() {
        return scoreboardConfig.getConfig();
    }

    @NotNull
    public FileConfiguration getMotd() {
        return motdConfig.getConfig();
    }

    @NotNull
    public FileConfiguration getLobbies() {
        return lobbiesConfig.getConfig();
    }
}
