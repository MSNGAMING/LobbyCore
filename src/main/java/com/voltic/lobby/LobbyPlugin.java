package com.voltic.lobby;

import com.voltic.lobby.chat.ChatListener;
import com.voltic.lobby.chat.ChatManager;
import com.voltic.lobby.command.*;
import com.voltic.lobby.config.ConfigManager;
import com.voltic.lobby.gui.GuiListener;
import com.voltic.lobby.gui.anvil.AnvilListener;
import com.voltic.lobby.hologram.HologramManager;
import com.voltic.lobby.hotbar.HotbarListener;
import com.voltic.lobby.hotbar.HotbarManager;
import com.voltic.lobby.listener.PlayerConnectionListener;
import com.voltic.lobby.listener.ServerListPingListener;
import com.voltic.lobby.listener.WorldProtectionListener;
import com.voltic.lobby.lobby.LobbyManager;
import com.voltic.lobby.npc.NpcInteractListener;
import com.voltic.lobby.npc.NpcManager;
import com.voltic.lobby.scoreboard.ScoreboardManager;
import com.voltic.lobby.tab.TabManager;
import com.voltic.lobby.util.TextUtil;
import lombok.Getter;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.logging.Level;

/**
 * Main entry point for the modular Lobby plugin.
 * Built for Paper/Purpur 1.21.4 through 1.26.x.
 */
public class LobbyPlugin extends JavaPlugin {

    private static volatile LobbyPlugin instance;

    @Getter
    private ConfigManager configManager;
    @Getter
    private LobbyManager lobbyManager;
    @Getter
    private HologramManager hologramManager;
    @Getter
    private NpcManager npcManager;
    @Getter
    private ChatManager chatManager;
    @Getter
    private TabManager tabManager;
    @Getter
    private ScoreboardManager scoreboardManager;
    @Getter
    private HotbarManager hotbarManager;
    @Getter
    private WorldProtectionListener worldProtectionListener;
    @Getter
    private ServerListPingListener serverListPingListener;

    /**
     * Returns the thread-safe singleton instance of LobbyPlugin.
     *
     * @return Active plugin instance
     * @throws IllegalStateException If called when the plugin is uninitialized
     */
    @NotNull
    public static LobbyPlugin getInstance() {
        LobbyPlugin inst = instance;
        if (inst == null) {
            throw new IllegalStateException("LobbyPlugin has not been initialized or has already been disabled!");
        }
        return inst;
    }

    @Override
    public void onEnable() {
        instance = this;

        long startTime = System.currentTimeMillis();
        getLogger().info("Starting LobbyCore initialization...");

        try {
            // Initialize modular configs
            this.configManager = new ConfigManager(this);

            // Initialize managers
            this.lobbyManager = new LobbyManager(this);
            this.hologramManager = new HologramManager(this);
            this.npcManager = new NpcManager(this);
            this.chatManager = new ChatManager(this);
            this.tabManager = new TabManager(this);
            this.scoreboardManager = new ScoreboardManager(this);
            this.hotbarManager = new HotbarManager(this);

            // Register events
            registerListeners();

            // Register commands
            registerCommands();

            long elapsed = System.currentTimeMillis() - startTime;
            getLogger().info("LobbyCore enabled successfully in " + elapsed + "ms!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to start LobbyCore properly!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down LobbyCore...");

        // Stop scheduled tasks and cleanup cosmetic displays
        if (tabManager != null) {
            tabManager.stopTask();
        }

        if (scoreboardManager != null) {
            scoreboardManager.destroyAll();
        }

        if (hologramManager != null) {
            hologramManager.removeAllEntities();
        }

        if (npcManager != null) {
            npcManager.removeAllEntities();
        }

        // Asynchronously flush any remaining config saves
        if (configManager != null) {
            try {
                configManager.saveAllAsync().get();
            } catch (Exception ignored) {
            }
        }

        instance = null;
        getLogger().info("LobbyCore disabled successfully.");
    }

    /**
     * Hot-reloads all configurations and updates running managers.
     */
    public void reloadPlugin() {
        configManager.reloadAll();
        TextUtil.updatePapiPresent();

        lobbyManager.loadLobbies();
        if (worldProtectionListener != null) {
            worldProtectionListener.reloadConfig();
        }
        if (serverListPingListener != null) {
            serverListPingListener.reloadConfig();
        }
        chatManager.loadConfig();

        tabManager.loadConfig();
        tabManager.startTask();
        tabManager.updateAllPlayers();

        scoreboardManager.loadConfig();
        scoreboardManager.startTask();
        scoreboardManager.updateAll();

        hologramManager.loadHolograms();
        npcManager.loadNpcs();

        getLogger().info("LobbyCore configuration files and features hot-reloaded.");
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        this.worldProtectionListener = new WorldProtectionListener(this);
        this.serverListPingListener = new ServerListPingListener(this);

        pm.registerEvents(new PlayerConnectionListener(this), this);
        pm.registerEvents(this.worldProtectionListener, this);
        pm.registerEvents(this.serverListPingListener, this);
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new NpcInteractListener(this), this);
        pm.registerEvents(new GuiListener(this), this);
        pm.registerEvents(new HotbarListener(this), this);
        pm.registerEvents(new AnvilListener(this), this);
    }

    private void registerCommands() {
        registerCmd("lobby", new LobbyCommand(this));
        registerCmd("setlobby", new SetLobbyCommand(this));
        registerCmd("hologram", new HologramCommand(this));
        registerCmd("npc", new NpcCommand(this));
        registerCmd("lobbyadmin", new LobbyAdminCommand(this));
    }

    private void registerCmd(String name, Object executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            if (executor instanceof org.bukkit.command.CommandExecutor ce) {
                cmd.setExecutor(ce);
            }
            if (executor instanceof org.bukkit.command.TabCompleter tc) {
                cmd.setTabCompleter(tc);
            }
        } else {
            getLogger().warning("Command '" + name + "' is not registered in plugin.yml!");
        }
    }
}
