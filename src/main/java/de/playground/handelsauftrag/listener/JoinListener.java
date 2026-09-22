package de.playground.handelsauftrag.listener;

import de.playground.handelsauftrag.HandelsAuftragPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

/**
 * Liefert beim Join alle Benachrichtigungen nach ("X hat Y für deinen
 * Auftrag geliefert"), die aufgelaufen sind während der Spieler offline war
 * - das Geld war da schon längst ausgezahlt, das hier ist nur die Info.
 */
public final class JoinListener implements Listener {

    private final HandelsAuftragPlugin plugin;

    public JoinListener(HandelsAuftragPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            int pendingItemCount = plugin.getStorage().countPendingItems(player.getUniqueId());

            List<String> messages = plugin.getStorage().popNotifications(player.getUniqueId());
            if (!messages.isEmpty()) {
                player.sendMessage(plugin.prefixed("&7Während deiner Abwesenheit:"));
                for (String msg : messages) {
                    player.sendMessage(HandelsAuftragPlugin.color(msg));
                }
            }

            if (pendingItemCount > 0) {
                player.sendMessage(plugin.prefixed("&eDu hast &f" + pendingItemCount
                        + " Stapel&e in der Abholung liegen! Öffne &f/ha &e-> &fAbholung&e um sie abzuholen."));
            }
        }, 40L);
    }
}
