package de.playground.handelsauftrag.util;

/**
 * Formatiert eine Sekundenzahl als kurze, lesbare Dauer ("2d 5h", "3h 20m",
 * "45m 10s", "30s") - für Anzeigen wie "läuft ab in..." in den GUIs.
 */
public final class TimeFormatUtil {

    private TimeFormatUtil() {
    }

    public static String format(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "abgelaufen";
        }

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
