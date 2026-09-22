package de.playground.handelsauftrag.gui;

import de.playground.handelsauftrag.HandelsAuftragPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Zeigt alle wartenden Item-Lieferungen eines Spielers (aus /ha-Aufträgen, die
 * beliefert wurden). Items landen NIE automatisch im Inventar - der Spieler
 * holt sie hier bewusst ab, einzeln per Klick oder alle auf einmal.
 */
public final class CollectionGUI {

    private CollectionGUI() {
    }

    public static final int SIZE = 54;
    public static final int ITEMS_PER_PAGE = 45;
    public static final int COLLECT_ALL_SLOT = 49;
    public static final int MORE_INFO_SLOT = 50;
    public static final int CLOSE_SLOT = 53;

    public static void open(HandelsAuftragPlugin plugin, Player player) {
        List<ItemStack> items = plugin.getStorage().getPendingItems(player.getUniqueId());

        CollectionHolder holder = new CollectionHolder();
        Inventory inv = Bukkit.createInventory(holder, SIZE, plugin.getCollectionTitle());
        holder.setInventory(inv);

        for (int slot = 45; slot < 54; slot++) {
            inv.setItem(slot, GuiUtil.filler());
        }

        int shown = Math.min(items.size(), ITEMS_PER_PAGE);
        for (int i = 0; i < shown; i++) {
            ItemStack display = items.get(i).clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                if (meta.hasLore() && meta.getLore() != null) {
                    lore.addAll(meta.getLore());
                    lore.add("");
                }
                lore.add(HandelsAuftragPlugin.color("&aKlicken zum Abholen"));
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(i, display);
            holder.getSlotIndexes().put(i, i);
        }

        if (items.isEmpty()) {
            inv.setItem(22, GuiUtil.named(Material.PAPER, "&7Keine wartenden Lieferungen."));
        } else {
            inv.setItem(COLLECT_ALL_SLOT, GuiUtil.named(Material.HOPPER, "&a&lAlles abholen",
                    "&7Gibt dir alle &f" + items.size() + " Stapel&7 auf einmal.",
                    "&7Was nicht reinpasst, fällt vor dir hin."));
            if (items.size() > ITEMS_PER_PAGE) {
                inv.setItem(MORE_INFO_SLOT, GuiUtil.named(Material.NETHER_STAR,
                        "&e+" + (items.size() - ITEMS_PER_PAGE) + " weitere Stapel",
                        "&7Hol erst diese hier ab, der Rest", "&7erscheint dann beim nächsten Öffnen."));
            }
        }

        inv.setItem(CLOSE_SLOT, GuiUtil.named(Material.BARRIER, "&c&lSchließen"));

        player.openInventory(inv);
    }
}
