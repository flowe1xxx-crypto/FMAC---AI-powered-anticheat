package dev.famesti.fmanticheat.listener;

import dev.famesti.fmanticheat.detection.PunishmentAnimationService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class JoinListener implements Listener {

    private final PunishmentAnimationService punishmentAnimationService;

    public JoinListener(PunishmentAnimationService punishmentAnimationService) {
        this.punishmentAnimationService = punishmentAnimationService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        punishmentAnimationService.handleJoin(event.getPlayer());
    }
}
