package com.voltic.lobby.hologram;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.util.LocationUtil;
import com.voltic.lobby.util.TextUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modern Paper TextDisplay hologram manager.
 * Thread-safe, region-scheduler compliant, transient entity lifecycle management.
 */
public class HologramManager {

    private final LobbyPlugin plugin;
    public final NamespacedKey holoKey;

    @Getter
    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();

    public HologramManager(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
        this.holoKey = new NamespacedKey(plugin, "hologram_id");
        this.loadHolograms();
    }

    /**
     * Loads holograms from holograms.yml and spawns them.
     */
    public void loadHolograms() {
        removeAllEntities();
        holograms.clear();

        FileConfiguration config = plugin.getConfigManager().getHolograms();
        ConfigurationSection section = config.getConfigurationSection("holograms");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection holoSec = section.getConfigurationSection(id);
            if (holoSec == null) continue;

            Location loc = LocationUtil.readFromSection(holoSec.getConfigurationSection("location"));
            if (loc == null || loc.getWorld() == null) {
                plugin.getLogger().warning("Skipping hologram " + id + ": Invalid location or unloaded world.");
                continue;
            }

            List<String> lines = holoSec.getStringList("lines");
            if (lines.isEmpty()) {
                String single = holoSec.getString("text");
                if (single != null) lines = List.of(single);
            }

            String billboardStr = holoSec.getString("billboard", "CENTER");
            Display.Billboard billboard;
            try {
                billboard = Display.Billboard.valueOf(billboardStr.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                billboard = Display.Billboard.CENTER;
            }

            boolean shadow = holoSec.getBoolean("shadow", true);

            Hologram hologram = Hologram.builder()
                    .id(id)
                    .location(loc)
                    .lines(new ArrayList<>(lines))
                    .billboard(billboard)
                    .shadow(shadow)
                    .build();

            holograms.put(id.toLowerCase(Locale.ROOT), hologram);
            spawnHologramEntity(hologram);
        }
    }

    /**
     * Spawns the TextDisplay entity for the given hologram on the correct region thread.
     *
     * @param hologram Target hologram
     */
    public void spawnHologramEntity(@NotNull Hologram hologram) {
        Location loc = hologram.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        if (!world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            world.getChunkAtAsync(loc).thenAccept(chunk -> {
                Bukkit.getRegionScheduler().execute(plugin, loc, () -> spawnTextDisplay(world, hologram));
            });
            return;
        }

        Bukkit.getRegionScheduler().execute(plugin, loc, () -> spawnTextDisplay(world, hologram));
    }

    private void spawnTextDisplay(@NotNull World world, @NotNull Hologram hologram) {
        Location loc = hologram.getLocation();

        // Remove any existing entity with this ID in the immediate vicinity
        for (Entity entity : world.getNearbyEntities(loc, 3.0, 3.0, 3.0)) {
            if (entity instanceof TextDisplay td) {
                String tag = td.getPersistentDataContainer().get(holoKey, PersistentDataType.STRING);
                if (hologram.getId().equalsIgnoreCase(tag)) {
                    td.remove();
                }
            }
        }

        TextDisplay display = world.spawn(loc, TextDisplay.class, entity -> {
            entity.text(composeText(hologram.getLines()));
            entity.setBillboard(hologram.getBillboard());
            entity.setShadowed(hologram.isShadow());
            entity.setBackgroundColor(Color.fromARGB(60, 0, 0, 0));
            entity.setViewRange(1.0f);
            entity.getPersistentDataContainer().set(holoKey, PersistentDataType.STRING, hologram.getId());
            // Set transient to prevent saving into world chunk files and causing duplicate entity stacking
            entity.setPersistent(false);
        });

        hologram.setEntityUuid(display.getUniqueId());
    }

