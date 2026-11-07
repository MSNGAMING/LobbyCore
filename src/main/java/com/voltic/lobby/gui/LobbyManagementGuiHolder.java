package com.voltic.lobby.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Custom InventoryHolder to identify the Lobby Worlds Management GUI safely.
 */
public class LobbyManagementGuiHolder implements InventoryHolder {

    private Inventory inventory;

    public void setInventory(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
