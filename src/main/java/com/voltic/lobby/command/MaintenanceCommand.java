package com.voltic.lobby.command;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.util.TextUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Modern Paper native BasicCommand for toggling and managing maintenance mode.
 */
public class MaintenanceCommand implements BasicCommand {

    private final LobbyPlugin plugin;

    public MaintenanceCommand(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String permission() {
        return "lobby.admin.maintenance";
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();
        if (!sender.hasPermission("lobby.admin.maintenance")) {
            sender.sendMessage(TextUtil.parse("<red>You do not have permission to manage maintenance mode.</red>"));
            return;
        }

        if (args.length == 0) {
            boolean active = plugin.getMaintenanceManager().toggleMaintenance(sender);
            sender.sendMessage(TextUtil.parse("<yellow>Maintenance mode is now " + (active ? "<green><bold>ENABLED</bold></green>" : "<red><bold>DISABLED</bold></red>") + "<yellow>!</yellow>"));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "on", "enable", "start" -> {
                if (plugin.getMaintenanceManager().isEnabled()) {
                    sender.sendMessage(TextUtil.parse("<yellow>Maintenance mode is already <green><bold>ENABLED</bold></green>.</yellow>"));
                    return;
                }
                plugin.getMaintenanceManager().setMaintenance(true, sender);
                sender.sendMessage(TextUtil.parse("<green><bold>Maintenance mode enabled!</bold> Non-whitelisted players have been disconnected.</green>"));
            }
            case "off", "disable", "stop" -> {
                if (!plugin.getMaintenanceManager().isEnabled()) {
                    sender.sendMessage(TextUtil.parse("<yellow>Maintenance mode is already <red><bold>DISABLED</bold></red>.</yellow>"));
                    return;
                }
                plugin.getMaintenanceManager().setMaintenance(false, sender);
                sender.sendMessage(TextUtil.parse("<green><bold>Maintenance mode disabled!</bold> Players may now join freely.</green>"));
            }
            case "toggle" -> {
                boolean active = plugin.getMaintenanceManager().toggleMaintenance(sender);
                sender.sendMessage(TextUtil.parse("<yellow>Maintenance mode toggled to " + (active ? "<green><bold>ENABLED</bold></green>" : "<red><bold>DISABLED</bold></red>") + "<yellow>!</yellow>"));
            }
            case "status" -> {
                boolean active = plugin.getMaintenanceManager().isEnabled();
                sender.sendMessage(TextUtil.parse("<gold><bold>--- Maintenance Mode Status ---</bold></gold>"));
                sender.sendMessage(TextUtil.parse("<yellow>Status: " + (active ? "<green><bold>ACTIVE (Restricted)</bold></green>" : "<red><bold>INACTIVE (Public)</bold></red>")));
                sender.sendMessage(TextUtil.parse("<gray>Use <yellow>/maintenance on</yellow> or <yellow>/maintenance off</yellow> to change.</gray>"));
            }
            default -> {
                sender.sendMessage(TextUtil.parse("<gold><bold>Maintenance Mode Commands:</bold></gold>"));
                sender.sendMessage(TextUtil.parse("<yellow>/maintenance</yellow> <gray>- Toggle maintenance on/off</gray>"));
                sender.sendMessage(TextUtil.parse("<yellow>/maintenance on</yellow> <gray>- Enable maintenance mode</gray>"));
                sender.sendMessage(TextUtil.parse("<yellow>/maintenance off</yellow> <gray>- Disable maintenance mode</gray>"));
                sender.sendMessage(TextUtil.parse("<yellow>/maintenance status</yellow> <gray>- View maintenance status</gray>"));
            }
        }
    }

    @Override
    public Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
            List<String> list = List.of("on", "off", "toggle", "status");
            return list.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
