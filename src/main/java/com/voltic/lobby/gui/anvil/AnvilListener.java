package com.voltic.lobby.gui.anvil;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for Anvil GUI events using modern Paper AnvilView API to capture and confirm admin text inputs.
 */
public class AnvilListener implements Listener {

    private final LobbyPlugin plugin;

    public AnvilListener(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }

        AnvilInputGui.AnvilSession session = AnvilInputGui.getSession(player.getUniqueId());
        if (session == null) {
            return;
        }

        String rename = null;
        if (event.getView() instanceof AnvilView anvilView) {
            rename = anvilView.getRenameText();
        }

        if (rename == null || rename.trim().isEmpty()) {
            rename = session.getInitialText();
        }

        ItemStack resultItem = ItemBuilder.from(Material.NAME_TAG)
                .name(Component.text(rename))
                .loreStrings(
                        "<green><bold>✔ CLICK TO SAVE</bold></green>",
                        "",
                        "<gray>Apply modified text live to configuration.</gray>"
                )
                .build();

        event.setResult(resultItem);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        AnvilInputGui.AnvilSession session = AnvilInputGui.getSession(player.getUniqueId());
        if (session == null) {
            return;
        }

        if (event.getView() instanceof AnvilView anvilView) {
            int rawSlot = event.getRawSlot();

            // Result slot clicked
            if (rawSlot == 2) {
                event.setCancelled(true);

                String rename = anvilView.getRenameText();
                if (rename == null || rename.trim().isEmpty()) {
                    rename = session.getInitialText();
                }

                AnvilInputGui.removeSession(player.getUniqueId());

                final String finalInput = rename;
                player.getScheduler().run(plugin, task -> {
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                    session.getOnComplete().accept(finalInput);
                }, null);
                return;
            }

            // Lock input slots
            if (rawSlot == 0 || rawSlot == 1) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        AnvilInputGui.AnvilSession session = AnvilInputGui.removeSession(player.getUniqueId());
        if (session != null) {
            player.getScheduler().run(plugin, task -> session.getOnCancel().run(), null);
        }
    }
}
