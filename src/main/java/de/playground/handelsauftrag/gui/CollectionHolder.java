package de.playground.handelsauftrag.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Markiert eine Inventory-Instanz als Abholungs-GUI und merkt sich, welcher
 * Slot zu welchem Index in der wartenden Item-Liste des Spielers gehört.
 */
public final class CollectionHolder implements InventoryHolder {

    private Inventory inventory;
    private final Map<Integer, Integer> slotIndexes = new HashMap<>();

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Map<Integer, Integer> getSlotIndexes() {
        return slotIndexes;
    }
}
