package dev.famesti.fmanticheat.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public final class ActionBarAdapter {

    private ActionBarAdapter() {
    }

    public static void send(Player player, String message) {
        if (player == null || !player.isOnline()) {
            return;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
}
