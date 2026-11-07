package com.voltic.lobby.gui;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.gui.anvil.AnvilInputGui;
import com.voltic.lobby.util.TextUtil;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Listens for interactions in the Admin Configuration GUI and applies live hot-reloads.
 */
public class GuiListener implements Listener {

    private final LobbyPlugin plugin;

    public GuiListener(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Verify custom holder
        if (event.getInventory().getHolder() instanceof AdminGuiHolder) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getItemMeta() == null) {
                return;
            }

            String action = clicked.getItemMeta().getPersistentDataContainer()
                    .get(AdminConfigGui.ACTION_KEY, PersistentDataType.STRING);
            if (action == null) {
                return;
            }

            Inventory topInv = event.getView().getTopInventory();
            ClickType clickType = event.getClick();
            FileConfiguration main = plugin.getConfigManager().getMain();
            FileConfiguration chat = plugin.getConfigManager().getChat();
            FileConfiguration motd = plugin.getConfigManager().getMotd();
            FileConfiguration tab = plugin.getConfigManager().getTab();
            FileConfiguration sb = plugin.getConfigManager().getScoreboard();

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);

            switch (action) {
                case "TOGGLE_PVP" -> {
                    boolean current = main.getBoolean("protection.pvp-enabled", false);
                    main.set("protection.pvp-enabled", !current);
                    plugin.getConfigManager().getMainConfig().saveAsync();
                }
                case "TOGGLE_BUILD" -> {
                    if (clickType.isRightClick()) {
                        boolean place = main.getBoolean("protection.block-place", false);
                        main.set("protection.block-place", !place);
                    } else {
                        boolean breakBlock = main.getBoolean("protection.block-break", false);
                        main.set("protection.block-break", !breakBlock);
                    }
                    plugin.getConfigManager().getMainConfig().saveAsync();
                }
                case "TOGGLE_HUNGER" -> {
                    boolean hunger = main.getBoolean("protection.hunger-depletion", false);
                    main.set("protection.hunger-depletion", !hunger);
                    plugin.getConfigManager().getMainConfig().saveAsync();
                }
                case "TOGGLE_WEATHER" -> {
                    boolean weather = main.getBoolean("protection.weather-lock", true);
                    main.set("protection.weather-lock", !weather);
                    plugin.getConfigManager().getMainConfig().saveAsync();
                }
                case "TOGGLE_CHAT" -> {
                    if (clickType.isShiftClick()) {
                        String currentFormat = chat.getString("chat.format", "");
                        AnvilInputGui.open(plugin, player, "Edit Chat Format", currentFormat, newFormat -> {
                            chat.set("chat.format", newFormat);
                            plugin.getConfigManager().getChatConfig().saveAsync();
                            plugin.getChatManager().loadConfig();
                            player.sendMessage(TextUtil.parse("<green>Chat format updated successfully!</green>"));
                            AdminConfigGui.open(player, plugin);
                        }, () -> AdminConfigGui.open(player, plugin));
                        return;
                    } else if (clickType.isRightClick()) {
                        boolean spam = chat.getBoolean("anti-spam.enabled", true);
                        chat.set("anti-spam.enabled", !spam);
                    } else {
                        boolean swear = chat.getBoolean("anti-swear.enabled", true);
                        chat.set("anti-swear.enabled", !swear);
                    }
                    plugin.getConfigManager().getChatConfig().saveAsync();
                    plugin.getChatManager().loadConfig();
                }
                case "TOGGLE_MOTD" -> {
                    if (clickType.isShiftClick()) {
                        boolean m = motd.getBoolean("motd.enabled", true);
                        motd.set("motd.enabled", !m);
                        plugin.getConfigManager().getMotdConfig().saveAsync();
                    } else if (clickType.isRightClick()) {
                        String currentLine2 = motd.getString("motd.line2", "");
                        AnvilInputGui.open(plugin, player, "Edit MOTD Line 2", currentLine2, newLine2 -> {
                            motd.set("motd.line2", newLine2);
                            plugin.getConfigManager().getMotdConfig().saveAsync();
                            player.sendMessage(TextUtil.parse("<green>MOTD Line 2 updated successfully!</green>"));
                            AdminConfigGui.open(player, plugin);
                        }, () -> AdminConfigGui.open(player, plugin));
                        return;
                    } else {
                        String currentLine1 = motd.getString("motd.line1", "");
                        AnvilInputGui.open(plugin, player, "Edit MOTD Line 1", currentLine1, newLine1 -> {
                            motd.set("motd.line1", newLine1);
                            plugin.getConfigManager().getMotdConfig().saveAsync();
                            player.sendMessage(TextUtil.parse("<green>MOTD Line 1 updated successfully!</green>"));
                            AdminConfigGui.open(player, plugin);
                        }, () -> AdminConfigGui.open(player, plugin));
                        return;
                    }
                }
                case "TOGGLE_SCOREBOARD" -> {
                    if (clickType.isShiftClick()) {
                        String currentTitle = sb.getString("scoreboard.title", "");
                        AnvilInputGui.open(plugin, player, "Edit Scoreboard Title", currentTitle, newTitle -> {
                            sb.set("scoreboard.title", newTitle);
                            plugin.getConfigManager().getScoreboardConfig().saveAsync();
                            plugin.getScoreboardManager().loadConfig();
                            plugin.getScoreboardManager().updateAll();
                            player.sendMessage(TextUtil.parse("<green>Scoreboard title updated successfully!</green>"));
                            AdminConfigGui.open(player, plugin);
                        }, () -> AdminConfigGui.open(player, plugin));
                        return;
                    } else if (clickType.isRightClick()) {
                        long current = sb.getLong("scoreboard.update-interval-ticks", 20L);
                        long next = (current == 10L) ? 20L : (current == 20L ? 40L : 10L);
                        sb.set("scoreboard.update-interval-ticks", next);
                    } else {
                        boolean enabled = sb.getBoolean("scoreboard.enabled", true);
                        sb.set("scoreboard.enabled", !enabled);
                    }
                    plugin.getConfigManager().getScoreboardConfig().saveAsync();
                    plugin.getScoreboardManager().loadConfig();
                    plugin.getScoreboardManager().startTask();
                    plugin.getScoreboardManager().updateAll();
                }
                case "TOGGLE_TAB" -> {
                    boolean enabled = tab.getBoolean("tab.enabled", true);
                    tab.set("tab.enabled", !enabled);
                    plugin.getConfigManager().getTabConfig().saveAsync();
                    plugin.getTabManager().loadConfig();
                    plugin.getTabManager().startTask();
                    plugin.getTabManager().updateAllPlayers();
                }
                case "TOGGLE_VOID" -> {
                    boolean voidFall = main.getBoolean("protection.void-fall.enabled", true);
                    main.set("protection.void-fall.enabled", !voidFall);
                    plugin.getConfigManager().getMainConfig().saveAsync();
                }
                case "RELOAD_ALL" -> {
                    plugin.reloadPlugin();
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
                    player.sendMessage(TextUtil.parse("<green><bold>All configurations and managers hot-reloaded successfully!</bold></green>"));
                }
                case "CLOSE" -> player.closeInventory();
            }

            if (!"CLOSE".equals(action)) {
                AdminConfigGui.refresh(topInv, plugin);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof AdminGuiHolder) {
            event.setCancelled(true);
        }
    }
}
