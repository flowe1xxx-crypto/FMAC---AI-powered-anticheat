package dev.famesti.fmanticheat.listener;

import dev.famesti.fmanticheat.dataset.CombatDatasetWriter;
import dev.famesti.fmanticheat.detection.PunishmentAnimationService;
import dev.famesti.fmanticheat.monitor.PlayerMonitorService;
import dev.famesti.fmanticheat.monitor.ProbabilityWatchService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class QuitListener implements Listener {

    private final PlayerMonitorService monitorService;
    private final CombatDatasetWriter datasetWriter;
    private final ProbabilityWatchService probabilityWatchService;
    private final PunishmentAnimationService punishmentAnimationService;

    public QuitListener(PlayerMonitorService monitorService, CombatDatasetWriter datasetWriter,
                        ProbabilityWatchService probabilityWatchService,
                        PunishmentAnimationService punishmentAnimationService) {
        this.monitorService = monitorService;
        this.datasetWriter = datasetWriter;
        this.probabilityWatchService = probabilityWatchService;
        this.punishmentAnimationService = punishmentAnimationService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        punishmentAnimationService.handleQuit(player);
        datasetWriter.stop(player, true);
        probabilityWatchService.remove(player);
        monitorService.remove(player);
    }
}
