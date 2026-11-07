package com.voltic.lobby.lobby;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Data model for a configured lobby instance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LobbyData {

    @NotNull
    private String id;

    @NotNull
    private String displayName;

    @NotNull
    private Location spawnLocation;

    private int maxPlayers;

    @Nullable
    private String permission;

    private boolean enabled;

    /**
     * Checks if a player has permission to join this lobby.
     *
     * @param hasPermFunction Permission check predicate
     * @return true if no permission required or check passes
     */
    public boolean canAccess(@NotNull java.util.function.Predicate<String> hasPermFunction) {
        if (!enabled) {
            return false;
        }
        if (permission == null || permission.trim().isEmpty()) {
            return true;
        }
        return hasPermFunction.test(permission);
    }
}
