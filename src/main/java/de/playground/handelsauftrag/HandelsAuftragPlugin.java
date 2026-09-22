package de.playground.handelsauftrag;

import de.playground.handelsauftrag.command.HandelsAuftragCommand;
import de.playground.handelsauftrag.gui.GuiListener;
import de.playground.handelsauftrag.listener.ChatInputListener;
import de.playground.handelsauftrag.listener.JoinListener;
import de.playground.handelsauftrag.model.PendingCreation;
import de.playground.handelsauftrag.storage.RequestStorage;
import de.playground.handelsauftrag.task.ExpiryTask;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HandelsAuftragPlugin extends JavaPlugin {

    private Economy economy;
    private RequestStorage storage;

    private int maxActiveRequestsPerPlayer;
    private double minPricePerUnit;
    private int minAmount;
    private int maxAmount;

    private String boardTitle;
    private String createTitle;
    private String myRequestsTitle;
    private String deliverTitle;
    private String collectionTitle;

    private String prefixRaw;
    private int pendingSeconds;
    private double requestLifetimeHours;
    private long expiryCheckIntervalMinutes;

    private BukkitTask expiryTask;

    private final Map<UUID, PendingCreation> pendingCreations = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfigValues();

        if (!setupEconomy()) {
            getLogger().severe("Kein Vault-Economy-Provider gefunden (fehlt Vault oder ein Economy-Plugin wie EssentialsX)! "
                    + "HandelsAuftrag wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        storage = new RequestStorage(this);
        storage.load();

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        HandelsAuftragCommand executor = new HandelsAuftragCommand(this);
        getCommand("ha").setExecutor(executor);
        getCommand("ha").setTabCompleter(executor);

        long intervalTicks = Math.max(20L, expiryCheckIntervalMinutes * 60L * 20L);
        expiryTask = new ExpiryTask(this).runTaskTimer(this, intervalTicks, intervalTicks);

        getLogger().info("HandelsAuftrag aktiviert. Aktive Aufträge geladen: " + storage.getAllActive().size());
    }

    @Override
    public void onDisable() {
        if (expiryTask != null) {
            expiryTask.cancel();
        }
        if (storage != null) {
            storage.save();
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void reloadConfigValues() {
        FileConfiguration cfg = getConfig();
        maxActiveRequestsPerPlayer = cfg.getInt("max-active-requests-per-player", 5);
        minPricePerUnit = cfg.getDouble("min-price-per-unit", 0.01);
        minAmount = cfg.getInt("min-amount", 1);
        maxAmount = cfg.getInt("max-amount", 100000);
        boardTitle = color(cfg.getString("board-title", "&8Handelsauftrag-Board"));
        createTitle = color(cfg.getString("create-title", "&8Auftrag erstellen - Item wählen"));
        myRequestsTitle = color(cfg.getString("my-requests-title", "&8Meine Aufträge"));
        deliverTitle = color(cfg.getString("deliver-title", "&8Lieferung bestätigen"));
        collectionTitle = color(cfg.getString("collection-title", "&8Abholung"));

        prefixRaw = cfg.getString("prefix", "&aPlay&bGround &7» &r");
        pendingSeconds = Math.max(0, cfg.getInt("pending-seconds", 30));

        requestLifetimeHours = cfg.getDouble("request-lifetime-hours", 48.0);
        if (requestLifetimeHours <= 0) {
            requestLifetimeHours = 48.0;
        }
        expiryCheckIntervalMinutes = Math.max(1, cfg.getInt("expiry-check-interval-minutes", 5));
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Übersetzt sowohl normale &-Codes als auch &#RRGGBB-Hexfarben (für
     * Farbverläufe wie im Playground-Prefix) - ChatColor#translateAlternateColorCodes
     * kann von Haus aus nur einfache &-Codes, kein Hex.
     */
    public static String color(String s) {
        if (s == null) {
            return "";
        }
        Matcher matcher = HEX_PATTERN.matcher(s);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : matcher.group(1).toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * Für alle tatsächlichen Chat-Nachrichten des Plugins (nicht GUI-Titel/Item-
     * Texte) - hängt den konfigurierten "[Playground]"-Prefix konsistent davor.
     */
    public String prefixed(String raw) {
        return color(prefixRaw + raw);
    }

    public Economy getEconomy() {
        return economy;
    }

    public RequestStorage getStorage() {
        return storage;
    }

    public int getMaxActiveRequestsPerPlayer() {
        return maxActiveRequestsPerPlayer;
    }

    public double getMinPricePerUnit() {
        return minPricePerUnit;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public String getBoardTitle() {
        return boardTitle;
    }

    public String getCreateTitle() {
        return createTitle;
    }

    public String getMyRequestsTitle() {
        return myRequestsTitle;
    }

    public String getDeliverTitle() {
        return deliverTitle;
    }

    public String getCollectionTitle() {
        return collectionTitle;
    }

    public int getPendingSeconds() {
        return pendingSeconds;
    }

    public double getRequestLifetimeHours() {
        return requestLifetimeHours;
    }

    public long getExpiryCheckIntervalMinutes() {
        return expiryCheckIntervalMinutes;
    }

    public Map<UUID, PendingCreation> getPendingCreations() {
        return pendingCreations;
    }
}
