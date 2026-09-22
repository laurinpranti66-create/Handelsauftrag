package de.playground.handelsauftrag.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Markiert eine Inventory-Instanz als Handelsauftrag-Board und merkt sich,
 * welcher Slot zu welchem Auftrag gehört (für die aktuell angezeigte Seite),
 * damit Klicks robust auf den richtigen Auftrag gemappt werden auch wenn
 * sich die Liste zwischenzeitlich ändert.
 */
public final class BoardHolder implements InventoryHolder {

    private Inventory inventory;
    private final int page;
    private final Map<Integer, UUID> slotRequests = new HashMap<>();

    public BoardHolder(int page) {
        this.page = page;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public int getPage() {
        return page;
    }

    public Map<Integer, UUID> getSlotRequests() {
        return slotRequests;
    }
}
