package de.playground.handelsauftrag.gui;

import de.playground.handelsauftrag.HandelsAuftragPlugin;
import de.playground.handelsauftrag.model.ItemRequest;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Zentraler Klick-/Drag-/Close-Handler für alle vier GUIs des Plugins.
 * Routing erfolgt über den InventoryHolder-Typ der jeweiligen Inventory.
 */
public final class GuiListener implements Listener {

    private final HandelsAuftragPlugin plugin;

    public GuiListener(HandelsAuftragPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder rawHolder = event.getInventory().getHolder();
        if (rawHolder instanceof BoardHolder holder) {
            handleBoardClick(event, holder);
        } else if (rawHolder instanceof CreateHolder holder) {
            handleCreateClick(event, holder);
        } else if (rawHolder instanceof DeliverHolder holder) {
            handleDeliverClick(event, holder);
        } else if (rawHolder instanceof MyRequestsHolder holder) {
            handleMyRequestsClick(event, holder);
        } else if (rawHolder instanceof CollectionHolder holder) {
            handleCollectionClick(event, holder);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder rawHolder = event.getInventory().getHolder();
        int topSize = event.getView().getTopInventory().getSize();

        if (rawHolder instanceof BoardHolder || rawHolder instanceof MyRequestsHolder || rawHolder instanceof CollectionHolder) {
            for (int slot : event.getRawSlots()) {
                if (slot < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }
        if (rawHolder instanceof CreateHolder) {
            for (int slot : event.getRawSlots()) {
                if (slot < topSize && slot != CreateRequestGUI.SAMPLE_SLOT) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }
        if (rawHolder instanceof DeliverHolder) {
            for (int slot : event.getRawSlots()) {
                if (slot < topSize && !DeliverGUI.isDeliverySlot(slot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder rawHolder = event.getInventory().getHolder();
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (rawHolder instanceof CreateHolder) {
            ItemStack sample = event.getInventory().getItem(CreateRequestGUI.SAMPLE_SLOT);
            if (sample != null && sample.getType() != Material.AIR) {
                returnItem(player, sample);
                event.getInventory().setItem(CreateRequestGUI.SAMPLE_SLOT, null);
            }
        } else if (rawHolder instanceof DeliverHolder) {
            returnRemainingDeliverySlots(player, event.getInventory());
        }
    }

    // ---------------------------------------------------------------
    // Board
    // ---------------------------------------------------------------

    private void handleBoardClick(InventoryClickEvent event, BoardHolder holder) {
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == BoardGUI.PREV_SLOT) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> BoardGUI.open(plugin, player, holder.getPage() - 1));
            return;
        }
        if (slot == BoardGUI.NEXT_SLOT) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> BoardGUI.open(plugin, player, holder.getPage() + 1));
            return;
        }
        if (slot == BoardGUI.CREATE_SLOT) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> CreateRequestGUI.open(plugin, player));
            return;
        }
        if (slot == BoardGUI.MY_REQUESTS_SLOT) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> MyRequestsGUI.open(plugin, player));
            return;
        }
        if (slot == BoardGUI.COLLECT_SLOT) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> CollectionGUI.open(plugin, player));
            return;
        }
        if (slot == BoardGUI.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (slot == BoardGUI.PAGE_INFO_SLOT) {
            return;
        }

        UUID requestId = holder.getSlotRequests().get(slot);
        if (requestId == null) {
            return;
        }
        ItemRequest request = plugin.getStorage().getRequest(requestId);
        if (request == null) {
            player.sendMessage(plugin.prefixed("&cDieser Auftrag ist nicht mehr verfügbar."));
            int page = holder.getPage();
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> BoardGUI.open(plugin, player, page));
            return;
        }
        if (request.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage(plugin.prefixed("&cDu kannst deinen eigenen Auftrag nicht beliefern."));
            return;
        }

        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> DeliverGUI.open(plugin, player, request));
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    private void handleCreateClick(InventoryClickEvent event, CreateHolder holder) {
        Player player = (Player) event.getWhoClicked();
        Inventory top = event.getView().getTopInventory();
        boolean clickedTop = event.getClickedInventory() != null && event.getClickedInventory().equals(top);

        if (!clickedTop) {
            if (event.getClick().isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        int slot = event.getSlot();
        if (slot == CreateRequestGUI.SAMPLE_SLOT) {
            return;
        }

        event.setCancelled(true);

        if (slot == CreateRequestGUI.CANCEL_SLOT) {
            player.closeInventory();
            return;
        }
        if (slot == CreateRequestGUI.CONFIRM_SLOT) {
            ItemStack sample = top.getItem(CreateRequestGUI.SAMPLE_SLOT);
            if (sample == null || sample.getType() == Material.AIR) {
                player.sendMessage(plugin.prefixed("&cLege zuerst ein Item in das Feld in der Mitte."));
                return;
            }
            Material material = sample.getType();
            if (material.isAir() || !material.isItem()) {
                player.sendMessage(plugin.prefixed("&cDieses Item kann nicht angefragt werden."));
                return;
            }

            int activeCount = plugin.getStorage().countActiveByOwner(player.getUniqueId());
            if (activeCount >= plugin.getMaxActiveRequestsPerPlayer()) {
                player.sendMessage(plugin.prefixed("&cDu hast bereits die maximale Anzahl offener Aufträge (&f"
                        + plugin.getMaxActiveRequestsPerPlayer() + "&c)."));
                return;
            }

            top.setItem(CreateRequestGUI.SAMPLE_SLOT, null);
            returnItem(player, sample);

            de.playground.handelsauftrag.model.PendingCreation pending = new de.playground.handelsauftrag.model.PendingCreation(material);
            plugin.getPendingCreations().put(player.getUniqueId(), pending);

            player.closeInventory();
            player.sendMessage(plugin.prefixed("&aItem gewählt: &f" + material.name()));
            player.sendMessage(plugin.prefixed("&eSchreibe jetzt die gewünschte &fMenge&e in den Chat. (&7'abbrechen' zum Abbrechen&e)"));
        }
    }

    // ---------------------------------------------------------------
    // Deliver
    // ---------------------------------------------------------------

    private void handleDeliverClick(InventoryClickEvent event, DeliverHolder holder) {
        Player player = (Player) event.getWhoClicked();
        Inventory top = event.getView().getTopInventory();
        boolean clickedTop = event.getClickedInventory() != null && event.getClickedInventory().equals(top);

        if (!clickedTop) {
            if (event.getClick().isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        int slot = event.getSlot();
        if (DeliverGUI.isDeliverySlot(slot)) {
            return;
        }

        event.setCancelled(true);

        if (slot == DeliverGUI.CANCEL_SLOT) {
            player.closeInventory();
            return;
        }
        if (slot == DeliverGUI.CONFIRM_SLOT) {
            processDelivery(player, holder, top);
        }
    }

    private void processDelivery(Player player, DeliverHolder holder, Inventory top) {
        ItemRequest request = plugin.getStorage().getRequest(holder.getRequestId());
        if (request == null) {
            player.sendMessage(plugin.prefixed("&cDieser Auftrag ist nicht mehr verfügbar."));
            returnRemainingDeliverySlots(player, top);
            player.closeInventory();
            return;
        }

        int matched = 0;
        List<Integer> matchedSlots = new ArrayList<>();
        for (int slot : DeliverGUI.DELIVERY_SLOTS) {
            ItemStack item = top.getItem(slot);
            if (item != null && item.getType() == request.getMaterial()) {
                matched += item.getAmount();
                matchedSlots.add(slot);
            }
        }

        if (matched <= 0) {
            player.sendMessage(plugin.prefixed("&cLege passende Items (&f" + request.getMaterial().name()
                    + "&c) in die Lieferfläche."));
            return;
        }

        int toDeliver = Math.min(matched, request.getAmountRemaining());
        double payout = toDeliver * request.getPricePerUnit();

        // Erst die Auszahlung bestätigen, DANACH erst Items abbuchen und den
        // Auftrag reduzieren - so verliert niemand Items, falls Vault aus
        // irgendeinem Grund die Auszahlung nicht durchführen kann.
        EconomyResponse response = plugin.getEconomy().depositPlayer(player, payout);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("Auszahlung an " + player.getName() + " fehlgeschlagen: " + response.errorMessage);
            player.sendMessage(plugin.prefixed("&cAuszahlung fehlgeschlagen, versuch's gleich nochmal. Es wurde nichts abgebucht."));
            return;
        }

        int remainingToTake = toDeliver;
        for (int slot : matchedSlots) {
            if (remainingToTake <= 0) {
                break;
            }
            ItemStack item = top.getItem(slot);
            if (item == null) {
                continue;
            }
            int take = Math.min(item.getAmount(), remainingToTake);
            if (take >= item.getAmount()) {
                top.setItem(slot, null);
            } else {
                item.setAmount(item.getAmount() - take);
            }
            remainingToTake -= take;
        }

        plugin.getStorage().reduceRemaining(request.getId(), toDeliver);

        player.sendMessage(plugin.prefixed("&aDu hast &f" + toDeliver + "x " + request.getMaterial().name()
                + " &ageliefert und &f" + format(payout) + " &aerhalten."));

        // Die gelieferten Items gehören jetzt dem Auftraggeber - er hat sie ja
        // per Escrow bezahlt. Landen IMMER in der Abholung (nie automatisch im
        // Inventar), der Auftraggeber holt sie bewusst über /ha -> Abholung ab.
        List<ItemStack> deliveredStacks = splitIntoStacks(request.getMaterial(), toDeliver);
        plugin.getStorage().addPendingItems(request.getOwnerUuid(), deliveredStacks);

        String notifyMsg = "&a" + player.getName() + " &7hat &f" + toDeliver + "x " + request.getMaterial().name()
                + " &7für deinen Auftrag geliefert (&f" + format(payout) + "&7) - hol die Items über &f/ha &7-> &fAbholung&7 ab.";
        Player owner = Bukkit.getPlayer(request.getOwnerUuid());
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(plugin.prefixed(notifyMsg));
        } else {
            plugin.getStorage().addNotification(request.getOwnerUuid(), notifyMsg);
        }

        boolean fulfilled = plugin.getStorage().getRequest(request.getId()) == null;
        if (fulfilled) {
            player.sendMessage(plugin.prefixed("&6Dieser Auftrag ist jetzt vollständig beliefert!"));
        }

        returnRemainingDeliverySlots(player, top);
        player.closeInventory();
    }

    // ---------------------------------------------------------------
    // Meine Aufträge
    // ---------------------------------------------------------------

    private void handleMyRequestsClick(InventoryClickEvent event, MyRequestsHolder holder) {
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == MyRequestsGUI.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        UUID requestId = holder.getSlotRequests().get(slot);
        if (requestId == null) {
            return;
        }
        ItemRequest request = plugin.getStorage().getRequest(requestId);
        if (request == null || !request.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage(plugin.prefixed("&cDieser Auftrag existiert nicht mehr."));
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> MyRequestsGUI.open(plugin, player));
            return;
        }

        // Erst die Rückerstattung bestätigen, DANACH erst den Auftrag löschen -
        // so geht kein Geld verloren, falls Vault die Erstattung nicht durchführen kann.
        double refund = request.getRemainingValue();
        EconomyResponse response = plugin.getEconomy().depositPlayer(player, refund);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("Erstattung an " + player.getName() + " fehlgeschlagen: " + response.errorMessage);
            player.sendMessage(plugin.prefixed("&cErstattung fehlgeschlagen, versuch's gleich nochmal. Der Auftrag wurde nicht storniert."));
            return;
        }

        plugin.getStorage().removeRequest(requestId);
        player.sendMessage(plugin.prefixed("&aAuftrag storniert. &f" + format(refund)
                + " &awurden erstattet."));

        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> MyRequestsGUI.open(plugin, player));
    }

    // ---------------------------------------------------------------
    // Abholung
    // ---------------------------------------------------------------

    private void handleCollectionClick(InventoryClickEvent event, CollectionHolder holder) {
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == CollectionGUI.CLOSE_SLOT || slot == CollectionGUI.MORE_INFO_SLOT) {
            if (slot == CollectionGUI.CLOSE_SLOT) {
                player.closeInventory();
            }
            return;
        }

        if (slot == CollectionGUI.COLLECT_ALL_SLOT) {
            List<ItemStack> items = plugin.getStorage().popPendingItems(player.getUniqueId());
            if (items.isEmpty()) {
                return;
            }
            giveOrDrop(player, items);
            player.sendMessage(plugin.prefixed("&aDu hast &f" + items.size() + " Stapel&a abgeholt."));
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> CollectionGUI.open(plugin, player));
            return;
        }

        Integer pendingIndex = holder.getSlotIndexes().get(slot);
        if (pendingIndex == null) {
            return;
        }
        ItemStack collected = plugin.getStorage().removePendingItemAt(player.getUniqueId(), pendingIndex);
        if (collected == null) {
            player.sendMessage(plugin.prefixed("&cDas war nicht mehr verfügbar - GUI wird aktualisiert."));
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> CollectionGUI.open(plugin, player));
            return;
        }
        returnItem(player, collected);
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> CollectionGUI.open(plugin, player));
    }

    // ---------------------------------------------------------------
    // Hilfsfunktionen
    // ---------------------------------------------------------------

    private void returnRemainingDeliverySlots(Player player, Inventory top) {
        for (int slot : DeliverGUI.DELIVERY_SLOTS) {
            ItemStack item = top.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                returnItem(player, item);
                top.setItem(slot, null);
            }
        }
    }

    private void returnItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack rest : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }
    }

    /**
     * Gibt einem Online-Spieler eine Liste von Items direkt ins Inventar;
     * was nicht reinpasst, wird zu seinen Füßen fallen gelassen statt verloren
     * zu gehen.
     */
    private void giveOrDrop(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            returnItem(player, item);
        }
    }

    /**
     * Teilt eine Gesamtmenge eines Materials in mehrere ItemStacks auf, die
     * jeweils nicht größer als die maximale Stapelgröße des Materials sind.
     */
    private List<ItemStack> splitIntoStacks(Material material, int amount) {
        List<ItemStack> stacks = new ArrayList<>();
        int maxStack = material.getMaxStackSize();
        if (maxStack <= 0) {
            maxStack = 64;
        }
        int remaining = amount;
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);
            stacks.add(new ItemStack(material, stackAmount));
            remaining -= stackAmount;
        }
        return stacks;
    }

    private static String format(double value) {
        return String.format(Locale.GERMANY, "%.2f", value);
    }
}
