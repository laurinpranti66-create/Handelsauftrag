package de.playground.handelsauftrag.model;

import org.bukkit.Material;

/**
 * Übergangszustand während ein Spieler per Chat Menge und Preis für einen
 * neuen Auftrag eingibt (nachdem er das Item im GUI gewählt hat).
 */
public final class PendingCreation {

    public enum Stage {
        AWAIT_AMOUNT,
        AWAIT_PRICE
    }

    private final Material material;
    private Stage stage;
    private int amount;

    public PendingCreation(Material material) {
        this.material = material;
        this.stage = Stage.AWAIT_AMOUNT;
    }

    public Material getMaterial() {
        return material;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
