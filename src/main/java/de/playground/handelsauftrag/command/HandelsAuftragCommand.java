package de.playground.handelsauftrag.command;

import de.playground.handelsauftrag.HandelsAuftragPlugin;
import de.playground.handelsauftrag.gui.BoardGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * /ha (Aliase: /handelsauftrag, /auftrag) öffnet direkt das Board.
 * Keine weiteren Unterbefehle nötig - alles läuft über die GUI.
 */
public final class HandelsAuftragCommand implements CommandExecutor, TabCompleter {

    private final HandelsAuftragPlugin plugin;

    public HandelsAuftragCommand(HandelsAuftragPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können diesen Befehl nutzen.");
            return true;
        }
        if (!player.hasPermission("playground.handelsauftrag.use")) {
            player.sendMessage(plugin.prefixed("&cDazu hast du keine Berechtigung."));
            return true;
        }
        Bukkit.getScheduler().runTask(plugin, () -> BoardGUI.open(plugin, player, 0));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
