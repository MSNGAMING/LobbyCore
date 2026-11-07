package com.voltic.lobby.command;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.gui.AdminConfigGui;
import com.voltic.lobby.gui.LobbyManagementGui;
import com.voltic.lobby.util.TextUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Modern Paper native BasicCommand for opening the Admin GUI control panel, Lobby Worlds Manager, or triggering hot-reloads.
 */
public class LobbyAdminCommand implements BasicCommand {

    private final LobbyPlugin plugin;

    public LobbyAdminCommand(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String permission() {
        return "lobby.admin.gui";
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();
        if (!sender.hasPermission("lobby.admin.gui")) {
            sender.sendMessage(TextUtil.parse("<red>You do not have permission to use admin lobby commands.</red>"));
            return;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("reload")) {
                plugin.reloadPlugin();
                sender.sendMessage(TextUtil.parse("<green><bold>Lobby plugin hot-reloaded successfully!</bold></green>"));
                return;
            } else if (sub.equals("lobbies") || sub.equals("worlds")) {
                if (sender instanceof Player player) {
                    LobbyManagementGui.open(player, plugin);
                    return;
                }
                sender.sendMessage(TextUtil.parse("<red>Only in-game players can open the Lobby Worlds Manager GUI.</red>"));
                return;
            }
        }

        if (sender instanceof Player player) {
            AdminConfigGui.open(player, plugin);
            return;
        }

        sender.sendMessage(TextUtil.parse("<yellow>Console usage: /lobbyadmin reload</yellow>"));
    }

    @Override
    public Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
            List<String> list = List.of("reload", "gui", "lobbies", "worlds");
            return list.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
