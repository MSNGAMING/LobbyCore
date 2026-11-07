package com.voltic.lobby.npc;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.gui.AdminConfigGui;
import com.voltic.lobby.util.LocationUtil;
import com.voltic.lobby.util.TextUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modern Paper Entity API-driven NPC manager using Interaction hitboxes and visual entities.
 * Thread-safe, RegionScheduler compliant, and transient entity lifecycle management.
 */
public class NpcManager {

    private final LobbyPlugin plugin;

    @Getter
    private final NamespacedKey npcKey;

    @Getter
    private final Map<String, NpcData> npcs = new ConcurrentHashMap<>();

    public NpcManager(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
        this.npcKey = new NamespacedKey(plugin, "npc_id");
        this.loadNpcs();
    }

    /**
     * Loads NPCs from npcs.yml.
     */
    public void loadNpcs() {
        removeAllEntities();
        npcs.clear();

        FileConfiguration config = plugin.getConfigManager().getNpcs();
        ConfigurationSection section = config.getConfigurationSection("npcs");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection npcSec = section.getConfigurationSection(id);
            if (npcSec == null) continue;

            String typeStr = npcSec.getString("type", "VILLAGER");
            EntityType type;
            try {
                type = EntityType.valueOf(typeStr.toUpperCase(Locale.ROOT));
                if (type == EntityType.PLAYER || !type.isSpawnable() || type.getEntityClass() == null) {
                    type = EntityType.VILLAGER;
                }
            } catch (IllegalArgumentException e) {
                type = EntityType.VILLAGER;
            }

            String name = npcSec.getString("name", "<gold>NPC " + id + "</gold>");
            Location loc = LocationUtil.readFromSection(npcSec.getConfigurationSection("location"));
            if (loc == null || loc.getWorld() == null) {
                plugin.getLogger().warning("Invalid location for NPC: " + id);
                continue;
            }

            String actionTypeStr = npcSec.getString("action.type", "COMMAND");
            NpcData.ActionType actionType;
            try {
                actionType = NpcData.ActionType.valueOf(actionTypeStr.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                actionType = NpcData.ActionType.COMMAND;
            }

            String actionValue = npcSec.getString("action.value", "say Hello %player%!");

            NpcData npcData = NpcData.builder()
                    .id(id)
                    .entityType(type)
                    .displayName(name)
                    .location(loc)
                    .actionType(actionType)
                    .actionValue(actionValue)
                    .build();

            npcs.put(id.toLowerCase(Locale.ROOT), npcData);
            spawnNpc(npcData);
        }
    }

    /**
     * Spawns both the visual entity and interaction hitbox for the NPC on the region scheduler.
     *
     * @param npc NpcData
     */
    public void spawnNpc(@NotNull NpcData npc) {
        Location loc = npc.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        if (!world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            world.getChunkAtAsync(loc).thenAccept(chunk -> {
                Bukkit.getRegionScheduler().execute(plugin, loc, () -> spawnEntities(world, npc));
            });
            return;
        }

        Bukkit.getRegionScheduler().execute(plugin, loc, () -> spawnEntities(world, npc));
    }

    private void spawnEntities(@NotNull World world, @NotNull NpcData npc) {
        Location loc = npc.getLocation();

        // Remove previous entities matching this NPC tag
        for (Entity nearby : world.getNearbyEntities(loc, 3.0, 3.0, 3.0)) {
            String tag = nearby.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
            if (npc.getId().equalsIgnoreCase(tag)) {
                nearby.remove();
            }
        }

        // Spawn visual entity
        Class<? extends Entity> entityClass = npc.getEntityType().getEntityClass();
        if (entityClass == null || !Entity.class.isAssignableFrom(entityClass)) {
            entityClass = org.bukkit.entity.Villager.class;
        }

        @SuppressWarnings("unchecked")
        Class<Entity> spawnClass = (Class<Entity>) entityClass;

        Entity visual = world.spawn(loc, spawnClass, entity -> {
            entity.customName(TextUtil.parse(npc.getDisplayName()));
            entity.setCustomNameVisible(true);
            entity.setInvulnerable(true);
            entity.setSilent(true);
            entity.setGravity(false);
            entity.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, npc.getId());
            entity.setPersistent(false);

            if (entity instanceof LivingEntity living) {
                living.setAI(false);
                living.setCollidable(false);
                living.setRemoveWhenFarAway(false);
            }
        });

