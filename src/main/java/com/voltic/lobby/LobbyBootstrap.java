package com.voltic.lobby;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import org.jetbrains.annotations.NotNull;

/**
 * Native Paper plugin bootstrapper for early initialization and lifecycle management.
 */
public class LobbyBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        context.getLogger().info("Bootstrapping LobbyCore native Paper plugin (1.21.x - 26.x)...");
    }
}
