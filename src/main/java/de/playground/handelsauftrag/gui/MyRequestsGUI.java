package de.playground.handelsauftrag.gui;

import de.playground.handelsauftrag.HandelsAuftragPlugin;
import de.playground.handelsauftrag.model.ItemRequest;
import de.playground.handelsauftrag.util.TimeFormatUtil;
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
 * Zeigt die eigenen offenen Aufträge des Spielers; Klick auf einen Eintrag
 * storniert ihn und erstattet das noch hinterlegte Restgeld.
 */
public final class MyRequestsGUI {

    private MyRequestsGUI() {
    }

    public static final int SIZE = 54;
    public static final int CLOSE_SLOT = 49;

    public static void open(HandelsAuftragPlugin plugin, Player player) {
        List<ItemRequest> own = plugin.getStorage().getByOwner(player.getUniqueId());

        MyRequestsHolder holder = new MyRequestsHolder();
        Inventory inv = Bukkit.createInventory(holder, SIZE, plugin.getMyRequestsTitle());
        holder.setInventory(inv);

        for (int slot = 45; slot < 54; slot++) {
            inv.setItem(slot, GuiUtil.filler());
        }
        inv.setItem(CLOSE_SLOT, GuiUtil.named(Material.BARRIER, "&c&lSchließen"));

        int slot = 0;
        for (ItemRequest req : own) {
            if (slot >= 45) {
                break;
            }
            ItemStack item = new ItemStack(req.getMaterial());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                boolean pending = req.isPending();
                meta.setDisplayName(HandelsAuftragPlugin.color((pending ? "&6" : "&e") + req.getMaterial().name()));
                List<String> lore = new ArrayList<>();
                lore.add(HandelsAuftragPlugin.color("&7Offen: &f" + req.getAmountRemaining() + "&7/&f" + req.getAmountTotal()));
                lore.add(HandelsAuftragPlugin.color("&7Preis/Stück: &a" + String.format(Locale.GERMANY, "%.2f", req.getPricePerUnit())));
                lore.add(HandelsAuftragPlugin.color("&7Hinterlegtes Restgeld: &a" + String.format(Locale.GERMANY, "%.2f", req.getRemainingValue())));
                lore.add(HandelsAuftragPlugin.color("&7Läuft ab in: &6" + TimeFormatUtil.format(req.getSecondsUntilExpiry())));
                lore.add("");
                if (pending) {
                    lore.add(HandelsAuftragPlugin.color("&6⏳ Wird in &f~" + req.getSecondsUntilActive() + "s &6veröffentlicht"));
                    lore.add(HandelsAuftragPlugin.color("&7Noch nicht auf dem Board sichtbar."));
                    lore.add("");
                    lore.add(HandelsAuftragPlugin.color("&cKlicken zum Stornieren (Vertippt?)"));
                } else {
                    lore.add(HandelsAuftragPlugin.color("&cKlicken zum Stornieren"));
                }
                lore.add(HandelsAuftragPlugin.color("&7(Restgeld wird sofort erstattet)"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
            holder.getSlotRequests().put(slot, req.getId());
            slot++;
        }

        if (own.isEmpty()) {
            inv.setItem(22, GuiUtil.named(Material.PAPER, "&7Du hast keine offenen Aufträge."));
        }

        player.openInventory(inv);
    }
}
