package com.voltic.lobby.config;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * Manages an individual YAML configuration file with thread-safe async saving.
 */
public class ConfigFile {

    private final Plugin plugin;
    @Getter
    private final String fileName;
    private final File file;
    private final ReentrantLock fileLock = new ReentrantLock();

    @Getter
    private volatile FileConfiguration config;

    public ConfigFile(@NotNull Plugin plugin, @NotNull String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.file = new File(plugin.getDataFolder(), fileName);
        this.createIfNotExists();
        this.reload();
    }

    /**
     * Create the file from embedded resource if it does not already exist.
     */
    private void createIfNotExists() {
        if (!plugin.getDataFolder().exists()) {
            boolean created = plugin.getDataFolder().mkdirs();
            if (!created && !plugin.getDataFolder().exists()) {
                plugin.getLogger().warning("Could not create plugin data folder: " + plugin.getDataFolder().getPath());
            }
        }

        if (!file.exists()) {
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            } else {
                try {
                    boolean created = file.createNewFile();
                    if (!created) {
                        plugin.getLogger().warning("Failed to create new empty config file: " + fileName);
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to create " + fileName, e);
                }
            }
        }
    }

    /**
     * Reload the configuration from disk.
     */
    public void reload() {
        fileLock.lock();
        try {
            this.config = YamlConfiguration.loadConfiguration(file);
        } finally {
            fileLock.unlock();
        }
    }

    /**
     * Save configuration changes synchronously.
     */
    public void saveSync() {
        fileLock.lock();
        try {
            this.config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + fileName, e);
        } finally {
            fileLock.unlock();
        }
    }

    /**
     * Save configuration changes asynchronously to avoid main thread latency and prevent CME.
     *
     * @return CompletableFuture completing when the save finishes
     */
    @NotNull
    public CompletableFuture<Void> saveAsync() {
        String yamlContent;
        fileLock.lock();
        try {
            yamlContent = this.config.saveToString();
        } finally {
            fileLock.unlock();
        }

        return CompletableFuture.runAsync(() -> {
            fileLock.lock();
            try {
                java.nio.file.Files.writeString(
                        file.toPath(),
                        yamlContent,
                        java.nio.charset.StandardCharsets.UTF_8
                );
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Asynchronous save failed for " + fileName, e);
            } finally {
                fileLock.unlock();
            }
        }, runnable -> Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run()));
    }

    /**
     * Sets a value in the configuration with thread-safe locking.
     *
     * @param path  Config path
     * @param value Value to set
     */
    public void set(@NotNull String path, Object value) {
        fileLock.lock();
        try {
            config.set(path, value);
        } finally {
            fileLock.unlock();
        }
    }
}
