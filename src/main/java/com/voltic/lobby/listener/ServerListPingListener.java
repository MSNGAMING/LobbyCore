package com.voltic.lobby.listener;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ultra-high-performance server ping listener with pre-computed MOTD and hover profiles.
 * Eliminates per-ping allocations, string parsing, and regex evaluations.
 */
public class ServerListPingListener implements Listener {

    private final LobbyPlugin plugin;

    private volatile boolean motdEnabled;
    private volatile Component cachedMotd;
    private volatile boolean overrideMaxPlayers;
    private volatile int cachedMaxPlayers;
    private volatile boolean overrideVersion;
    private volatile String cachedVersionText;
    private volatile boolean hoverEnabled;
    private final List<PaperServerListPingEvent.ListedPlayerInfo> cachedListedPlayers = new ArrayList<>();

    public ServerListPingListener(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
        this.reloadConfig();
    }

    /**
     * Pre-computes and caches MOTD components and hover player samples.
     */
    public synchronized void reloadConfig() {
        FileConfiguration config = plugin.getConfigManager().getMotd();

        this.motdEnabled = config.getBoolean("motd.enabled", true);
        if (!this.motdEnabled) {
            return;
        }

        String line1 = config.getString("motd.line1", "<gradient:#00f2fe:#4facfe><bold>VOLTIC NETWORK</bold></gradient> <gray>•</gray> <yellow>1.21 - 1.26</yellow>");
        String line2 = config.getString("motd.line2", "<white>Modular Next-Gen Paper Lobby System</white>");

        this.cachedMotd = TextUtil.parse(line1)
                .append(Component.newline())
                .append(TextUtil.parse(line2));

        this.overrideMaxPlayers = config.getBoolean("motd.override-max-players", false);
        this.cachedMaxPlayers = config.getInt("motd.fake-max-players", 500);

        this.overrideVersion = config.getBoolean("motd.override-version", false);
        this.cachedVersionText = config.getString("motd.version-text", "Paper 1.21 - 1.26");

        this.hoverEnabled = config.getBoolean("motd.hover.enabled", true);
        this.cachedListedPlayers.clear();

        if (this.hoverEnabled) {
            List<String> hoverLines = config.getStringList("motd.hover.lines");
            for (String rawLine : hoverLines) {
                Component parsed = TextUtil.parse(rawLine);
                String legacy = LegacyComponentSerializer.legacySection().serialize(parsed);
                this.cachedListedPlayers.add(new PaperServerListPingEvent.ListedPlayerInfo(legacy, UUID.randomUUID()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerListPing(PaperServerListPingEvent event) {
        if (!motdEnabled) {
            return;
        }

        // Apply pre-compiled MOTD
        if (cachedMotd != null) {
            event.motd(cachedMotd);
        }

        // Apply max players override
        if (overrideMaxPlayers) {
            event.setMaxPlayers(cachedMaxPlayers);
        }

        // Apply custom version text
        if (overrideVersion && cachedVersionText != null) {
            event.setVersion(cachedVersionText);
        }

        // Apply modern ListedPlayerInfo hover sample
        if (hoverEnabled && !cachedListedPlayers.isEmpty()) {
            List<PaperServerListPingEvent.ListedPlayerInfo> listed = event.getListedPlayers();
            listed.clear();
            listed.addAll(cachedListedPlayers);
        }
    }
}
