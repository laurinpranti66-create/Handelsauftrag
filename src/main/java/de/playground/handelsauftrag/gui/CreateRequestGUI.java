package de.playground.handelsauftrag.gui;

import de.playground.handelsauftrag.HandelsAuftragPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * GUI in dem der Spieler ein Beispiel-Item in den mittleren Slot legt, um das
 * Material für seinen neuen Auftrag festzulegen. Menge und Preis werden im
 * Anschluss per Chat abgefragt (siehe ChatInputListener).
 */
public final class CreateRequestGUI {

    private CreateRequestGUI() {
    }

    public static final int SIZE = 27;
    public static final int SAMPLE_SLOT = 13;
    public static final int CANCEL_SLOT = 11;
    public static final int CONFIRM_SLOT = 15;

    public static void open(HandelsAuftragPlugin plugin, Player player) {
        CreateHolder holder = new CreateHolder();
        Inventory inv = Bukkit.createInventory(holder, SIZE, plugin.getCreateTitle());
        holder.setInventory(inv);

        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, GuiUtil.filler());
        }
        inv.setItem(SAMPLE_SLOT, null);

        inv.setItem(CANCEL_SLOT, GuiUtil.named(Material.BARRIER, "&c&lAbbrechen"));
        inv.setItem(CONFIRM_SLOT, GuiUtil.named(Material.LIME_DYE, "&a&lBestätigen",
                "&7Lege zuerst ein Item in das", "&7freie Feld in der Mitte."));

        player.openInventory(inv);
    }
}
