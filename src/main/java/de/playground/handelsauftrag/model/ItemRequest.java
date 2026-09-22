package de.playground.handelsauftrag.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

/**
 * Ein einzelner Kauf-Auftrag: Spieler A hat bereits Geld hinterlegt (Escrow)
 * für "amountTotal" Stück von "material" zu "pricePerUnit" je Stück.
 * Andere Spieler liefern Items und bekommen dafür automatisch bezahlt,
 * bis amountRemaining auf 0 sinkt.
 */
public final class ItemRequest {

    private final UUID id;
    private final UUID ownerUuid;
    private final String ownerName;
    private final Material material;
    private final int amountTotal;
    private int amountRemaining;
    private final double pricePerUnit;
    private final long createdAt;
    private final long activationTime;
    private final long expiresAt;

    public ItemRequest(UUID id, UUID ownerUuid, String ownerName, Material material,
                        int amountTotal, int amountRemaining, double pricePerUnit, long createdAt,
                        long activationTime, long expiresAt) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.material = material;
        this.amountTotal = amountTotal;
        this.amountRemaining = amountRemaining;
        this.pricePerUnit = pricePerUnit;
        this.createdAt = createdAt;
        this.activationTime = activationTime;
        this.expiresAt = expiresAt;
    }

    public static ItemRequest fromSection(UUID id, ConfigurationSection section) {
        UUID owner = UUID.fromString(section.getString("owner-uuid"));
        String ownerName = section.getString("owner-name", "Unbekannt");
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) {
            material = Material.STONE;
        }
        int amountTotal = section.getInt("amount-total");
        int amountRemaining = section.getInt("amount-remaining");
        double pricePerUnit = section.getDouble("price-per-unit");
        long createdAt = section.getLong("created-at");
        // Fehlt bei älteren, vor dem Pending-Fenster gespeicherten Aufträgen -
        // Fallback auf createdAt, damit sie sofort als aktiv gelten (nicht pending).
        long activationTime = section.getLong("activation-time", createdAt);
        // Fehlt bei älteren, vor der Ablaufzeit-Funktion gespeicherten Aufträgen -
        // Fallback "läuft nie ab", damit alte Aufträge beim Update nicht plötzlich
        // reihenweise verfallen.
        long expiresAt = section.getLong("expires-at", Long.MAX_VALUE);
        return new ItemRequest(id, owner, ownerName, material, amountTotal, amountRemaining, pricePerUnit,
                createdAt, activationTime, expiresAt);
    }

    public void writeToSection(ConfigurationSection section) {
        section.set("owner-uuid", ownerUuid.toString());
        section.set("owner-name", ownerName);
        section.set("material", material.name());
        section.set("amount-total", amountTotal);
        section.set("amount-remaining", amountRemaining);
        section.set("price-per-unit", pricePerUnit);
        section.set("created-at", createdAt);
        section.set("activation-time", activationTime);
        section.set("expires-at", expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmountTotal() {
        return amountTotal;
    }

    public int getAmountRemaining() {
        return amountRemaining;
    }

    public void setAmountRemaining(int amountRemaining) {
        this.amountRemaining = amountRemaining;
    }

    public double getPricePerUnit() {
        return pricePerUnit;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getActivationTime() {
        return activationTime;
    }

    /**
     * True, solange der Auftrag noch im Sicherheitsfenster nach dem Erstellen
     * ist (noch nicht auf dem Board sichtbar/belieferbar) - z.B. bei Vertippern
     * kann der Ersteller ihn in dieser Zeit noch über "Meine Aufträge" stornieren.
     */
    public boolean isPending() {
        return System.currentTimeMillis() < activationTime;
    }

    public long getSecondsUntilActive() {
        return Math.max(0L, (activationTime - System.currentTimeMillis()) / 1000L);
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    /**
     * True, wenn der Auftrag seine Lebenszeit überschritten hat und beim
     * nächsten Durchlauf des ExpiryTask automatisch storniert + erstattet wird.
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }

    public long getSecondsUntilExpiry() {
        return Math.max(0L, (expiresAt - System.currentTimeMillis()) / 1000L);
    }

    public double getRemainingValue() {
        return amountRemaining * pricePerUnit;
    }

    public boolean isFulfilled() {
        return amountRemaining <= 0;
    }
}
