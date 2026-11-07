package com.voltic.lobby.hotbar;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.gui.AdminConfigGui;
import com.voltic.lobby.util.TextUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for interactions and enforces inventory protections for lobby hotbar items.
 */
public class HotbarListener implements Listener {

    private final LobbyPlugin plugin;

    public HotbarListener(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) {
            return;
        }

        String action = item.getItemMeta().getPersistentDataContainer()
                .get(plugin.getHotbarManager().getHotbarKey(), PersistentDataType.STRING);
        if (action == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        switch (action) {
            case "SELECTOR" -> {
                player.performCommand("lobby");
            }
            case "VISIBILITY" -> {
                plugin.getHotbarManager().toggleVisibility(player);
            }
            case "ADMIN" -> {
                if (player.hasPermission("lobby.admin.gui")) {
                    AdminConfigGui.open(player, plugin);
                } else {
                    player.performCommand("lobby");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getHotbarManager().isLocked()) {
            return;
        }

        if (event.getWhoClicked().hasPermission("lobby.admin.build")) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        if (current != null && current.getItemMeta() != null) {
            if (current.getItemMeta().getPersistentDataContainer().has(plugin.getHotbarManager().getHotbarKey(), PersistentDataType.STRING)) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getItemMeta() != null) {
            if (cursor.getItemMeta().getPersistentDataContainer().has(plugin.getHotbarManager().getHotbarKey(), PersistentDataType.STRING)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!plugin.getHotbarManager().isLocked()) {
            return;
        }

        if (event.getPlayer().hasPermission("lobby.admin.build")) {
            return;
        }

        ItemStack dropped = event.getItemDrop().getItemStack();
        if (dropped.getItemMeta() != null && dropped.getItemMeta().getPersistentDataContainer().has(plugin.getHotbarManager().getHotbarKey(), PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        if (!plugin.getHotbarManager().isLocked()) {
            return;
        }

        if (event.getPlayer().hasPermission("lobby.admin.build")) {
            return;
        }

        ItemStack mainHand = event.getMainHandItem();
        if (mainHand != null && mainHand.getItemMeta() != null && mainHand.getItemMeta().getPersistentDataContainer().has(plugin.getHotbarManager().getHotbarKey(), PersistentDataType.STRING)) {
            event.setCancelled(true);
            return;
        }

        ItemStack offHand = event.getOffHandItem();
        if (offHand != null && offHand.getItemMeta() != null && offHand.getItemMeta().getPersistentDataContainer().has(plugin.getHotbarManager().getHotbarKey(), PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }
}
