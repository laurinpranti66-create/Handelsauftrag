package de.playground.handelsauftrag.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Markiert eine Inventory-Instanz als Liefer-GUI für einen bestimmten,
 * konkreten Auftrag (per ID referenziert statt per Objekt-Referenz, damit
 * immer der aktuelle Stand aus dem Storage gelesen wird).
 */
public final class DeliverHolder implements InventoryHolder {

    private Inventory inventory;
    private final UUID requestId;

    public DeliverHolder(UUID requestId) {
        this.requestId = requestId;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getRequestId() {
        return requestId;
    }
}
