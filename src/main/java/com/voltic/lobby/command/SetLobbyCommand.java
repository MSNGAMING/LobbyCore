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
 * Modern Paper native BasicCommand for setting a lobby spawn location.
 */
public class SetLobbyCommand implements BasicCommand {

    private final LobbyPlugin plugin;

    public SetLobbyCommand(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String permission() {
        return "lobby.admin.setlobby";
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.parse("<red>Only players can execute /setlobby.</red>"));
            return;
        }

        String lobbyId = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "lobby-1";
        LobbyData existing = plugin.getLobbyManager().getLobby(lobbyId);

        String displayName = existing != null ? existing.getDisplayName() : "<gradient:#00f2fe:#4facfe>Lobby " + lobbyId + "</gradient>";
        int maxPlayers = existing != null ? existing.getMaxPlayers() : 100;

        plugin.getLobbyManager().setLobby(lobbyId, player.getLocation(), displayName, maxPlayers);
        player.sendMessage(TextUtil.parse("<green>Spawn point for <yellow>" + lobbyId + "</yellow> set to your current location!</green>"));
    }

    @Override
    public Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
            List<String> list = new ArrayList<>();
            for (String key : plugin.getLobbyManager().getLobbies().keySet()) {
                if (key.startsWith(prefix)) {
                    list.add(key);
                }
            }
            return list;
        }
        return List.of();
    }
}
