package com.voltic.lobby.npc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Data model for a lobby NPC.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NpcData {

    @NotNull
    private String id;

    @NotNull
    private EntityType entityType;

    @NotNull
    private String displayName;

    @NotNull
    private Location location;

    @NotNull
    private ActionType actionType;

    @NotNull
    private String actionValue;

    @Nullable
    private UUID visualEntityUuid;

    @Nullable
    private UUID hitboxEntityUuid;

    public enum ActionType {
        COMMAND,
        TELEPORT,
        MESSAGE,
        GUI
    }
}
