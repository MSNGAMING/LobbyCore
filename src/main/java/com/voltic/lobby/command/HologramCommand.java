package com.voltic.lobby.command;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.hologram.Hologram;
import com.voltic.lobby.hologram.HologramManager;
import com.voltic.lobby.util.TextUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Modern Paper native BasicCommand for managing TextDisplay holograms.
 */
public class HologramCommand implements BasicCommand {

    private final LobbyPlugin plugin;

    public HologramCommand(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String permission() {
        return "lobby.admin.hologram";
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();
        if (!sender.hasPermission("lobby.admin.hologram")) {
            sender.sendMessage(TextUtil.parse("<red>You do not have permission to manage holograms.</red>"));
            return;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(TextUtil.parse("<red>Only players can create holograms at their position.</red>"));
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(TextUtil.parse("<red>Usage: /holo create <id> <text...></red>"));
                    return;
                }
                String id = args[1].toLowerCase(Locale.ROOT);
                String line = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                plugin.getHologramManager().createHologram(id, player.getLocation().add(0, 1.2, 0), List.of(line));
                player.sendMessage(TextUtil.parse("<green>Hologram '<yellow>" + id + "</yellow>' created successfully!</green>"));
            }
            case "delete", "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage(TextUtil.parse("<red>Usage: /holo delete <id></red>"));
                    return;
                }
                String id = args[1].toLowerCase(Locale.ROOT);
                if (plugin.getHologramManager().deleteHologram(id)) {
                    sender.sendMessage(TextUtil.parse("<green>Hologram '<yellow>" + id + "</yellow>' deleted!</green>"));
                } else {
                    sender.sendMessage(TextUtil.parse("<red>Hologram not found!</red>"));
                }
            }
            case "addline" -> {
                if (args.length < 3) {
                    sender.sendMessage(TextUtil.parse("<red>Usage: /holo addline <id> <text...></red>"));
                    return;
                }
                String id = args[1].toLowerCase(Locale.ROOT);
                Hologram holo = plugin.getHologramManager().getHologram(id);
                if (holo == null) {
                    sender.sendMessage(TextUtil.parse("<red>Hologram not found!</red>"));
                    return;
                }
                String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                List<String> currentLines = new ArrayList<>(holo.getLines());
                currentLines.add(text);
                plugin.getHologramManager().updateLines(id, currentLines);
                sender.sendMessage(TextUtil.parse("<green>Added line to hologram '<yellow>" + id + "</yellow>'!</green>"));
            }
            case "preset", "load" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(TextUtil.parse("<red>Only in-game players can spawn hologram presets.</red>"));
                    return;
                }
                if (args.length < 2) {
                    player.sendMessage(TextUtil.parse("<red>Usage: /holo preset <preset-name> [id]</red>"));
                    player.sendMessage(TextUtil.parse("<gray>Available presets: <yellow>" + String.join(", ", plugin.getHologramManager().getPresets().keySet()) + "</yellow></gray>"));
                    return;
                }
                String presetName = args[1].toLowerCase(Locale.ROOT);
                if (!plugin.getHologramManager().getPresets().containsKey(presetName)) {
                    player.sendMessage(TextUtil.parse("<red>Unknown preset '<yellow>" + presetName + "</yellow>'.</red>"));
                    player.sendMessage(TextUtil.parse("<gray>Available presets: <yellow>" + String.join(", ", plugin.getHologramManager().getPresets().keySet()) + "</yellow></gray>"));
                    return;
                }

                String id = (args.length >= 3) ? args[2].toLowerCase(Locale.ROOT) : presetName;
                if (plugin.getHologramManager().getHolograms().containsKey(id)) {
                    int counter = 1;
                    while (plugin.getHologramManager().getHolograms().containsKey(id + "-" + counter)) {
                        counter++;
                    }
                    id = id + "-" + counter;
                }

                Hologram spawned = plugin.getHologramManager().spawnPreset(presetName, id, player.getLocation().add(0, 1.2, 0));
                if (spawned != null) {
                    player.sendMessage(TextUtil.parse("<green>Hologram '<yellow>" + id + "</yellow>' spawned from preset '<gold>" + presetName + "</gold>'!</green>"));
                } else {
                    player.sendMessage(TextUtil.parse("<red>Failed to spawn hologram preset.</red>"));
                }
            }
            case "presets" -> {
                sender.sendMessage(TextUtil.parse("<gold><bold>--- Available Hologram Presets ---</bold></gold>"));
                if (plugin.getHologramManager().getPresets().isEmpty()) {
                    sender.sendMessage(TextUtil.parse("<gray>No presets configured in holograms.yml.</gray>"));
                } else {
                    for (HologramManager.HologramPreset p : plugin.getHologramManager().getPresets().values()) {
                        sender.sendMessage(TextUtil.parse("<yellow>• " + p.name() + "</yellow> <gray>(" + p.lines().size() + " lines) - <white>/holo preset " + p.name() + " [id]</white></gray>"));
                    }
                }
            }
            case "list" -> {
                sender.sendMessage(TextUtil.parse("<gold><bold>--- Registered Holograms ---</bold></gold>"));
                for (Hologram h : plugin.getHologramManager().getHolograms().values()) {
                    sender.sendMessage(TextUtil.parse("<yellow>• " + h.getId() + "</yellow> <gray>(" + h.getLines().size() + " lines)</gray>"));
                }
            }
            case "reload" -> {
                plugin.getConfigManager().getHologramsConfig().reload();
                plugin.getHologramManager().loadHolograms();
                sender.sendMessage(TextUtil.parse("<green>Holograms reloaded successfully!</green>"));
            }
            default -> sendHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(TextUtil.parse("<gold><bold>Hologram Management Commands:</bold></gold>"));
        sender.sendMessage(TextUtil.parse("<yellow>/holo preset <name> [id]</yellow> <gray>- Spawn a pre-configured template</gray>"));
        sender.sendMessage(TextUtil.parse("<yellow>/holo presets</yellow> <gray>- List all built-in templates</gray>"));
        sender.sendMessage(TextUtil.parse("<yellow>/holo create <id> <text></yellow> <gray>- Create custom hologram</gray>"));
        sender.sendMessage(TextUtil.parse("<yellow>/holo delete <id></yellow> <gray>- Delete hologram</gray>"));
        sender.sendMessage(TextUtil.parse("<yellow>/holo addline <id> <text></yellow> <gray>- Append text line</gray>"));
        sender.sendMessage(TextUtil.parse("<yellow>/holo list</yellow> <gray>- List all active holograms</gray>"));
        sender.sendMessage(TextUtil.parse("<yellow>/holo reload</yellow> <gray>- Reload from config</gray>"));
    }

    @Override
    public Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
            List<String> list = List.of("preset", "presets", "create", "delete", "addline", "list", "reload");
            return list.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("preset") || args[0].equalsIgnoreCase("load"))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return plugin.getHologramManager().getPresets().keySet().stream()
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("addline"))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return plugin.getHologramManager().getHolograms().keySet().stream()
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
