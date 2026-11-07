package com.voltic.lobby.command;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.lobby.LobbyData;
import com.voltic.lobby.util.TextUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Modern Paper native BasicCommand for teleporting to a lobby or the auto-balanced default lobby.
 */
public class LobbyCommand implements BasicCommand {

    private final LobbyPlugin plugin;

    public LobbyCommand(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.parse("<red>Only players can execute /lobby.</red>"));
            return;
        }

        if (!plugin.getLobbyManager().hasLobbiesSet()) {
            player.sendMessage(TextUtil.parse("<red>The lobby spawn point has not been configured yet!</red> <gray>Please ask an administrator to set one via <yellow>/setlobby</yellow>.</gray>"));
            return;
        }

        if (args.length == 0) {
            LobbyData best = plugin.getLobbyManager().getBestLobby(player);
            if (best == null) {
                player.sendMessage(TextUtil.parse("<red>No accessible lobbies are currently configured!</red>"));
                return;
            }
            plugin.getLobbyManager().teleportToLobby(player, best.getId());
            return;
        }

        String targetLobby = args[0].toLowerCase(Locale.ROOT);
        LobbyData lobby = plugin.getLobbyManager().getLobby(targetLobby);
        if (lobby == null) {
            player.sendMessage(TextUtil.parse("<red>Lobby '" + targetLobby + "' does not exist!</red>"));
            return;
        }

        plugin.getLobbyManager().teleportToLobby(player, lobby.getId());
    }

    @Override
    public Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
            List<String> suggestions = new ArrayList<>();
            for (String id : plugin.getLobbyManager().getLobbies().keySet()) {
                if (id.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    suggestions.add(id);
                }
            }
            return suggestions;
        }
        return List.of();
    }
}
