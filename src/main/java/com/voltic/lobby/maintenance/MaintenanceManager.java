package com.voltic.lobby.maintenance;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.util.TextUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * High-performance maintenance mode manager.
 * Restricts server entry to administrators and bypass-permitted users.
 */
public class MaintenanceManager {

    private final LobbyPlugin plugin;

    @Getter
    private volatile boolean enabled;

    @Getter
    private volatile Component kickMessage;

    public MaintenanceManager(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
        this.loadConfig();
    }

    /**
     * Loads maintenance mode settings and pre-computes the Adventure kick screen.
     */
    public synchronized void loadConfig() {
        FileConfiguration config = plugin.getConfigManager().getMain();
        this.enabled = config.getBoolean("maintenance.enabled", false);

        List<String> lines = config.getStringList("maintenance.kick-message");
        if (lines.isEmpty()) {
            lines = List.of(
                    "<red><bold>Voltic Network</bold></red>",
                    "<yellow>The server is currently undergoing scheduled maintenance.</yellow>",
                    "<gray>Please check back later!</gray>"
            );
        }

        Component comp = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            comp = comp.append(TextUtil.parse(lines.get(i)));
            if (i < lines.size() - 1) {
                comp = comp.append(Component.newline());
            }
        }
        this.kickMessage = comp;
    }

    /**
     * Toggles maintenance mode and updates configurations live.
     *
     * @param initiator Who triggered the toggle
     * @return New maintenance status
     */
    public boolean toggleMaintenance(@Nullable CommandSender initiator) {
        setMaintenance(!this.enabled, initiator);
        return this.enabled;
    }

    /**
     * Sets maintenance mode status, saves to config, and kicks non-exempt players if enabled.
     *
     * @param enable    Target state
     * @param initiator Who triggered the change
     */
    public void setMaintenance(boolean enable, @Nullable CommandSender initiator) {
        this.enabled = enable;

        FileConfiguration config = plugin.getConfigManager().getMain();
        config.set("maintenance.enabled", enable);
        plugin.getConfigManager().getMainConfig().saveAsync();

        String initiatorName = initiator != null ? initiator.getName() : "Console";

        if (enable) {
            String notice = "<gold>[Maintenance]</gold> <yellow>Maintenance mode has been <green><bold>ENABLED</bold></green> by <gold>" + initiatorName + "</gold>! Non-exempt players have been kicked.</yellow>";
            Bukkit.broadcast(TextUtil.parse(notice), "lobby.admin.maintenance");

            // Kick any online player who does not have bypass permission
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!hasBypass(player)) {
                    player.getScheduler().run(plugin, task -> {
                        if (player.isOnline() && !hasBypass(player)) {
                            player.kick(kickMessage);
                        }
                    }, null);
                }
            }
        } else {
            String notice = "<gold>[Maintenance]</gold> <yellow>Maintenance mode has been <red><bold>DISABLED</bold></red> by <gold>" + initiatorName + "</gold>! Players may now connect freely.</yellow>";
            Bukkit.broadcast(TextUtil.parse(notice), "lobby.admin.maintenance");
        }
    }

    /**
     * Checks if a player has permission to bypass maintenance mode.
     *
     * @param player Target player
     * @return true if exempt
     */
    public boolean hasBypass(@NotNull Player player) {
        return player.isOp() ||
                player.hasPermission("lobby.maintenance.bypass") ||
                player.hasPermission("lobby.admin.maintenance") ||
                player.hasPermission("lobby.admin.*");
    }
}
