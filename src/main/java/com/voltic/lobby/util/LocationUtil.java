package com.voltic.lobby.util;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Utility for safe serializing and deserializing of Bukkit Locations.
 */
@UtilityClass
public class LocationUtil {

    /**
     * Serializes a Location to a single readable string format: "world,x,y,z,yaw,pitch".
     *
     * @param loc Bukkit Location
     * @return Formatted string
     */
    @NotNull
    public static String toSimpleString(@NotNull Location loc) {
        World world = loc.getWorld();
        String worldName = world != null ? world.getName() : "world";
        return String.format(Locale.ROOT, "%s,%.3f,%.3f,%.3f,%.2f,%.2f",
                worldName, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    /**
     * Deserializes a Location from a single string format: "world,x,y,z,yaw,pitch".
     *
     * @param str Serialized location string
     * @return Bukkit Location or null if world or format is invalid
     */
    @Nullable
    public static Location fromSimpleString(@Nullable String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }

        String[] parts = str.split(",");
        if (parts.length < 4) {
            return null;
        }

        try {
            World world = Bukkit.getWorld(parts[0].trim());
            if (world == null) {
                return null;
            }

            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0.0f;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5].trim()) : 0.0f;

            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Writes a Location to a Yaml ConfigurationSection.
     *
     * @param section Target section
     * @param loc     Location to write
     */
    public static void writeToSection(@NotNull ConfigurationSection section, @NotNull Location loc) {
        World world = loc.getWorld();
        section.set("world", world != null ? world.getName() : "world");
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        section.set("yaw", (double) loc.getYaw());
        section.set("pitch", (double) loc.getPitch());
    }

    /**
     * Reads a Location from a Yaml ConfigurationSection.
     *
     * @param section Configuration section containing world, x, y, z, yaw, pitch
     * @return Deserialized Location or null
     */
    @Nullable
    public static Location readFromSection(@Nullable ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world");
        if (worldName == null) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0.0);
        float pitch = (float) section.getDouble("pitch", 0.0);

        return new Location(world, x, y, z, yaw, pitch);
    }
}
