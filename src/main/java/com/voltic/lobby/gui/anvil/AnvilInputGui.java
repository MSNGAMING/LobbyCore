package com.voltic.lobby.gui.anvil;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.util.ItemBuilder;
import com.voltic.lobby.util.TextUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.view.AnvilView;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Modern Paper Anvil GUI using MenuType and AnvilView without NMS or deprecated APIs.
 */
public class AnvilInputGui {

    private static final Map<UUID, AnvilSession> SESSIONS = new ConcurrentHashMap<>();

    @Getter
    public static class AnvilSession {
        private final Player player;
        private final String title;
        private final String initialText;
        private final Consumer<String> onComplete;
        private final Runnable onCancel;

        public AnvilSession(Player player, String title, String initialText, Consumer<String> onComplete, Runnable onCancel) {
            this.player = player;
            this.title = title;
            this.initialText = initialText != null ? initialText : "";
            this.onComplete = onComplete;
            this.onCancel = onCancel;
        }
    }

    /**
     * Opens a virtual Anvil GUI for the player to input or edit text.
     *
     * @param plugin      Plugin instance
     * @param player      Target player
     * @param title       Title or prompt description
     * @param initialText Initial string displayed in the rename field
     * @param onComplete  Callback invoked when the player confirms their text
     * @param onCancel    Callback invoked if the player closes the anvil without saving
     */
    @SuppressWarnings("deprecation")
    public static void open(@NotNull LobbyPlugin plugin, @NotNull Player player, @NotNull String title,
                            @NotNull String initialText, @NotNull Consumer<String> onComplete, @NotNull Runnable onCancel) {
        AnvilSession session = new AnvilSession(player, title, initialText, onComplete, onCancel);
        SESSIONS.put(player.getUniqueId(), session);

        // Build virtual anvil view via modern Paper MenuType API
        AnvilView view;
        try {
            view = MenuType.ANVIL.builder()
                    .title(TextUtil.parse(title))
                    .checkReachable(false)
                    .build(player);
        } catch (Throwable fallback) {
            // Fallback for older Paper builds
            view = (AnvilView) player.openAnvil(null, true);
        }

        if (view == null) {
            SESSIONS.remove(player.getUniqueId());
            player.sendMessage(TextUtil.parse("<red>Failed to open Anvil editor.</red>"));
            return;
        }

        player.openInventory(view);
        AnvilInventory anvilInv = view.getTopInventory();

        // Set prompt item in first slot
        ItemStack inputItem = ItemBuilder.from(Material.PAPER)
                .name(Component.text(initialText))
                .loreStrings(
                        "<gray>Prompt: <yellow>" + title + "</yellow></gray>",
                        "<gray>Edit the text in the box above,</gray>",
                        "<gray>then click the output slot on the right to save!</gray>"
                )
                .build();

        anvilInv.setItem(0, inputItem);
    }

    public static AnvilSession getSession(@NotNull UUID uuid) {
        return SESSIONS.get(uuid);
    }

    public static AnvilSession removeSession(@NotNull UUID uuid) {
        return SESSIONS.remove(uuid);
    }
}
