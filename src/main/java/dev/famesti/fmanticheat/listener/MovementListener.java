package dev.famesti.fmanticheat.listener;

import dev.famesti.fmanticheat.detection.PunishmentAnimationService;
import dev.famesti.fmanticheat.monitor.PlayerMonitorService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class MovementListener implements Listener {

    private final PlayerMonitorService monitorService;
    private final PunishmentAnimationService punishmentAnimationService;

    public MovementListener(PlayerMonitorService monitorService, PunishmentAnimationService punishmentAnimationService) {
        this.monitorService = monitorService;
        this.punishmentAnimationService = punishmentAnimationService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (punishmentAnimationService.isAnimating(player)) {
            event.setTo(punishmentAnimationService.lockMovement(player, event.getFrom(), event.getTo()));
            return;
        }
        monitorService.getOrCreate(player);
    }
}
