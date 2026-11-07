package com.voltic.lobby.npc;

import com.voltic.lobby.LobbyPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for interactions with NPC entities and dispatches actions.
 */
public class NpcInteractListener implements Listener {

    private final LobbyPlugin plugin;

    public NpcInteractListener(@NotNull LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Entity clicked = event.getRightClicked();
        String npcId = clicked.getPersistentDataContainer().get(plugin.getNpcManager().getNpcKey(), PersistentDataType.STRING);
        if (npcId == null) {
            return;
        }

        event.setCancelled(true);
        NpcData npc = plugin.getNpcManager().getNpc(npcId);
        if (npc != null) {
            plugin.getNpcManager().handleInteraction(event.getPlayer(), npc);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        String npcId = damaged.getPersistentDataContainer().get(plugin.getNpcManager().getNpcKey(), PersistentDataType.STRING);
        if (npcId == null) {
            return;
        }

        event.setCancelled(true);
        if (event.getDamager() instanceof Player player) {
            NpcData npc = plugin.getNpcManager().getNpc(npcId);
            if (npc != null) {
                plugin.getNpcManager().handleInteraction(player, npc);
            }
        }
    }
}
