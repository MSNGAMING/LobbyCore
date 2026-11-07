package com.voltic.lobby.gui;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.lobby.LobbyData;
import com.voltic.lobby.util.ItemBuilder;
import com.voltic.lobby.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Interactive In-Game GUI for managing multi-world lobby instances, spawns, and balancing.
 */
public class LobbyManagementGui {

    public static final String GUI_TITLE = "<bold><gradient:#00f2fe:#4facfe>Lobby Worlds Manager</gradient></bold>";
    public static final NamespacedKey ACTION_KEY = new NamespacedKey("lobby", "lobby_mgmt_action");
    public static final NamespacedKey LOBBY_ID_KEY = new NamespacedKey("lobby", "lobby_mgmt_id");

    /**
     * Opens the Lobby Management GUI for an administrative player.
     *
     * @param player Admin player
     * @param plugin LobbyPlugin instance
     */
    public static void open(@NotNull Player player, @NotNull LobbyPlugin plugin) {
        if (!player.hasPermission("lobby.admin.gui")) {
            player.sendMessage(TextUtil.parse("<red>You do not have permission to access the Lobby Worlds Manager.</red>"));
            return;
        }

        LobbyManagementGuiHolder holder = new LobbyManagementGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, TextUtil.parse(GUI_TITLE));
        holder.setInventory(inv);

        refresh(inv, plugin);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
    }

    /**
     * Refreshes the items in the Lobby Worlds Management GUI.
     *
     * @param inv    Inventory to refresh
     * @param plugin LobbyPlugin instance
     */
    public static void refresh(@NotNull Inventory inv, @NotNull LobbyPlugin plugin) {
        inv.clear();

        List<LobbyData> sortedLobbies = new ArrayList<>(plugin.getLobbyManager().getLobbies().values());
        sortedLobbies.sort(Comparator.comparing(LobbyData::getId));

        String defaultId = plugin.getLobbyManager().getDefaultLobbyId().toLowerCase(Locale.ROOT);
        boolean autoBalancing = plugin.getLobbyManager().isAutoBalancing();

        // Populate lobby cards (slots 0 to 35)
        if (sortedLobbies.isEmpty()) {
            inv.setItem(13, ItemBuilder.from(Material.BARRIER)
                    .name("<red><bold>No Lobby Worlds Configured!</bold></red>")
                    .loreStrings(
                            "<gray>Players will not be teleported to any lobby</gray>",
                            "<gray>until at least one lobby instance is created.</gray>",
                            "",
                            "<green>▶ Click '+ Register New Lobby' below to create one!</green>"
                    )
                    .build());
        } else {
            int slot = 0;
            for (LobbyData lobby : sortedLobbies) {
                if (slot >= 36) break;

                boolean isDefault = lobby.getId().equalsIgnoreCase(defaultId);
                boolean isEnabled = lobby.isEnabled();
                Location loc = lobby.getSpawnLocation();
                String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "unloaded";
                int playerCount = plugin.getLobbyManager().getPlayerCountInLobby(lobby);

                Material icon = isEnabled ? (isDefault ? Material.BEACON : Material.EMERALD_BLOCK) : Material.REDSTONE_BLOCK;

                inv.setItem(slot++, ItemBuilder.from(icon)
                        .name(lobby.getDisplayName() + " <gray>(" + lobby.getId() + ")</gray>")
                        .loreStrings(
                                "<gray>World: <white>" + worldName + "</white></gray>",
                                String.format(Locale.ROOT, "<gray>Spawn: <yellow>%.1f, %.1f, %.1f</yellow> <dark_gray>(Yaw: %.0f, Pitch: %.0f)</dark_gray></gray>",
                                        loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()),
                                "<gray>Population: <aqua>" + playerCount + "</aqua><gray>/</gray><white>" + lobby.getMaxPlayers() + "</white> <gray>players</gray>",
                                "<gray>Status: " + statusTag(isEnabled),
                                "<gray>Default Lobby: " + (isDefault ? "<gold><bold>YES</bold></gold>" : "<gray>No</gray>"),
                                "<gray>Permission: <white>" + (lobby.getPermission() != null && !lobby.getPermission().isEmpty() ? lobby.getPermission() : "None (Public)") + "</white></gray>",
                                "",
                                "<yellow>▶ Left-Click: Teleport here</yellow>",
                                "<gold>▶ Right-Click: Toggle Enable / Disable</gold>",
                                "<aqua>▶ Shift + Left-Click: Set spawn to your location</aqua>",
                                "<light_purple>▶ Shift + Right-Click: Set as Default Lobby</light_purple>",
                                "<red>▶ Drop Key (Q): Delete this lobby</red>"
                        )
                        .tagString(ACTION_KEY, "LOBBY_CARD")
                        .tagString(LOBBY_ID_KEY, lobby.getId())
                        .build());
            }
        }

        // Row 5: Filler separator
        ItemStack divider = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                .name(Component.text(" "))
                .build();
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, divider);
        }

        // Slot 45: Back to Control Panel
        inv.setItem(45, ItemBuilder.from(Material.ARROW)
                .name("<yellow><bold>← Back to Control Panel</bold></yellow>")
                .loreStrings(
                        "<gray>Return to the main configuration GUI.</gray>",
                        "",
                        "<yellow>▶ Click to go back</yellow>"
                )
                .tagString(ACTION_KEY, "BACK_ADMIN")
                .build());

        // Slot 48: Auto-Balancing Toggle
        inv.setItem(48, ItemBuilder.from(Material.COMPARATOR)
                .name("<yellow><bold>Auto-Balancing Engine</bold></yellow>")
                .loreStrings(
                        "<gray>Directs players automatically to the least populated lobby.</gray>",
                        "",
                        "<white>Status: " + statusTag(autoBalancing),
                        "",
                        "<yellow>▶ Click to toggle</yellow>"
                )
                .tagString(ACTION_KEY, "TOGGLE_AUTOBALANCE")
                .build());

        // Slot 49: Create New Lobby
        inv.setItem(49, ItemBuilder.from(Material.NETHER_STAR)
                .name("<gradient:#00ff87:#60efff><bold>+ REGISTER NEW LOBBY</bold></gradient>")
                .loreStrings(
                        "<gray>Creates a new lobby instance at your current coordinates.</gray>",
                        "<gray>You will be prompted to type an ID in an anvil editor.</gray>",
                        "",
                        "<green>▶ Click to Register</green>"
                )
                .glow(true)
                .tagString(ACTION_KEY, "CREATE_LOBBY")
                .build());

        // Slot 50: Quick Set Default Spawn
        inv.setItem(50, ItemBuilder.from(Material.COMPASS)
                .name("<aqua><bold>Quick-Set Default Lobby</bold></aqua>")
                .loreStrings(
                        "<gray>Instantly sets the active default lobby spawn</gray>",
                        "<gray>to your current location and eye direction.</gray>",
                        "",
                        "<yellow>▶ Click to set spawn</yellow>"
                )
                .tagString(ACTION_KEY, "QUICK_SET")
                .build());

        // Slot 53: Close Menu
        inv.setItem(53, ItemBuilder.from(Material.BARRIER)
                .name("<red><bold>Close Menu</bold></red>")
                .tagString(ACTION_KEY, "CLOSE")
                .build());
    }

    private static String statusTag(boolean enabled) {
        return enabled ? "<green><bold>ENABLED</bold></green>" : "<red><bold>DISABLED</bold></red>";
    }
}