    /**
     * Handles chunk loading to spawn any holograms present in that chunk.
     *
     * @param chunk Loaded chunk
     */
    public void handleChunkLoad(@NotNull Chunk chunk) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        for (Hologram holo : holograms.values()) {
            Location loc = holo.getLocation();
            if (loc.getWorld() != null && loc.getWorld().equals(world)) {
                if ((loc.getBlockX() >> 4) == chunkX && (loc.getBlockZ() >> 4) == chunkZ) {
                    if (holo.getEntityUuid() == null || Bukkit.getEntity(holo.getEntityUuid()) == null) {
                        spawnTextDisplay(world, holo);
                    }
                }
            }
        }
    }

    /**
     * Composes multi-line text into a single Adventure Component.
     *
     * @param lines Text lines
     * @return Composed Component
     */
    @NotNull
    public Component composeText(@NotNull List<String> lines) {
        if (lines.isEmpty()) {
            return Component.empty();
        }

        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            Component lineComp = TextUtil.parse(lines.get(i));
            result = result.append(lineComp);
            if (i < lines.size() - 1) {
                result = result.append(Component.newline());
            }
        }
        return result;
    }

    /**
     * Updates an existing hologram's lines.
     *
     * @param id    Hologram ID
     * @param lines New lines
     */
    public boolean updateLines(@NotNull String id, @NotNull List<String> lines) {
        Hologram holo = holograms.get(id.toLowerCase(Locale.ROOT));
        if (holo == null) return false;

        holo.setLines(new ArrayList<>(lines));
        saveHologram(holo);

        if (holo.getEntityUuid() != null) {
            Entity entity = Bukkit.getEntity(holo.getEntityUuid());
            if (entity instanceof TextDisplay td) {
                td.text(composeText(lines));
                return true;
            }
        }

        spawnHologramEntity(holo);
        return true;
    }

    /**
     * Creates and saves a new hologram.
     *
     * @param id       Unique ID
     * @param location Spawn location
     * @param lines    Initial text lines
     */
    public void createHologram(@NotNull String id, @NotNull Location location, @NotNull List<String> lines) {
        String key = id.toLowerCase(Locale.ROOT);
        Hologram holo = Hologram.builder()
                .id(key)
                .location(location)
                .lines(new ArrayList<>(lines))
                .billboard(Display.Billboard.CENTER)
                .shadow(true)
                .build();

        holograms.put(key, holo);
        saveHologram(holo);
        spawnHologramEntity(holo);
    }

    /**
     * Deletes a hologram and removes its entity.
     *
     * @param id Hologram ID
     * @return true if removed
     */
    public boolean deleteHologram(@NotNull String id) {
        String key = id.toLowerCase(Locale.ROOT);
        Hologram holo = holograms.remove(key);
        if (holo == null) return false;

        if (holo.getEntityUuid() != null) {
            Entity entity = Bukkit.getEntity(holo.getEntityUuid());
            if (entity != null) {
                entity.remove();
            }
        }

        FileConfiguration config = plugin.getConfigManager().getHolograms();
        config.set("holograms." + key, null);
        plugin.getConfigManager().getHologramsConfig().saveAsync();
        return true;
    }

    /**
     * Saves a hologram to holograms.yml.
     *
     * @param hologram Target hologram
     */
    public void saveHologram(@NotNull Hologram hologram) {
        FileConfiguration config = plugin.getConfigManager().getHolograms();
        String path = "holograms." + hologram.getId();

        config.set(path + ".lines", hologram.getLines());
        config.set(path + ".billboard", hologram.getBillboard().name());
        config.set(path + ".shadow", hologram.isShadow());

        ConfigurationSection locSec = config.createSection(path + ".location");
        LocationUtil.writeToSection(locSec, hologram.getLocation());

        plugin.getConfigManager().getHologramsConfig().saveAsync();
    }

    /**
     * Removes all hologram entities from all worlds efficiently without querying non-TextDisplay entities.
     */
    public void removeAllEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (TextDisplay td : world.getEntitiesByClass(TextDisplay.class)) {
                if (td.getPersistentDataContainer().has(holoKey, PersistentDataType.STRING)) {
                    td.remove();
                }
            }
        }
    }

    @Nullable
    public Hologram getHologram(@NotNull String id) {
        return holograms.get(id.toLowerCase(Locale.ROOT));
    }
}
