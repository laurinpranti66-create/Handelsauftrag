package de.playground.handelsauftrag.gui;

import de.playground.handelsauftrag.HandelsAuftragPlugin;
import de.playground.handelsauftrag.model.ItemRequest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GUI zum (teilweisen) Beliefern eines konkreten Auftrags. Spieler legen
 * beliebige Mengen in die freie Fläche (Reihen 3-4), passende Items werden
 * beim Bestätigen automatisch abgezogen und ausbezahlt; überschüssige oder
 * nicht passende Items bleiben liegen und werden zurückgegeben.
 */
public final class DeliverGUI {

    private DeliverGUI() {
    }

    public static final int SIZE = 54;
    public static final int INFO_SLOT = 4;
    public static final int[] DELIVERY_SLOTS;

    static {
        List<Integer> slots = new ArrayList<>();
        for (int row = 2; row <= 3; row++) {
            for (int col = 1; col <= 7; col++) {
                slots.add(row * 9 + col);
            }
        }
        DELIVERY_SLOTS = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            DELIVERY_SLOTS[i] = slots.get(i);
        }
    }

    public static final int CANCEL_SLOT = 48;
    public static final int CONFIRM_SLOT = 50;

    public static boolean isDeliverySlot(int slot) {
        for (int s : DELIVERY_SLOTS) {
            if (s == slot) {
                return true;
            }
        }
        return false;
    }

    public static void open(HandelsAuftragPlugin plugin, Player player, ItemRequest request) {
        DeliverHolder holder = new DeliverHolder(request.getId());
        Inventory inv = Bukkit.createInventory(holder, SIZE, plugin.getDeliverTitle());
        holder.setInventory(inv);

        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, GuiUtil.filler());
        }
        for (int slot : DELIVERY_SLOTS) {
            inv.setItem(slot, null);
        }

        ItemStack info = new ItemStack(request.getMaterial());
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(HandelsAuftragPlugin.color("&eAuftrag von &f" + request.getOwnerName()));
            List<String> lore = new ArrayList<>();
            lore.add(HandelsAuftragPlugin.color("&7Material: &f" + request.getMaterial().name()));
            lore.add(HandelsAuftragPlugin.color("&7Offen: &f" + request.getAmountRemaining() + "&7/&f" + request.getAmountTotal()));
            lore.add(HandelsAuftragPlugin.color("&7Preis/Stück: &a" + String.format(Locale.GERMANY, "%.2f", request.getPricePerUnit())));
            lore.add("");
            lore.add(HandelsAuftragPlugin.color("&7Lege passende Items in die Fläche"));
            lore.add(HandelsAuftragPlugin.color("&7darunter (auch teilweise möglich)."));
            meta.setLore(lore);
            info.setItemMeta(meta);
        }
        inv.setItem(INFO_SLOT, info);

        inv.setItem(CANCEL_SLOT, GuiUtil.named(Material.BARRIER, "&c&lAbbrechen", "&7Gibt deine eingelegten Items zurück."));
        inv.setItem(CONFIRM_SLOT, GuiUtil.named(Material.LIME_DYE, "&a&lLiefern", "&7Liefert die eingelegten Items", "&7und zahlt dich automatisch aus."));

        player.openInventory(inv);
    }
}
