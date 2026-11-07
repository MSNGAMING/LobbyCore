package com.voltic.lobby.gui;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.util.ItemBuilder;
import com.voltic.lobby.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interactive In-Game Configuration GUI for administrators.
 * Modifies YAML configurations live and triggers hot-reloads without server restarts.
 */
public class AdminConfigGui {

    public static final String GUI_TITLE = "<bold><gradient:#00f2fe:#4facfe>Lobby Control Panel</gradient></bold>";
    public static final NamespacedKey ACTION_KEY = new NamespacedKey("lobby", "gui_action");

    /**
     * Opens the administrative configuration GUI for the player.
     *
     * @param player Admin player
     * @param plugin LobbyPlugin instance
     */
    public static void open(@NotNull Player player, @NotNull LobbyPlugin plugin) {
        if (!player.hasPermission("lobby.admin.gui")) {
            player.sendMessage(TextUtil.parse("<red>You do not have permission to access the Lobby Control Panel.</red>"));
            return;
        }

        AdminGuiHolder holder = new AdminGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, TextUtil.parse(GUI_TITLE));
        holder.setInventory(inv);

        refresh(inv, plugin);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
    }

    /**
     * Refreshes all items inside the GUI to reflect live configuration states.
     *
     * @param inv    Inventory to refresh
     * @param plugin LobbyPlugin instance
     */
    public static void refresh(@NotNull Inventory inv, @NotNull LobbyPlugin plugin) {
        FileConfiguration main = plugin.getConfigManager().getMain();
        FileConfiguration chat = plugin.getConfigManager().getChat();
        FileConfiguration motd = plugin.getConfigManager().getMotd();
        FileConfiguration tab = plugin.getConfigManager().getTab();
        FileConfiguration sb = plugin.getConfigManager().getScoreboard();

        ItemStack filler = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                .name(Component.text(" "))
                .build();

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Slot 11: PvP Protection
        boolean pvp = main.getBoolean("protection.pvp-enabled", false);
        inv.setItem(11, ItemBuilder.from(Material.DIAMOND_SWORD)
                .name("<yellow><bold>PvP Combat Rule</bold></yellow>")
                .loreStrings(
                        "<gray>Controls whether players can attack each other.</gray>",
                        "",
                        "<white>Status: " + statusTag(pvp),
                        "",
                        "<yellow>▶ Click to toggle</yellow>"
                )
                .tagString(ACTION_KEY, "TOGGLE_PVP")
                .build());

        // Slot 13: Block Break & Place Protection
        boolean blockBreak = main.getBoolean("protection.block-break", false);
        boolean blockPlace = main.getBoolean("protection.block-place", false);
        inv.setItem(13, ItemBuilder.from(Material.BRICKS)
                .name("<yellow><bold>Building Protections</bold></yellow>")
                .loreStrings(
                        "<gray>Controls block breaking and placement in lobby worlds.</gray>",
                        "",
                        "<white>Block Break: " + statusTag(blockBreak),
                        "<white>Block Place: " + statusTag(blockPlace),
                        "",
                        "<yellow>▶ Left-Click: Toggle Break</yellow>",
                        "<yellow>▶ Right-Click: Toggle Place</yellow>"
                )
                .tagString(ACTION_KEY, "TOGGLE_BUILD")
                .build());

        // Slot 15: Hunger Depletion
        boolean hunger = main.getBoolean("protection.hunger-depletion", false);
        inv.setItem(15, ItemBuilder.from(Material.COOKED_BEEF)
                .name("<yellow><bold>Hunger Depletion</bold></yellow>")
                .loreStrings(
                        "<gray>Controls if hunger drains in lobby worlds.</gray>",
                        "",
                        "<white>Status: " + statusTag(hunger),
                        "",
                        "<yellow>▶ Click to toggle</yellow>"
                )
                .tagString(ACTION_KEY, "TOGGLE_HUNGER")
                .build());

        // Slot 20: Weather & Time Lock
        boolean weather = main.getBoolean("protection.weather-lock", true);
        inv.setItem(20, ItemBuilder.from(Material.CLOCK)
                .name("<yellow><bold>Weather & Time Lock</bold></yellow>")
                .loreStrings(
                        "<gray>Forces clear skies and locks lobby world time.</gray>",
                        "",
                        "<white>Weather Lock: " + statusTag(weather),
                        "",
                        "<yellow>▶ Click to toggle Weather Lock</yellow>"
                )
                .tagString(ACTION_KEY, "TOGGLE_WEATHER")
                .build());

        // Slot 22: Chat Moderation
        boolean swear = chat.getBoolean("anti-swear.enabled", true);
        boolean spam = chat.getBoolean("anti-spam.enabled", true);
        String action = chat.getString("anti-swear.action", "MASK");
        inv.setItem(22, ItemBuilder.from(Material.WRITABLE_BOOK)
                .name("<yellow><bold>Chat Moderation</bold></yellow>")
                .loreStrings(
                        "<gray>Configure real-time chat filters and formatting.</gray>",
                        "",
                        "<white>Anti-Swear: " + statusTag(swear) + " <gray>(" + action + ")</gray>",
                        "<white>Anti-Spam: " + statusTag(spam),
                        "",
                        "<yellow>▶ Left-Click: Toggle Anti-Swear</yellow>",
                        "<yellow>▶ Right-Click: Toggle Anti-Spam</yellow>",
                        "<yellow>▶ Shift-Click: Edit Chat Format (Anvil)</yellow>"
                )
                .tagString(ACTION_KEY, "TOGGLE_CHAT")
                .build());

        // Slot 24: MOTD System
        boolean motdEnabled = motd.getBoolean("motd.enabled", true);
        boolean hoverEnabled = motd.getBoolean("motd.hover.enabled", true);
        inv.setItem(24, ItemBuilder.from(Material.OAK_SIGN)
                .name("<yellow><bold>Server List MOTD</bold></yellow>")
                .loreStrings(
                        "<gray>Server list ping message and player hover samples.</gray>",
                        "",
                        "<white>MOTD Active: " + statusTag(motdEnabled),
                        "<white>Hover Sample: " + statusTag(hoverEnabled),
                        "",
                        "<yellow>▶ Left-Click: Edit MOTD Line 1 (Anvil)</yellow>",
                        "<yellow>▶ Right-Click: Edit MOTD Line 2 (Anvil)</yellow>",
                        "<yellow>▶ Shift-Click: Toggle MOTD System</yellow>"
                )
                .tagString(ACTION_KEY, "TOGGLE_MOTD")
                .build());

        // Slot 29: Side Scoreboard
        boolean sbEnabled = sb.getBoolean("scoreboard.enabled", true);
        long sbInterval = sb.getLong("scoreboard.update-interval-ticks", 20L);
        inv.setItem(29, ItemBuilder.from(Material.ITEM_FRAME)
                .name("<yellow><bold>Side Scoreboard</bold></yellow>")
                .loreStrings(
                        "<gray>Flicker-free sidebar scoreboard settings.</gray>",
                        "",
                        "<white>Status: " + statusTag(sbEnabled),
                        "<white>Update Rate: <gold>" + sbInterval + " ticks</gold>",
                        "",
                        "<yellow>▶ Left-Click: Toggle Scoreboard</yellow>",
                        "<yellow>▶ Right-Click: Cycle Update Rate (10/20/40)</yellow>",
                        "<yellow>▶ Shift-Click: Edit Scoreboard Title (Anvil)</yellow>"
                )
                .tagString(ACTION_KEY, "TOGGLE_SCOREBOARD")
                .build());

        // Slot 31: Tablist Header & Footer
        boolean tabEnabled = tab.getBoolean("tab.enabled", true);
        inv.setItem(31, ItemBuilder.from(Material.PLAYER_HEAD)
                .name("<yellow><bold>Tablist Display</bold></yellow>")
                .loreStrings(
                        "<gray>Dynamic Adventure MiniMessage Tablist header & footer.</gray>",
                        "",
                        "<white>Status: " + statusTag(tabEnabled),
                        "",
                        "<yellow>▶ Click to toggle Tablist</yellow>"
                )
                .tagString(ACTION_KEY, "TOGGLE_TAB")
                .build());

        // Slot 33: Void Fall Rescue
        boolean voidFall = main.getBoolean("protection.void-fall.enabled", true);
        double voidY = main.getDouble("protection.void-fall.threshold-y", -50.0);
        inv.setItem(33, ItemBuilder.from(Material.ENDER_PEARL)
                .name("<yellow><bold>Void Fall Rescue</bold></yellow>")
                .loreStrings(
                        "<gray>Teleports players falling into the void back to spawn.</gray>",
                        "",
                        "<white>Status: " + statusTag(voidFall),
                        "<white>Threshold Y: <gold>" + voidY + "</gold>",
                        "",
                        "<yellow>▶ Click to toggle Void Rescue</yellow>"
                )
                .tagString(ACTION_KEY, "TOGGLE_VOID")
                .build());

        // Slot 40: Lobby Worlds Management
        int lobbyCount = plugin.getLobbyManager().getLobbies().size();
        boolean hasSet = plugin.getLobbyManager().hasLobbiesSet();
        inv.setItem(40, ItemBuilder.from(Material.BEACON)
                .name("<gradient:#00f2fe:#4facfe><bold>LOBBY WORLDS MANAGER</bold></gradient>")
                .loreStrings(
                        "<gray>Configure multi-lobby instances, spawns,</gray>",
                        "<gray>world balancing, and player capacities.</gray>",
                        "",
                        "<white>Configured Lobbies: <gold>" + lobbyCount + "</gold>",
                        "<white>Lobby Status: " + statusTag(hasSet),
                        "",
                        "<yellow>▶ Click to Open Worlds Manager</yellow>"
                )
                .glow(true)
                .tagString(ACTION_KEY, "OPEN_LOBBY_MANAGER")
                .build());

        // Slot 49: Hot-Reload Everything
        inv.setItem(49, ItemBuilder.from(Material.NETHER_STAR)
                .name("<gradient:#00ff87:#60efff><bold>HOT-RELOAD ALL CONFIGURATIONS</bold></gradient>")
                .loreStrings(
                        "<gray>Reloads all 8 YAML files from disk,",
                        "<gray>refreshes Holograms, NPCs, Chat, Tab, and Scoreboard.</gray>",
                        "",
                        "<green>▶ Click to Hot-Reload</green>"
                )
                .glow(true)
                .tagString(ACTION_KEY, "RELOAD_ALL")
                .build());

        // Slot 48: Close
        inv.setItem(48, ItemBuilder.from(Material.BARRIER)
                .name("<red><bold>Close Menu</bold></red>")
                .tagString(ACTION_KEY, "CLOSE")
                .build());
    }

    private static String statusTag(boolean enabled) {
        return enabled ? "<green><bold>ENABLED</bold></green>" : "<red><bold>DISABLED</bold></red>";
    }
}
