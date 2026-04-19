package dev.famesti.fmanticheat.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)&\\#([0-9A-F]{6})");

    private ColorUtil() {
    }

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String message = applyHex(text);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }

    public static String gradient(String text, ChatColor... colors) {
        if (text == null || text.isEmpty() || colors.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int printable = 0;
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                printable++;
            }
        }
        if (printable == 0) {
            return text;
        }
        int visibleIndex = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) {
                builder.append(ch);
                continue;
            }
            int paletteIndex = Math.min(colors.length - 1, (visibleIndex * colors.length) / printable);
            builder.append(colors[paletteIndex]).append(ch);
            visibleIndex++;
        }
        return builder.toString();
    }

    public static String progressBar(int percent) {
        int safePercent = Math.max(0, Math.min(100, percent));
        int filled = (int) Math.round(safePercent / 10.0D);
        StringBuilder builder = new StringBuilder();
        builder.append("&8[");
        for (int i = 0; i < 10; i++) {
            builder.append(i < filled ? "&6|" : "&7|");
        }
        builder.append("&8]");
        return colorize(builder.toString());
    }

    public static void action(Player player, String message) {
        ActionBarAdapter.send(player, colorize(message));
    }

    private static String applyHex(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, toSectionHex(matcher.group(1)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String toSectionHex(String hex) {
        StringBuilder builder = new StringBuilder("§x");
        for (int i = 0; i < hex.length(); i++) {
            builder.append('§').append(Character.toLowerCase(hex.charAt(i)));
        }
        return builder.toString();
    }
}
