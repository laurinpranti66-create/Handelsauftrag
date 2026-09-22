package de.playground.handelsauftrag.task;

import de.playground.handelsauftrag.HandelsAuftragPlugin;
import de.playground.handelsauftrag.model.ItemRequest;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Locale;

/**
 * Läuft periodisch und storniert Aufträge, deren Lebenszeit abgelaufen ist -
 * das noch hinterlegte Restgeld geht per Vault zurück an den Ersteller, auch
 * wenn der gerade offline ist. Ohne das würde das Board sich mit vergessenen
 * Aufträgen von längst inaktiven Spielern zumüllen.
 */
public final class ExpiryTask extends BukkitRunnable {

    private final HandelsAuftragPlugin plugin;

    public ExpiryTask(HandelsAuftragPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        List<ItemRequest> all = plugin.getStorage().getAllRequestsSnapshot();
        for (ItemRequest request : all) {
            if (!request.isExpired()) {
                continue;
            }

            double refund = request.getRemainingValue();
            OfflinePlayer owner = Bukkit.getOfflinePlayer(request.getOwnerUuid());

            EconomyResponse response = plugin.getEconomy().depositPlayer(owner, refund);
            if (!response.transactionSuccess()) {
                plugin.getLogger().warning("Ablauf-Rückerstattung an " + request.getOwnerName()
                        + " fehlgeschlagen (" + response.errorMessage + ") - Auftrag " + request.getId()
                        + " wird beim nächsten Durchlauf erneut versucht.");
                continue;
            }

            plugin.getStorage().removeRequest(request.getId());

            String message = "&6Dein Auftrag &f" + request.getAmountRemaining() + "x "
                    + request.getMaterial().name() + " &6ist nach Ablauf der Zeit automatisch storniert worden. &f"
                    + String.format(Locale.GERMANY, "%.2f", refund) + " &6wurden erstattet.";

            Player online = owner.getPlayer();
            if (online != null) {
                online.sendMessage(plugin.prefixed(message));
            } else {
                plugin.getStorage().addNotification(request.getOwnerUuid(), message);
            }
        }
    }
}
