package com.voltic.lobby.command;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.npc.NpcData;
import com.voltic.lobby.util.TextUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Modern Paper native BasicCommand for managing Paper Interaction/visual NPCs.
 */
public class NpcCommand implements BasicCommand {

    private final LobbyPlugin plugin;

    public NpcCommand(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String permission() {
        return "lobby.admin.npc";
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();
        if (!sender.hasPermission("lobby.admin.npc")) {
            sender.sendMessage(TextUtil.parse("<red>You do not have permission to manage NPCs.</red>"));
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
                    sender.sendMessage(TextUtil.parse("<red>Only players can create an NPC at their position.</red>"));
                    return;
                }
                if (args.length < 4) {
                    player.sendMessage(TextUtil.parse("<red>Usage: /npc create <id> <entity_type> <name></red>"));
                    return;
                }
                String id = args[1].toLowerCase(Locale.ROOT);
                EntityType type;
                try {
                    type = EntityType.valueOf(args[2].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    player.sendMessage(TextUtil.parse("<red>Invalid entity type: " + args[2] + "</red>"));
                    return;
                }
                String name = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

                plugin.getNpcManager().createNpc(id, type, name, player.getLocation(), NpcData.ActionType.COMMAND, "say Hello %player%!");
                player.sendMessage(TextUtil.parse("<green>NPC '<yellow>" + id + "</yellow>' created successfully!</green>"));
            }
            case "delete", "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage(TextUtil.parse("<red>Usage: /npc delete <id></red>"));
                    return;
                }
                String id = args[1].toLowerCase(Locale.ROOT);
                if (plugin.getNpcManager().deleteNpc(id)) {
                    sender.sendMessage(TextUtil.parse("<green>NPC '<yellow>" + id + "</yellow>' deleted!</green>"));
                } else {
                    sender.sendMessage(TextUtil.parse("<red>NPC not found!</red>"));
                }
            }
            case "action" -> {
                if (args.length < 4) {
                    sender.sendMessage(TextUtil.parse("<red>Usage: /npc action <id> <COMMAND|TELEPORT|MESSAGE|GUI> <value></red>"));
                    return;
                }
                String id = args[1].toLowerCase(Locale.ROOT);
                NpcData npc = plugin.getNpcManager().getNpc(id);
                if (npc == null) {
                    sender.sendMessage(TextUtil.parse("<red>NPC not found!</red>"));
                    return;
                }
                NpcData.ActionType actionType;
                try {
                    actionType = NpcData.ActionType.valueOf(args[2].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(TextUtil.parse("<red>Invalid action type. Must be: COMMAND, TELEPORT, MESSAGE, GUI</red>"));
                    return;
                }
                String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                npc.setActionType(actionType);
                npc.setActionValue(value);
                plugin.getNpcManager().saveNpc(npc);
                sender.sendMessage(TextUtil.parse("<green>Updated action for NPC '<yellow>" + id + "</yellow>' to <gold>" + actionType + "</gold>!</green>"));
            }
            case "movehere" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(TextUtil.parse("<red>Only players can move NPCs.</red>"));
                    return;
                }
                if (args.length < 2) {
                    player.sendMessage(TextUtil.parse("<red>Usage: /npc movehere <id></red>"));
                    return;
                }
                String id = args[1].toLowerCase(Locale.ROOT);
                NpcData npc = plugin.getNpcManager().getNpc(id);
                if (npc == null) {
                    player.sendMessage(TextUtil.parse("<red>NPC not found!</red>"));
                    return;
                }
                npc.setLocation(player.getLocation());
                plugin.getNpcManager().saveNpc(npc);
                plugin.getNpcManager().spawnNpc(npc);
                player.sendMessage(TextUtil.parse("<green>Moved NPC '<yellow>" + id + "</yellow>' to your location!</green>"));
            }
            case "list" -> {
                sender.sendMessage(TextUtil.parse("<gold><bold>--- Registered NPCs ---</bold></gold>"));
                for (NpcData n : plugin.getNpcManager().getNpcs().values()) {
                    sender.sendMessage(TextUtil.parse("<yellow>• " + n.getId() + "</yellow> <gray>(" + n.getEntityType() + ", Action: " + n.getActionType() + ")</gray>"));
                }
            }
            default -> sendHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(TextUtil.parse("<gold><bold>NPC Management Commands:</bold></gold>"));
        sender.sendMessage(TextUtil.parse("<yellow>/npc create <id> <type> <name></yellow> <gray>- Create NPC</gray>"));
        sender.sendMessage(TextUtil.parse("<yellow>/npc delete <id></yellow> <gray>- Delete NPC</gray>"));
        sender.sendMessage(TextUtil.parse("<yellow>/npc action <id> <type> <val></yellow> <gray>- Set action</gray>"));
        sender.sendMessage(TextUtil.parse("<yellow>/npc movehere <id></yellow> <gray>- Move NPC here</gray>"));
        sender.sendMessage(TextUtil.parse("<yellow>/npc list</yellow> <gray>- List all NPCs</gray>"));
    }

    @Override
    public Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
            List<String> list = List.of("create", "delete", "action", "movehere", "list");
            return list.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("action") || args[0].equalsIgnoreCase("movehere"))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return plugin.getNpcManager().getNpcs().keySet().stream()
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            String prefix = args[2].toUpperCase(Locale.ROOT);
            List<String> types = List.of("VILLAGER", "ALLAY", "IRON_GOLEM", "ARMOR_STAND", "ZOMBIE", "SKELETON", "WANDERING_TRADER");
            return types.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("action")) {
            String prefix = args[2].toUpperCase(Locale.ROOT);
            List<String> actions = List.of("COMMAND", "TELEPORT", "MESSAGE", "GUI");
            return actions.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
