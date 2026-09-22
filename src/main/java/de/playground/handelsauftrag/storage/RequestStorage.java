package de.playground.handelsauftrag.storage;

import de.playground.handelsauftrag.model.ItemRequest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Persistiert offene Aufträge, "während offline beliefert"-Text-Benachrichtigungen
 * UND tatsächlich gelieferte Items (die der Auftraggeber noch abholen muss, falls
 * er offline war) in einer requests.yml im Plugin-Datenordner. Alles läuft im
 * Speicher (Map), die Datei wird bei jeder Änderung neu geschrieben - bei der zu
 * erwartenden Anzahl gleichzeitiger Aufträge (Dutzende, nicht Tausende) völlig
 * ausreichend.
 */
public final class RequestStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, ItemRequest> requests = new LinkedHashMap<>();
    private final Map<UUID, List<String>> pendingNotifications = new LinkedHashMap<>();
    private final Map<UUID, List<ItemStack>> pendingItems = new LinkedHashMap<>();

    public RequestStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "requests.yml");
    }

    public void load() {
        requests.clear();
        pendingNotifications.clear();
        pendingItems.clear();

        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection requestsSection = yaml.getConfigurationSection("requests");
        if (requestsSection != null) {
            for (String key : requestsSection.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    ConfigurationSection entry = requestsSection.getConfigurationSection(key);
                    if (entry != null) {
                        ItemRequest request = ItemRequest.fromSection(id, entry);
                        requests.put(id, request);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().log(Level.WARNING, "Ungültiger Auftrags-Eintrag in requests.yml: " + key, e);
                }
            }
        }

        ConfigurationSection notifSection = yaml.getConfigurationSection("pending-notifications");
        if (notifSection != null) {
            for (String key : notifSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(key);
                    List<String> messages = notifSection.getStringList(key);
                    pendingNotifications.put(playerId, new ArrayList<>(messages));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().log(Level.WARNING, "Ungültiger Benachrichtigungs-Eintrag in requests.yml: " + key, e);
                }
            }
        }

        ConfigurationSection itemsSection = yaml.getConfigurationSection("pending-items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(key);
                    List<?> raw = itemsSection.getList(key);
                    List<ItemStack> stacks = new ArrayList<>();
                    if (raw != null) {
                        for (Object obj : raw) {
                            if (obj instanceof ItemStack stack) {
                                stacks.add(stack);
                            }
                        }
                    }
                    if (!stacks.isEmpty()) {
                        pendingItems.put(playerId, stacks);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().log(Level.WARNING, "Ungültiger Item-Eintrag in requests.yml: " + key, e);
                }
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();

        ConfigurationSection requestsSection = yaml.createSection("requests");
        for (ItemRequest request : requests.values()) {
            ConfigurationSection entry = requestsSection.createSection(request.getId().toString());
            request.writeToSection(entry);
        }

        ConfigurationSection notifSection = yaml.createSection("pending-notifications");
        for (Map.Entry<UUID, List<String>> entry : pendingNotifications.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                notifSection.set(entry.getKey().toString(), entry.getValue());
            }
        }

        ConfigurationSection itemsSection = yaml.createSection("pending-items");
        for (Map.Entry<UUID, List<ItemStack>> entry : pendingItems.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                itemsSection.set(entry.getKey().toString(), entry.getValue());
            }
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte requests.yml nicht speichern!", e);
        }
    }

    public synchronized void addRequest(ItemRequest request) {
        requests.put(request.getId(), request);
        save();
    }

    public synchronized void removeRequest(UUID id) {
        requests.remove(id);
        save();
    }

    public synchronized ItemRequest getRequest(UUID id) {
        return requests.get(id);
    }

    /**
     * Alle öffentlich sichtbaren (nicht mehr im Pending-Fenster befindlichen)
     * Aufträge - das ist es, was auf dem Board angezeigt und beliefert werden
     * kann. Frisch erstellte, noch pending Aufträge tauchen hier bewusst nicht
     * auf, damit Vertipper vorher noch storniert werden können.
     */
    public synchronized List<ItemRequest> getAllActive() {
        List<ItemRequest> list = new ArrayList<>();
        for (ItemRequest request : requests.values()) {
            if (!request.isPending()) {
                list.add(request);
            }
        }
        list.sort((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
        return Collections.unmodifiableList(list);
    }

    /**
     * ALLE Aufträge (aktiv + pending), ungefiltert - nur für interne Zwecke wie
     * den ExpiryTask, der auch pending Aufträge auf Ablauf prüfen muss.
     */
    public synchronized List<ItemRequest> getAllRequestsSnapshot() {
        return new ArrayList<>(requests.values());
    }

    public synchronized List<ItemRequest> getByOwner(UUID ownerUuid) {
        List<ItemRequest> list = new ArrayList<>();
        for (ItemRequest request : requests.values()) {
            if (request.getOwnerUuid().equals(ownerUuid)) {
                list.add(request);
            }
        }
        list.sort((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
        return list;
    }

    public synchronized int countActiveByOwner(UUID ownerUuid) {
        return getByOwner(ownerUuid).size();
    }

    /**
     * Reduziert die Restmenge eines Auftrags um "amount" (nicht mehr als vorhanden).
     * Entfernt den Auftrag automatisch, wenn er dadurch vollständig beliefert ist.
     * Gibt die tatsächlich abgezogene Menge zurück.
     */
    public synchronized int reduceRemaining(UUID id, int amount) {
        ItemRequest request = requests.get(id);
        if (request == null) {
            return 0;
        }
        int actual = Math.min(amount, request.getAmountRemaining());
        request.setAmountRemaining(request.getAmountRemaining() - actual);
        if (request.isFulfilled()) {
            requests.remove(id);
        }
        save();
        return actual;
    }

    public synchronized void addNotification(UUID playerUuid, String message) {
        pendingNotifications.computeIfAbsent(playerUuid, k -> new ArrayList<>()).add(message);
        save();
    }

    public synchronized List<String> popNotifications(UUID playerUuid) {
        List<String> messages = pendingNotifications.remove(playerUuid);
        if (messages != null && !messages.isEmpty()) {
            save();
        }
        return messages == null ? Collections.emptyList() : messages;
    }

    /**
     * Hängt tatsächlich gelieferte Items an die Warteliste eines Spielers an -
     * für den Fall, dass er beim Beliefern offline ist/war und die Items nicht
     * direkt ins Inventar gegeben werden konnten.
     */
    public synchronized void addPendingItems(UUID playerUuid, List<ItemStack> items) {
        if (items.isEmpty()) {
            return;
        }
        pendingItems.computeIfAbsent(playerUuid, k -> new ArrayList<>()).addAll(items);
        save();
    }

    public synchronized List<ItemStack> popPendingItems(UUID playerUuid) {
        List<ItemStack> items = pendingItems.remove(playerUuid);
        if (items != null && !items.isEmpty()) {
            save();
        }
        return items == null ? Collections.emptyList() : items;
    }

    /**
     * Zeigt die wartenden Items eines Spielers an, OHNE sie zu entfernen -
     * für die Abholungs-GUI (die entscheidet pro Klick, was wirklich abgeholt wird).
     */
    public synchronized List<ItemStack> getPendingItems(UUID playerUuid) {
        List<ItemStack> items = pendingItems.get(playerUuid);
        return items == null ? Collections.emptyList() : new ArrayList<>(items);
    }

    public synchronized int countPendingItems(UUID playerUuid) {
        List<ItemStack> items = pendingItems.get(playerUuid);
        return items == null ? 0 : items.size();
    }

    /**
     * Entfernt genau einen wartenden Item-Stapel (per Index in der Liste, wie
     * sie die Abholungs-GUI gerade anzeigt) und gibt ihn zurück, oder null
     * falls der Index nicht mehr gültig ist (z.B. Liste hat sich zwischenzeitlich
     * geändert).
     */
    public synchronized ItemStack removePendingItemAt(UUID playerUuid, int index) {
        List<ItemStack> items = pendingItems.get(playerUuid);
        if (items == null || index < 0 || index >= items.size()) {
            return null;
        }
        ItemStack removed = items.remove(index);
        if (items.isEmpty()) {
            pendingItems.remove(playerUuid);
        }
        save();
        return removed;
    }
}
