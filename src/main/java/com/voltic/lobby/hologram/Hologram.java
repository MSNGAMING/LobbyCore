package com.voltic.lobby.hologram;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Data model for a TextDisplay-driven hologram.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hologram {

    @NotNull
    private String id;

    @NotNull
    private Location location;

    @NotNull
    private List<String> lines;

    @Builder.Default
    private Display.Billboard billboard = Display.Billboard.CENTER;

    @Builder.Default
    private boolean shadow = true;

    @Nullable
    private UUID entityUuid;
}
