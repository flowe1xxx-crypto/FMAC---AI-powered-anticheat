package dev.famesti.fmanticheat.ui;

import dev.famesti.fmanticheat.ml.ModelRepository;
import dev.famesti.fmanticheat.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public final class BannerPrinter {

    private BannerPrinter() {
    }

    public static void print(ModelRepository modelRepository) {
        String[] lines = new String[]{
                "FFFFFFFFF  M     M",
                "FF         MM   MM",
                "FFFFF      M M M M",
                "FF         M  M  M",
                "FF         M     M"
        };
        Bukkit.getConsoleSender().sendMessage(ColorUtil.colorize("&#1f2235&l&m============================================================"));
        for (String line : lines) {
            Bukkit.getConsoleSender().sendMessage(ColorUtil.gradient(line, ChatColor.DARK_GRAY, ChatColor.LIGHT_PURPLE, ChatColor.RED, ChatColor.GOLD));
        }
        Bukkit.getConsoleSender().sendMessage(ColorUtil.colorize("&#f8f9fa&lFM AntiCheat &8| &#adb5bdminimal startup profile"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.colorize(InterfaceTheme.formatModelLine(
                "pipeline",
                "filter.dat -> cleaner.dat -> verifier.dat -> main.dat"
        )));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.colorize(InterfaceTheme.formatModelLine(
                "model",
                "Famesti-publik-45k / params=" + modelRepository.getMainModel().parameterCount()
        )));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.colorize(InterfaceTheme.formatModelLine(
                "detect",
                "Kill-Aim, Aimbot, aim-lock heuristics"
        )));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.colorize("&#1f2235&l&m============================================================"));
    }
}
