package com.voltic.lobby.hotbar;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.gui.AdminConfigGui;
import com.voltic.lobby.util.ItemBuilder;
import com.voltic.lobby.util.TextUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages lobby hotbar items given to players upon joining (Selector, Player Visibility Toggle, Admin Control).
 */
public class HotbarManager {

    private final LobbyPlugin plugin;

    @Getter
    private final NamespacedKey hotbarKey;

    @Getter
    private final Set<UUID> hiddenPlayers = ConcurrentHashMap.newKeySet();

    public HotbarManager(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
        this.hotbarKey = new NamespacedKey(plugin, "hotbar_action");
    }

    /**
     * Checks if hotbar items are enabled in config.yml.
     */
    public boolean isEnabled() {
        return plugin.getConfigManager().getMain().getBoolean("hotbar.enabled", true);
    }

    /**
     * Checks if hotbar items should be locked against inventory moves and drops.
     */
    public boolean isLocked() {
        return plugin.getConfigManager().getMain().getBoolean("hotbar.lock-slots", true);
    }

    /**
     * Gives the default lobby hotbar items to the player.
     *
     * @param player Target player
     */
    public void giveHotbarItems(@NotNull Player player) {
        if (!isEnabled()) {
            return;
        }

        // Slot 0: Game Selector (Compass)
        ItemStack selector = ItemBuilder.from(Material.COMPASS)
                .name("<gradient:#00f2fe:#4facfe><bold>Game Selector</bold></gradient> <gray>(Right Click)</gray>")
                .loreStrings(
                        "<gray>Explore all active game modes,</gray>",
                        "<gray>minigames, and worlds!</gray>",
                        "",
                        "<yellow>▶ Right-Click to open</yellow>"
                )
                .tagString(hotbarKey, "SELECTOR")
                .build();
        player.getInventory().setItem(0, selector);

        // Slot 4: Player Visibility Toggle (Lime/Gray Dye)
        boolean isHidden = hiddenPlayers.contains(player.getUniqueId());
        player.getInventory().setItem(4, buildVisibilityItem(isHidden));

        // Slot 8: Control Panel / Lobby Switcher (Nether Star)
        ItemStack control = ItemBuilder.from(Material.NETHER_STAR)
                .name("<gold><bold>Control Panel</bold></gold> <gray>(Right Click)</gray>")
                .loreStrings(
                        "<gray>Quick access to lobby settings,</gray>",
                        "<gray>hubs, and administrative controls.</gray>",
                        "",
                        "<yellow>▶ Right-Click to open</yellow>"
                )
                .glow(true)
                .tagString(hotbarKey, "ADMIN")
                .build();
        player.getInventory().setItem(8, control);
    }

    /**
     * Builds the visibility dye item according to the player's current hide state.
     *
     * @param isHidden true if players are currently hidden
     * @return Formatted ItemStack
     */
    @NotNull
    public ItemStack buildVisibilityItem(boolean isHidden) {
        if (isHidden) {
            return ItemBuilder.from(Material.GRAY_DYE)
                    .name("<gray><bold>Players: HIDDEN</bold></gray> <dark_gray>(Right Click)</dark_gray>")
                    .loreStrings(
                        "<gray>Other players are currently hidden.</gray>",
                        "",
                        "<green>▶ Right-Click to show players</green>"
                    )
                    .tagString(hotbarKey, "VISIBILITY")
                    .build();
        } else {
            return ItemBuilder.from(Material.LIME_DYE)
                    .name("<green><bold>Players: VISIBLE</bold></green> <gray>(Right Click)</gray>")
                    .loreStrings(
                        "<gray>Other players are currently visible.</gray>",
                        "",
                        "<yellow>▶ Right-Click to hide players</yellow>"
                    )
                    .tagString(hotbarKey, "VISIBILITY")
                    .build();
        }
    }

    /**
     * Toggles player visibility for the given player.
     *
     * @param player Target player
     */
    public void toggleVisibility(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        boolean currentlyHidden = hiddenPlayers.contains(uuid);

        if (currentlyHidden) {
            // Unhide players
            hiddenPlayers.remove(uuid);
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.equals(player)) {
                    player.showPlayer(plugin, target);
                }
            }
            player.getInventory().setItem(4, buildVisibilityItem(false));
            player.sendMessage(TextUtil.parse("<green>All players are now <bold>visible</bold>!</green>"));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f);
        } else {
            // Hide players
            hiddenPlayers.add(uuid);
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.equals(player) && !target.hasPermission("lobby.admin.bypasshide")) {
                    player.hidePlayer(plugin, target);
                }
            }
            player.getInventory().setItem(4, buildVisibilityItem(true));
            player.sendMessage(TextUtil.parse("<yellow>All players are now <bold>hidden</bold>!</yellow>"));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 0.8f);
        }
    }

    /**
     * Ensures newly joined players are hidden from players with hiding active.
     *
     * @param joined Newly joined player
     */
    public void handlePlayerJoinVisibility(@NotNull Player joined) {
        for (UUID uuid : hiddenPlayers) {
            Player observer = Bukkit.getPlayer(uuid);
            if (observer != null && observer.isOnline() && !joined.hasPermission("lobby.admin.bypasshide")) {
                observer.hidePlayer(plugin, joined);
            }
        }
    }

    /**
     * Cleans up tracking data when a player disconnects.
     *
     * @param uuid Player UUID
     */
    public void cleanupPlayer(@NotNull UUID uuid) {
        hiddenPlayers.remove(uuid);
    }
}