        // Spawn interaction hitbox for crisp click handling
        Interaction interaction = world.spawn(loc, Interaction.class, hitbox -> {
            hitbox.setInteractionWidth(1.2f);
            hitbox.setInteractionHeight(2.0f);
            hitbox.setResponsive(true);
            hitbox.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, npc.getId());
            hitbox.setPersistent(false);
        });

        npc.setVisualEntityUuid(visual.getUniqueId());
        npc.setHitboxEntityUuid(interaction.getUniqueId());
    }

    /**
     * Handles chunk loading to spawn any NPCs present in that chunk.
     *
     * @param chunk Loaded chunk
     */
    public void handleChunkLoad(@NotNull Chunk chunk) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        for (NpcData npc : npcs.values()) {
            Location loc = npc.getLocation();
            if (loc.getWorld() != null && loc.getWorld().equals(world)) {
                if ((loc.getBlockX() >> 4) == chunkX && (loc.getBlockZ() >> 4) == chunkZ) {
                    if (npc.getVisualEntityUuid() == null || Bukkit.getEntity(npc.getVisualEntityUuid()) == null) {
                        spawnEntities(world, npc);
                    }
                }
            }
        }
    }

    /**
     * Handles player interaction with the NPC in a thread-safe scheduler context.
     *
     * @param player Interacting player
     * @param npc    Triggered NPC
     */
    public void handleInteraction(@NotNull Player player, @NotNull NpcData npc) {
        player.getScheduler().run(plugin, task -> {
            switch (npc.getActionType()) {
                case COMMAND -> {
                    String cmd = TextUtil.replacePlaceholders(player, npc.getActionValue());
                    if (cmd.startsWith("[console] ")) {
                        String consoleCmd = cmd.substring("[console] ".length()).trim();
                        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), consoleCmd));
                    } else if (cmd.startsWith("[player] ")) {
                        String playerCmd = cmd.substring("[player] ".length()).trim();
                        player.performCommand(playerCmd);
                    } else {
                        player.performCommand(cmd.startsWith("/") ? cmd.substring(1) : cmd);
                    }
                }
                case TELEPORT -> plugin.getLobbyManager().teleportToLobby(player, npc.getActionValue());
                case MESSAGE -> player.sendMessage(TextUtil.parsePlayer(player, npc.getActionValue()));
                case GUI -> {
                    if (player.hasPermission("lobby.admin.gui")) {
                        AdminConfigGui.open(player, plugin);
                    } else {
                        player.sendMessage(TextUtil.parse("<red>You do not have permission to open this GUI!</red>"));
                    }
                }
            }
        }, null);
    }

    /**
     * Creates and saves a new NPC.
     *
     * @param id          Unique NPC ID
     * @param entityType  Entity type
     * @param displayName Display name
     * @param location    Spawn location
     * @param actionType  Action type
     * @param actionValue Action payload
     */
    public void createNpc(@NotNull String id, @NotNull EntityType entityType, @NotNull String displayName,
                          @NotNull Location location, @NotNull NpcData.ActionType actionType, @NotNull String actionValue) {
        String key = id.toLowerCase(Locale.ROOT);
        if (entityType == EntityType.PLAYER || !entityType.isSpawnable() || entityType.getEntityClass() == null) {
            entityType = EntityType.VILLAGER;
        }

        NpcData npc = NpcData.builder()
                .id(key)
                .entityType(entityType)
                .displayName(displayName)
                .location(location)
                .actionType(actionType)
                .actionValue(actionValue)
                .build();

        npcs.put(key, npc);
        saveNpc(npc);
        spawnNpc(npc);
    }

    /**
     * Saves an NPC to npcs.yml.
     *
     * @param npc Target NPC
     */
    public void saveNpc(@NotNull NpcData npc) {
        FileConfiguration config = plugin.getConfigManager().getNpcs();
        String path = "npcs." + npc.getId();

        config.set(path + ".type", npc.getEntityType().name());
        config.set(path + ".name", npc.getDisplayName());
        config.set(path + ".action.type", npc.getActionType().name());
        config.set(path + ".action.value", npc.getActionValue());

        ConfigurationSection locSec = config.createSection(path + ".location");
        LocationUtil.writeToSection(locSec, npc.getLocation());

        plugin.getConfigManager().getNpcsConfig().saveAsync();
    }

    /**
     * Deletes an NPC by ID.
     *
     * @param id NPC ID
     * @return true if deleted
     */
    public boolean deleteNpc(@NotNull String id) {
        String key = id.toLowerCase(Locale.ROOT);
        NpcData npc = npcs.remove(key);
        if (npc == null) return false;

        if (npc.getVisualEntityUuid() != null) {
            Entity ent = Bukkit.getEntity(npc.getVisualEntityUuid());
            if (ent != null) ent.remove();
        }
        if (npc.getHitboxEntityUuid() != null) {
            Entity ent = Bukkit.getEntity(npc.getHitboxEntityUuid());
            if (ent != null) ent.remove();
        }

        FileConfiguration config = plugin.getConfigManager().getNpcs();
        config.set("npcs." + key, null);
        plugin.getConfigManager().getNpcsConfig().saveAsync();
        return true;
    }

    /**
     * Removes all spawned NPC entities from all worlds.
     */
    public void removeAllEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(Interaction.class)) {
                if (entity.getPersistentDataContainer().has(npcKey, PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
            for (Entity entity : world.getEntitiesByClass(LivingEntity.class)) {
                if (entity.getPersistentDataContainer().has(npcKey, PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
        }
    }

    @Nullable
    public NpcData getNpc(@NotNull String id) {
        return npcs.get(id.toLowerCase(Locale.ROOT));
    }
}
