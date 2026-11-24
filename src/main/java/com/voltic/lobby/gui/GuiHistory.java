package com.voltic.lobby.gui;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks multi-level GUI navigation history per player to support 'Esc' key redirection.
 */
public final class GuiHistory {

    private static final Map<UUID, Deque<Runnable>> HISTORY = new ConcurrentHashMap<>();
    private static final Set<UUID> EXPLICIT_CLOSE = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> TRANSITIONING = ConcurrentHashMap.newKeySet();

    private GuiHistory() {}

    /**
     * Prepares a forward navigation transition to a child GUI, saving the previous GUI opener.
     *
     * @param uuid        Player UUID
     * @param previousGui Action to reopen the parent GUI
     */
    public static void transition(@NotNull UUID uuid, @NotNull Runnable previousGui) {
        HISTORY.computeIfAbsent(uuid, k -> new ArrayDeque<>()).push(previousGui);
        TRANSITIONING.add(uuid);
    }

    /**
     * Marks that a GUI switch is happening so the immediate close of the parent GUI is ignored.
     *
     * @param uuid Player UUID
     */
    public static void markTransition(@NotNull UUID uuid) {
        TRANSITIONING.add(uuid);
    }

    /**
     * Checks and consumes the transitioning flag.
     *
     * @param uuid Player UUID
     * @return true if player is switching between menus
     */
    public static boolean checkAndConsumeTransition(@NotNull UUID uuid) {
        return TRANSITIONING.remove(uuid);
    }

    /**
     * Retrieves and removes the most recent parent GUI on the player's navigation stack.
     *
     * @param uuid Player UUID
     * @return Reopen action or null if at root
     */
    @Nullable
    public static Runnable pop(@NotNull UUID uuid) {
        Deque<Runnable> stack = HISTORY.get(uuid);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Runnable prev = stack.pop();
        if (stack.isEmpty()) {
            HISTORY.remove(uuid);
        }
        return prev;
    }

    /**
     * Marks that the player intentionally closed the GUI (e.g. by clicking Close or teleporting)
     * so that the Esc redirect does not reopen the previous GUI.
     *
     * @param uuid Player UUID
     */
    public static void markExplicitClose(@NotNull UUID uuid) {
        EXPLICIT_CLOSE.add(uuid);
    }

    /**
     * Checks and consumes the explicit close flag.
     *
     * @param uuid Player UUID
     * @return true if close was explicit
     */
    public static boolean checkAndConsumeExplicitClose(@NotNull UUID uuid) {
        return EXPLICIT_CLOSE.remove(uuid);
    }

    /**
     * Clears all navigation history for a player.
     *
     * @param uuid Player UUID
     */
    public static void clear(@NotNull UUID uuid) {
        HISTORY.remove(uuid);
        EXPLICIT_CLOSE.remove(uuid);
        TRANSITIONING.remove(uuid);
    }
}
