package com.voltic.lobby.command;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.lobby.LobbyData;
import com.voltic.lobby.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Command to set the lobby spawn location.
 */
public class SetLobbyCommand implements CommandExecutor, TabCompleter {

    private final LobbyPlugin plugin;

    public SetLobbyCommand(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.parse("<red>Only players can execute /setlobby.</red>"));
            return true;
        }

        if (!player.hasPermission("lobby.admin.setlobby")) {
            player.sendMessage(TextUtil.parse("<red>You do not have permission to execute this command.</red>"));
            return true;
        }

        String lobbyId = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "lobby-1";
        LobbyData existing = plugin.getLobbyManager().getLobby(lobbyId);

        String displayName = existing != null ? existing.getDisplayName() : "<gradient:#00f2fe:#4facfe>Lobby " + lobbyId + "</gradient>";
        int maxPlayers = existing != null ? existing.getMaxPlayers() : 100;

        plugin.getLobbyManager().setLobby(lobbyId, player.getLocation(), displayName, maxPlayers);
        player.sendMessage(TextUtil.parse("<green>Spawn point for <yellow>" + lobbyId + "</yellow> set to your current location!</green>"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (String key : plugin.getLobbyManager().getLobbies().keySet()) {
                if (key.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    list.add(key);
                }
            }
            return list;
        }
        return List.of();
    }
}
