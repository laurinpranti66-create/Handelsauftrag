package de.playground.handelsauftrag.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Markiert eine Inventory-Instanz als "Meine Aufträge"-GUI und merkt sich,
 * welcher Slot zu welchem eigenen Auftrag gehört (zum Stornieren per Klick).
 */
public final class MyRequestsHolder implements InventoryHolder {

    private Inventory inventory;
    private final Map<Integer, UUID> slotRequests = new HashMap<>();

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Map<Integer, UUID> getSlotRequests() {
        return slotRequests;
    }
}
