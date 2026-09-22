package de.playground.handelsauftrag.listener;

import de.playground.handelsauftrag.HandelsAuftragPlugin;
import de.playground.handelsauftrag.model.ItemRequest;
import de.playground.handelsauftrag.model.PendingCreation;
import de.playground.handelsauftrag.util.TimeFormatUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Locale;
import java.util.UUID;

/**
 * Zustandsmaschine für die Chat-Eingabe von Menge und Preis, nachdem der
 * Spieler im CreateRequestGUI ein Material gewählt hat. Bei Bestätigung des
 * Preises wird das Geld sofort (Escrow) abgebucht und der Auftrag erstellt.
 */
public final class ChatInputListener implements Listener {

    private final HandelsAuftragPlugin plugin;

    public ChatInputListener(HandelsAuftragPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PendingCreation pending = plugin.getPendingCreations().get(uuid);
        if (pending == null) {
            return;
        }

        event.setCancelled(true);
        String raw = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        Bukkit.getScheduler().runTask(plugin, () -> handleInput(player, pending, raw));
    }

    private void handleInput(Player player, PendingCreation pending, String raw) {
        // Sicherstellen, dass der Vorgang zwischenzeitlich nicht abgebrochen/ersetzt wurde.
        if (plugin.getPendingCreations().get(player.getUniqueId()) != pending) {
            return;
        }

        if (raw.equalsIgnoreCase("abbrechen") || raw.equalsIgnoreCase("cancel")) {
            plugin.getPendingCreations().remove(player.getUniqueId());
            player.sendMessage(plugin.prefixed("&cAuftrag-Erstellung abgebrochen."));
            return;
        }

        if (pending.getStage() == PendingCreation.Stage.AWAIT_AMOUNT) {
            handleAmount(player, pending, raw);
        } else if (pending.getStage() == PendingCreation.Stage.AWAIT_PRICE) {
            handlePrice(player, pending, raw);
        }
    }

    private void handleAmount(Player player, PendingCreation pending, String raw) {
        int amount;
        try {
            amount = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.prefixed("&cBitte gib eine ganze Zahl ein. (&7'abbrechen' zum Abbrechen&c)"));
            return;
        }
        if (amount < plugin.getMinAmount() || amount > plugin.getMaxAmount()) {
            player.sendMessage(plugin.prefixed("&cMenge muss zwischen &f" + plugin.getMinAmount()
                    + "&c und &f" + plugin.getMaxAmount() + "&c liegen."));
            return;
        }

        pending.setAmount(amount);
        pending.setStage(PendingCreation.Stage.AWAIT_PRICE);
        player.sendMessage(plugin.prefixed("&aMenge: &f" + amount));
        player.sendMessage(plugin.prefixed("&eSchreibe jetzt den &fPreis pro Stück&e in den Chat (z.B. 0.5)."));
    }

    private void handlePrice(Player player, PendingCreation pending, String raw) {
        double price;
        try {
            price = Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.prefixed("&cBitte gib eine gültige Zahl ein. (&7'abbrechen' zum Abbrechen&c)"));
            return;
        }
        if (price < plugin.getMinPricePerUnit()) {
            player.sendMessage(plugin.prefixed("&cPreis pro Stück muss mindestens &f"
                    + String.format(Locale.GERMANY, "%.2f", plugin.getMinPricePerUnit()) + "&c betragen."));
            return;
        }

        double total = price * pending.getAmount();

        if (plugin.getStorage().countActiveByOwner(player.getUniqueId()) >= plugin.getMaxActiveRequestsPerPlayer()) {
            plugin.getPendingCreations().remove(player.getUniqueId());
            player.sendMessage(plugin.prefixed("&cDu hast in der Zwischenzeit die maximale Anzahl offener Aufträge erreicht."));
            return;
        }

        if (!plugin.getEconomy().has(player, total)) {
            plugin.getPendingCreations().remove(player.getUniqueId());
            player.sendMessage(plugin.prefixed("&cDu hast nicht genug Geld. Benötigt: &f"
                    + String.format(Locale.GERMANY, "%.2f", total)));
            return;
        }

        EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, total);
        if (!response.transactionSuccess()) {
            plugin.getPendingCreations().remove(player.getUniqueId());
            player.sendMessage(plugin.prefixed("&cGeld konnte nicht abgebucht werden: " + response.errorMessage));
            return;
        }

        int pendingSeconds = plugin.getPendingSeconds();
        long now = System.currentTimeMillis();
        long activationTime = now + (pendingSeconds * 1000L);
        long expiresAt = now + (long) (plugin.getRequestLifetimeHours() * 3600_000L);

        UUID requestId = UUID.randomUUID();
        ItemRequest request = new ItemRequest(requestId, player.getUniqueId(), player.getName(),
                pending.getMaterial(), pending.getAmount(), pending.getAmount(), price, now, activationTime,
                expiresAt);
        plugin.getStorage().addRequest(request);
        plugin.getPendingCreations().remove(player.getUniqueId());

        player.sendMessage(plugin.prefixed("&aAuftrag erstellt: &f" + pending.getAmount() + "x "
                + pending.getMaterial().name() + " &afür &f" + String.format(Locale.GERMANY, "%.2f", price)
                + " &apro Stück (&f" + String.format(Locale.GERMANY, "%.2f", total) + " &agesamt hinterlegt)."));

        if (pendingSeconds > 0) {
            player.sendMessage(plugin.prefixed("&7Er wird in &f" + pendingSeconds + " Sekunden&7 veröffentlicht. "
                    + "Falls du dich vertippt hast, kannst du ihn bis dahin über &f/ha &7-> &fMeine Aufträge&7 stornieren."));
        }
        player.sendMessage(plugin.prefixed("&7Er läuft automatisch nach &f"
                + TimeFormatUtil.format((long) (plugin.getRequestLifetimeHours() * 3600))
                + " &7ab, falls er bis dahin nicht vollständig beliefert wurde (Restgeld wird dann erstattet)."));

        Bukkit.getScheduler().runTaskLater(plugin, () -> announceIfStillActive(requestId), Math.max(1L, pendingSeconds * 20L));
    }

    private void announceIfStillActive(UUID requestId) {
        ItemRequest request = plugin.getStorage().getRequest(requestId);
        if (request == null) {
            // Wurde in der Zwischenzeit storniert - keine Ankündigung.
            return;
        }

        String announcement = plugin.prefixed("&a" + request.getOwnerName() + " &7sucht &f"
                + request.getAmountRemaining() + "x " + request.getMaterial().name() + " &7für &f"
                + String.format(Locale.GERMANY, "%.2f", request.getPricePerUnit()) + " &7pro Stück &8(&7Gesamtwert: &f"
                + String.format(Locale.GERMANY, "%.2f", request.getRemainingValue()) + "&8) &7- &f/ha");

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(announcement);
        }
    }
}
