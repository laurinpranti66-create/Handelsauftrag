package de.playground.handelsauftrag.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Markiert eine Inventory-Instanz als "Auftrag erstellen"-GUI, in der der
 * Spieler ein Beispiel-Item in einen freien Slot legt, um das gewünschte
 * Material festzulegen.
 */
public final class CreateHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
