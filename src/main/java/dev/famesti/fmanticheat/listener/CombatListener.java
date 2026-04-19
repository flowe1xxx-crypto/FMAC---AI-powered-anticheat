package dev.famesti.fmanticheat.listener;

import dev.famesti.fmanticheat.config.Settings;
import dev.famesti.fmanticheat.dataset.CombatDatasetWriter;
import dev.famesti.fmanticheat.monitor.PlayerCombatSnapshot;
import dev.famesti.fmanticheat.monitor.PlayerMonitorService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;

public final class CombatListener implements Listener {

    private final Settings settings;
    private final PlayerMonitorService monitorService;
    private final CombatDatasetWriter datasetWriter;

    public CombatListener(Settings settings, PlayerMonitorService monitorService, CombatDatasetWriter datasetWriter) {
        this.settings = settings;
        this.monitorService = monitorService;
        this.datasetWriter = datasetWriter;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        monitorService.getOrCreate(event.getPlayer()).registerClick(System.currentTimeMillis());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player attacker = (Player) event.getDamager();
        Entity target = event.getEntity();
        PlayerCombatSnapshot snapshot = monitorService.getOrCreate(attacker);
        snapshot.registerClick(System.currentTimeMillis());
        snapshot.registerHit(target, settings.getDatasets().getCombatWindowTicks());
        if (datasetWriter.isRecording(attacker)) {
            datasetWriter.updateActionBar(attacker);
        }
    }
}
