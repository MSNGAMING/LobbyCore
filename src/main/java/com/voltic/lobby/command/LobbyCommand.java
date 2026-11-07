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
 * Command to teleport to a lobby or the auto-balanced default lobby.
 */
public class LobbyCommand implements CommandExecutor, TabCompleter {

    private final LobbyPlugin plugin;

    public LobbyCommand(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.parse("<red>Only players can execute /lobby.</red>"));
            return true;
        }

        if (args.length == 0) {
            LobbyData best = plugin.getLobbyManager().getBestLobby(player);
            if (best == null) {
                player.sendMessage(TextUtil.parse("<red>No accessible lobbies are currently configured!</red>"));
                return true;
            }
            plugin.getLobbyManager().teleportToLobby(player, best.getId());
            return true;
        }

        String targetLobby = args[0].toLowerCase(Locale.ROOT);
        LobbyData lobby = plugin.getLobbyManager().getLobby(targetLobby);
        if (lobby == null) {
            player.sendMessage(TextUtil.parse("<red>Lobby '" + targetLobby + "' does not exist!</red>"));
            return true;
        }

        plugin.getLobbyManager().teleportToLobby(player, lobby.getId());
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            for (String id : plugin.getLobbyManager().getLobbies().keySet()) {
                if (id.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    suggestions.add(id);
                }
            }
            return suggestions;
        }
        return List.of();
    }
}
