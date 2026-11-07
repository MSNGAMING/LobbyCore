package com.voltic.lobby.command;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.gui.AdminConfigGui;
import com.voltic.lobby.gui.LobbyManagementGui;
import com.voltic.lobby.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Command for opening the Admin GUI control panel, Lobby Worlds Manager, or triggering hot-reloads.
 */
public class LobbyAdminCommand implements CommandExecutor, TabCompleter {

    private final LobbyPlugin plugin;

    public LobbyAdminCommand(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("lobby.admin.gui")) {
            sender.sendMessage(TextUtil.parse("<red>You do not have permission to use admin lobby commands.</red>"));
            return true;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            if (sub.equals("reload")) {
                plugin.reloadPlugin();
                sender.sendMessage(TextUtil.parse("<green><bold>Lobby plugin hot-reloaded successfully!</bold></green>"));
                return true;
            } else if (sub.equals("lobbies") || sub.equals("worlds")) {
                if (sender instanceof Player player) {
                    LobbyManagementGui.open(player, plugin);
                    return true;
                }
                sender.sendMessage(TextUtil.parse("<red>Only in-game players can open the Lobby Worlds Manager GUI.</red>"));
                return true;
            }
        }

        if (sender instanceof Player player) {
            AdminConfigGui.open(player, plugin);
            return true;
        }

        sender.sendMessage(TextUtil.parse("<yellow>Console usage: /" + label + " reload</yellow>"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload", "gui", "lobbies", "worlds");
        }
        return List.of();
    }
}
