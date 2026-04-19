package dev.famesti.fmanticheat.detection;

import dev.famesti.fmanticheat.config.Settings;
import dev.famesti.fmanticheat.ui.InterfaceTheme;
import dev.famesti.fmanticheat.util.ColorUtil;
import dev.famesti.fmanticheat.util.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AlertService {

    private final JavaPlugin plugin;
    private final Settings settings;
    private final PunishmentAnimationService punishmentAnimationService;
    private final Set<UUID> disabledAlerts = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> violationLevels = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Long> lastAlerts = new ConcurrentHashMap<UUID, Long>();

    public AlertService(JavaPlugin plugin, Settings settings, PunishmentAnimationService punishmentAnimationService) {
        this.plugin = plugin;
        this.settings = settings;
        this.punishmentAnimationService = punishmentAnimationService;
    }

    public boolean toggleAlerts(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player) sender;
        UUID id = player.getUniqueId();
        if (disabledAlerts.contains(id)) {
            disabledAlerts.remove(id);
            return true;
        }
        disabledAlerts.add(id);
        return false;
    }

    public void alert(Player suspect, double probability, String reason) {
        if (suspect == null || punishmentAnimationService.isAnimating(suspect)) {
            return;
        }
        UUID suspectId = suspect.getUniqueId();
        int currentVl = violationLevels.containsKey(suspectId) ? violationLevels.get(suspectId) : 0;
        SanctionPolicy.Decision decision = buildPolicy().resolve(probability, currentVl);
        if (!decision.isSuspicious() || (!decision.shouldPunish() && decision.getViolationGain() <= 0)) {
            return;
        }
        long now = System.currentTimeMillis();
        Long previous = lastAlerts.get(suspectId);
        if (!decision.isTerminal() && previous != null
                && now - previous < settings.getChecks().getCooldownBetweenAlertsMs()) {
            return;
        }
        lastAlerts.put(suspectId, now);
        int updatedVl = decision.getUpdatedViolationLevel();
        violationLevels.put(suspectId, updatedVl);
        String message = settings.getMessages().getAlertFormat()
                .replace("%player%", suspect.getName())
                .replace("%prob%", FormatUtil.percent(probability))
                .replace("%reason%", reason + " | "
                        + InterfaceTheme.formatAlertSuffix(
                        decision.getProbabilityPercent(),
                        updatedVl,
                        decision.getViolationGain(),
                        decision.isTerminal()
                ));

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("fmanticheat.notify") && !disabledAlerts.contains(online.getUniqueId())) {
                ColorUtil.send(online, message);
            }
        }
        PunishmentMode mode = decision.isTerminal() ? decision.getPreferredMode() : getPunishmentMode();
        plugin.getLogger().warning("[ALERT] " + suspect.getName()
                + " probability=" + FormatUtil.percent(probability)
                + "% reason=" + reason
                + " vl=" + updatedVl + "/100"
                + " terminal=" + decision.isTerminal()
                + " mode=" + mode.getId());
        if (settings.getChecks().isPunishEnabled() && decision.shouldPunish() && mode != PunishmentMode.ALERT) {
            if (punishmentAnimationService.start(suspect, probability, reason, mode, updatedVl)) {
                clearViolations(suspectId);
            }
        }
    }

    public PunishmentMode getPunishmentMode() {
        return PunishmentMode.parse(settings.getChecks().getPunishmentMode());
    }

    public PunishmentMode setPunishmentMode(String rawMode) {
        PunishmentMode mode = PunishmentMode.parse(rawMode);
        plugin.getConfig().set("checks.punishment-mode", mode.getId());
        plugin.saveConfig();
        settings.reload();
        return mode;
    }

    public void clearViolations(Player player) {
        if (player != null) {
            clearViolations(player.getUniqueId());
        }
    }

    public void clearViolations(UUID playerId) {
        if (playerId == null) {
            return;
        }
        violationLevels.remove(playerId);
        lastAlerts.remove(playerId);
    }

    private SanctionPolicy buildPolicy() {
        Settings.CheckSection checks = settings.getChecks();
        return new SanctionPolicy(
                checks.getWatchThresholdPercent(),
                checks.getWatchThresholdPercent(),
                checks.getMediumThresholdPercent(),
                checks.getTerminalThresholdPercent(),
                checks.getWatchViolationGain(),
                checks.getMediumViolationGain()
        );
    }
}
