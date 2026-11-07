package com.voltic.lobby.chat;

import com.voltic.lobby.LobbyPlugin;
import com.voltic.lobby.util.TextUtil;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Modern Paper AsyncChatEvent listener for formatting, anti-swear, and anti-spam.
 * Pre-computes template components outside rendering lambdas for minimal per-viewer overhead.
 */
public class ChatListener implements Listener {

    private final LobbyPlugin plugin;

    public ChatListener(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Anti-spam check
        if (plugin.getChatManager().checkSpam(player, rawMessage)) {
            event.setCancelled(true);
            return;
        }

        // Anti-swear check & filtering
        String filteredMessage = plugin.getChatManager().filterSwearWords(player, rawMessage);
        if (filteredMessage == null) {
            event.setCancelled(true);
            return;
        }

        // Update event message component so downstream plugins see filtered text
        event.message(Component.text(filteredMessage));

        // Construct final rendered message template once
        String format = plugin.getChatManager().getFormatTemplate();
        String formattedWithPlayer = TextUtil.replacePlaceholders(player, format);

        Component prefix;
        Component suffix;
        if (formattedWithPlayer.contains("%message%")) {
            String[] parts = formattedWithPlayer.split("%message%", 2);
            prefix = TextUtil.parse(parts[0]);
            suffix = parts.length > 1 ? TextUtil.parse(parts[1]) : Component.empty();
        } else {
            prefix = TextUtil.parse(formattedWithPlayer).append(Component.space());
            suffix = Component.empty();
        }

        Component messageComponent = player.hasPermission("lobby.chat.color")
                ? TextUtil.parse(filteredMessage)
                : Component.text(filteredMessage);

        Component assembledMessage = prefix.append(messageComponent).append(suffix);

        // Deliver assembled message with zero per-viewer recalculation
        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, msg) -> assembledMessage));
    }
}
