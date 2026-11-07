package com.voltic.lobby.listener;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.lobby.LobbyData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance per-world game rules and protections.
 * Employs zero-allocation caching and fast-path movement checks.
 */
public class WorldProtectionListener implements Listener {

    private final LobbyPlugin plugin;
    private final Map<UUID, Long> voidRescueCooldown = new ConcurrentHashMap<>();
    private final Set<String> protectedWorldNames = ConcurrentHashMap.newKeySet();

    private volatile boolean allWorldsProtected;
    private volatile boolean voidFallEnabled;
    private volatile double voidThresholdY;
    private volatile boolean pvpEnabled;
    private volatile boolean fallDamageEnabled;
    private volatile boolean drowningDamageEnabled;
    private volatile boolean blockBreakEnabled;
    private volatile boolean blockPlaceEnabled;
    private volatile boolean hungerDepletionEnabled;
    private volatile boolean mobSpawningEnabled;
    private volatile boolean weatherLockEnabled;
    private volatile boolean itemDropEnabled;
    private volatile boolean itemPickupEnabled;

    public WorldProtectionListener(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
        this.reloadConfig();
    }

    /**
     * Refreshes cached world rules and thresholds from configuration.
     */
    public void reloadConfig() {
        FileConfiguration config = plugin.getConfigManager().getMain();

        this.allWorldsProtected = config.getBoolean("protection.all-worlds", false);

        this.protectedWorldNames.clear();
        this.protectedWorldNames.addAll(config.getStringList("protection.lobby-worlds"));
        for (LobbyData lobby : plugin.getLobbyManager().getLobbies().values()) {
            if (lobby.getSpawnLocation().getWorld() != null) {
                this.protectedWorldNames.add(lobby.getSpawnLocation().getWorld().getName());
            }
        }

        this.voidFallEnabled = config.getBoolean("protection.void-fall.enabled", true);
        this.voidThresholdY = config.getDouble("protection.void-fall.threshold-y", -50.0);
        this.pvpEnabled = config.getBoolean("protection.pvp-enabled", false);
        this.fallDamageEnabled = config.getBoolean("protection.fall-damage", false);
        this.drowningDamageEnabled = config.getBoolean("protection.drowning-damage", false);
        this.blockBreakEnabled = config.getBoolean("protection.block-break", false);
        this.blockPlaceEnabled = config.getBoolean("protection.block-place", false);
        this.hungerDepletionEnabled = config.getBoolean("protection.hunger-depletion", false);
        this.mobSpawningEnabled = config.getBoolean("protection.mob-spawning", false);
        this.weatherLockEnabled = config.getBoolean("protection.weather-lock", true);
        this.itemDropEnabled = config.getBoolean("protection.item-drop", false);
        this.itemPickupEnabled = config.getBoolean("protection.item-pickup", false);
    }

    /**
     * Fast zero-allocation O(1) check if a world is protected by LobbyCore.
     */
    private boolean isProtectedWorld(World world) {
        if (world == null) return false;
        return allWorldsProtected || protectedWorldNames.contains(world.getName());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvPDamage(EntityDamageByEntityEvent event) {
        if (pvpEnabled) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!isProtectedWorld(victim.getWorld())) return;

        if (event.getDamager() instanceof Player) {
            event.setCancelled(true);
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isProtectedWorld(player.getWorld())) return;

        EntityDamageEvent.DamageCause cause = event.getCause();

        if (cause == EntityDamageEvent.DamageCause.FALL && !fallDamageEnabled) {
            event.setCancelled(true);
            return;
        }

        if (cause == EntityDamageEvent.DamageCause.DROWNING && !drowningDamageEnabled) {
            event.setCancelled(true);
            return;
        }

        if (cause == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            rescueFromVoid(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (blockBreakEnabled) return;
        Player player = event.getPlayer();
        if (!isProtectedWorld(player.getWorld())) return;

        if (!player.hasPermission("lobby.admin.build")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (blockPlaceEnabled) return;
        Player player = event.getPlayer();
        if (!isProtectedWorld(player.getWorld())) return;

        if (!player.hasPermission("lobby.admin.build")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHungerDepletion(FoodLevelChangeEvent event) {
        if (hungerDepletionEnabled) return;
        if (!isProtectedWorld(event.getEntity().getWorld())) return;

        event.setCancelled(true);
        event.getEntity().setFoodLevel(20);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (mobSpawningEnabled) return;
        if (!isProtectedWorld(event.getLocation().getWorld())) return;

        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!weatherLockEnabled) return;
        if (!isProtectedWorld(event.getWorld())) return;

        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (itemDropEnabled) return;
        Player player = event.getPlayer();
        if (!isProtectedWorld(player.getWorld())) return;

        if (!player.hasPermission("lobby.admin.build")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (itemPickupEnabled) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isProtectedWorld(player.getWorld())) return;

        if (!player.hasPermission("lobby.admin.build")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCropTrample(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock() != null) {
            if (isProtectedWorld(event.getPlayer().getWorld())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!voidFallEnabled) return;

        Location to = event.getTo();
        if (to == null) return;

        // Fast-path 1: Ignore pitch/yaw rotation or horizontal-only movement without vertical change
        if (event.getFrom().getY() == to.getY()) return;

        // Fast-path 2: Return immediately if Y coordinate is above the void threshold
        if (to.getY() >= voidThresholdY) return;

        Player player = event.getPlayer();
        if (!isProtectedWorld(player.getWorld())) return;

        rescueFromVoid(player);
    }

    private void rescueFromVoid(@NotNull Player player) {
        long now = System.currentTimeMillis();
        Long last = voidRescueCooldown.get(player.getUniqueId());
        if (last != null && (now - last) < 2000) {
            return;
        }
        voidRescueCooldown.put(player.getUniqueId(), now);

        LobbyData best = plugin.getLobbyManager().getBestLobby(player);
        Location target = (best != null) ? best.getSpawnLocation() : player.getWorld().getSpawnLocation();

        player.getScheduler().run(plugin, task -> {
            player.setFallDistance(0);
            player.teleportAsync(target);
        }, null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        plugin.getHologramManager().handleChunkLoad(event.getChunk());
        plugin.getNpcManager().handleChunkLoad(event.getChunk());
    }

    /**
     * Cleans up player cooldown entries to prevent memory leaks over time.
     */
    public void cleanupPlayer(@NotNull UUID uuid) {
        voidRescueCooldown.remove(uuid);
    }
}
