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
 * Das Haupt-Board: zeigt alle offenen Aufträge aller Spieler, paginiert zu
 * 36 pro Seite (Reihen 0-3), mit einer Steuerleiste unten (Reihe 5).
 */
public final class BoardGUI {

    private BoardGUI() {
    }

    public static final int SIZE = 54;
    public static final int SLOTS_PER_PAGE = 45;
    public static final int CREATE_SLOT = 46;
    public static final int MY_REQUESTS_SLOT = 47;
    public static final int PREV_SLOT = 49;
    public static final int PAGE_INFO_SLOT = 50;
    public static final int NEXT_SLOT = 51;
    public static final int COLLECT_SLOT = 52;
    public static final int CLOSE_SLOT = 53;

    public static void open(HandelsAuftragPlugin plugin, Player player, int page) {
        List<ItemRequest> all = plugin.getStorage().getAllActive();
        int maxPage = all.isEmpty() ? 0 : (all.size() - 1) / SLOTS_PER_PAGE;
        int clamped = Math.max(0, Math.min(page, maxPage));

        BoardHolder holder = new BoardHolder(clamped);
        Inventory inv = Bukkit.createInventory(holder, SIZE, plugin.getBoardTitle());
        holder.setInventory(inv);

        int start = clamped * SLOTS_PER_PAGE;
        int end = Math.min(start + SLOTS_PER_PAGE, all.size());
        for (int i = start; i < end; i++) {
            ItemRequest req = all.get(i);
            int slot = i - start;
            inv.setItem(slot, buildRequestIcon(req));
            holder.getSlotRequests().put(slot, req.getId());
        }

        ItemStack filler = GuiUtil.filler();
        for (int slot = 45; slot < 54; slot++) {
            inv.setItem(slot, filler);
        }

        inv.setItem(CREATE_SLOT, GuiUtil.named(Material.OAK_SIGN, "&a&lNeuer Auftrag",
                "&7Lege Geld für benötigte Items zurück -", "&7andere Spieler liefern und du", "&7bekommst automatisch beliefert."));
        inv.setItem(MY_REQUESTS_SLOT, GuiUtil.named(Material.HOPPER, "&e&lMeine Aufträge",
                "&7Zeigt deine offenen Aufträge", "&7und erlaubt das Stornieren."));

        if (clamped > 0) {
            inv.setItem(PREV_SLOT, GuiUtil.named(Material.ARROW, "&a« Vorherige Seite"));
        }
        inv.setItem(PAGE_INFO_SLOT, GuiUtil.named(Material.NETHER_STAR, "&d&lSeite " + (clamped + 1) + "&7/&d" + (maxPage + 1),
                "&7Offene Aufträge insgesamt: &f" + all.size()));
        if (end < all.size()) {
            inv.setItem(NEXT_SLOT, GuiUtil.named(Material.ARROW, "&aNächste Seite »"));
        }

        int pendingCount = plugin.getStorage().countPendingItems(player.getUniqueId());
        if (pendingCount > 0) {
            inv.setItem(COLLECT_SLOT, GuiUtil.named(Material.CHEST, "&e&lAbholung &8(&f" + pendingCount + "&8)",
                    "&7Du hast &f" + pendingCount + " Stapel&7 aus", "&7belieferten Aufträgen abzuholen!"));
        } else {
            inv.setItem(COLLECT_SLOT, GuiUtil.named(Material.CHEST, "&e&lAbholung",
                    "&7Hier landen gelieferte Items -", "&7aktuell nichts abzuholen."));
        }

        inv.setItem(CLOSE_SLOT, GuiUtil.named(Material.BARRIER, "&c&lSchließen"));

        if (all.isEmpty()) {
            inv.setItem(22, GuiUtil.named(Material.PAPER, "&7Aktuell gibt es keine offenen Aufträge."));
        }

        player.openInventory(inv);
    }

    public static ItemStack buildRequestIcon(ItemRequest req) {
        ItemStack item = new ItemStack(req.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(HandelsAuftragPlugin.color("&e" + formatMaterial(req.getMaterial())));
            List<String> lore = new ArrayList<>();
            lore.add(HandelsAuftragPlugin.color("&7Von: &f" + req.getOwnerName()));
            lore.add(HandelsAuftragPlugin.color("&7Offen: &f" + req.getAmountRemaining() + "&7/&f" + req.getAmountTotal()));
            lore.add(HandelsAuftragPlugin.color("&7Preis/Stück: &a" + format(req.getPricePerUnit())));
            lore.add(HandelsAuftragPlugin.color("&7Gesamtwert offen: &a" + format(req.getRemainingValue())));
            lore.add(HandelsAuftragPlugin.color("&7Läuft ab in: &6" + TimeFormatUtil.format(req.getSecondsUntilExpiry())));
            lore.add("");
            lore.add(HandelsAuftragPlugin.color("&eKlicken zum Liefern"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String format(double value) {
        return String.format(Locale.GERMANY, "%.2f", value);
    }

    private static String formatMaterial(Material material) {
        String name = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
