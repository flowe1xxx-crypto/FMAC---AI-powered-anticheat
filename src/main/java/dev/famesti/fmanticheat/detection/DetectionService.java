package dev.famesti.fmanticheat.detection;

import dev.famesti.fmanticheat.config.Settings;
import dev.famesti.fmanticheat.dataset.CombatFeatureFrame;
import dev.famesti.fmanticheat.ml.ModelPipeline;
import dev.famesti.fmanticheat.monitor.PlayerCombatSnapshot;
import dev.famesti.fmanticheat.monitor.PlayerMonitorService;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class DetectionService {

    private final JavaPlugin plugin;
    private final Settings settings;
    private final ModelPipeline modelPipeline;
    private final PlayerMonitorService monitorService;
    private final AlertService alertService;

    public DetectionService(JavaPlugin plugin, Settings settings, ModelPipeline modelPipeline,
                            PlayerMonitorService monitorService, AlertService alertService) {
        this.plugin = plugin;
        this.settings = settings;
        this.modelPipeline = modelPipeline;
        this.monitorService = monitorService;
        this.alertService = alertService;
    }

    public double evaluate(Player player) {
        if (!settings.getChecks().isEnabled()) {
            return 0.0D;
        }
        PlayerCombatSnapshot snapshot = monitorService.getOrCreate(player);
        long now = System.currentTimeMillis();
        if (!snapshot.canEvaluate(now, settings.getChecks().getMinimumPredictionIntervalMs())) {
            return snapshot.getLatestProbability();
        }
        HeuristicResult heuristicResult = evaluateHeuristics(snapshot);
        double model = modelPipeline.evaluate(snapshot);
        double calibratedModel = calibrateModel(model);
        double rawProbability = Math.max(
                Math.min(1.0D, calibratedModel * 0.98D + heuristicResult.score * 0.24D),
                Math.max(calibratedModel * 1.03D, heuristicResult.score * 0.90D)
        );
        if (heuristicResult.score < 0.08D && snapshot.getAimLockTicks() == 0 && calibratedModel < 0.72D) {
            rawProbability *= 0.97D;
        }
        if (snapshot.isInCombatWindow()) {
            snapshot.pushSuspicion(rawProbability);
        } else {
            snapshot.coolDownSuspicion();
        }
        double probability = Math.min(1.0D, Math.max(rawProbability, rawProbability * 0.96D + snapshot.getSuspicionBuffer() * 0.20D));
        probability = snapshot.updateSmoothedProbability(probability, settings.getChecks().getProbabilitySmoothingFactor());
        snapshot.setLatestProbability(probability);
        snapshot.setLatestReason(buildReason(model, calibratedModel, heuristicResult, snapshot));
        boolean stableSuspicion = snapshot.getSuspicionBuffer() >= 0.05D;
        boolean strongEvidence = heuristicResult.score >= 0.05D || calibratedModel >= 0.64D || model >= 0.72D;
        double alertThreshold = settings.getChecks().getThreshold();
        boolean suspiciousNow = probability >= alertThreshold && stableSuspicion && strongEvidence;
        int streak = snapshot.updateStableAlertStreak(suspiciousNow);
        if (suspiciousNow && streak >= settings.getChecks().getMinimumAlertStreak()) {
            alertService.alert(player, probability, snapshot.getLatestReason());
            if (snapshot.isDebug()) {
                player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation().add(0.0D, 1.0D, 0.0D), 6, 0.25D, 0.4D, 0.25D, 0.01D);
            }
        }
        return probability;
    }

    public double getProbability(Player player) {
        return monitorService.getOrCreate(player).getLatestProbability();
    }

    private HeuristicResult evaluateHeuristics(PlayerCombatSnapshot snapshot) {
        if (snapshot.getRecentFrames().isEmpty()) {
            return new HeuristicResult();
        }
        double snap = 0.0D;
        double smoothness = 0.0D;
        double consistency = 0.0D;
        double cps = 0.0D;
        double silentAim = 0.0D;
        double microCorrection = 0.0D;
        double aimLockPressure = 0.0D;
        double maxSnap = 0.0D;
        int size = 0;
        for (CombatFeatureFrame frame : snapshot.getRecentFrames()) {
            double absYaw = Math.abs(frame.getYawDelta());
            double absPitch = Math.abs(frame.getPitchDelta());
            snap += absYaw >= settings.getChecks().getRotationSnapThreshold() ? 1.0D : 0.0D;
            smoothness += frame.getSmoothness();
            consistency += frame.getTrackingConsistency();
            cps += frame.getCps();
            maxSnap = Math.max(maxSnap, absYaw);
            if (frame.getTrackingConsistency() > 0.965D && absYaw + absPitch <= settings.getChecks().getSilentAimAngleThreshold()) {
                silentAim += 1.0D;
            }
            if (frame.getTrackingConsistency() > 0.94D && absYaw > 0.05D && absYaw < 1.35D && absPitch < 0.95D) {
                microCorrection += 1.0D;
            }
            if (frame.getTrackingConsistency() > settings.getChecks().getTrackingConsistencyThreshold()
                    && frame.getTargetDistance() > 1.1F && frame.getTargetDistance() < 4.6F) {
                aimLockPressure += 1.0D;
            }
            size++;
        }
        if (size == 0) {
            return new HeuristicResult();
        }
        snap /= size;
        smoothness /= size;
        consistency /= size;
        cps /= size;
        silentAim /= size;
        microCorrection /= size;
        aimLockPressure /= size;
        if (consistency > settings.getChecks().getTrackingConsistencyThreshold()) {
            snapshot.setAimLockTicks(snapshot.getAimLockTicks() + 1);
        } else {
            snapshot.setAimLockTicks(0);
        }
        double score = 0.0D;
        score += snap * 0.32D;
        score += Math.max(0.0D, smoothness - settings.getChecks().getImpossibleSmoothnessThreshold()) * 1.9D;
        score += Math.max(0.0D, consistency - settings.getChecks().getTrackingConsistencyThreshold()) * 1.35D;
        score += Math.max(0.0D, (cps - settings.getChecks().getCpsHardLimit()) / 8.0D);
        score += silentAim * 0.55D;
        score += microCorrection * 0.34D;
        score += aimLockPressure * 0.24D;
        if (snapshot.getAimLockTicks() >= settings.getChecks().getAimLockDurationTicks()) {
            score += 0.22D;
        }
        HeuristicResult result = new HeuristicResult();
        result.score = Math.min(1.0D, score);
        result.snap = snap;
        result.smoothness = smoothness;
        result.consistency = consistency;
        result.cps = cps;
        result.silentAim = silentAim;
        result.microCorrection = microCorrection;
        result.maxSnap = maxSnap;
        return result;
    }

    private String buildReason(double model, double calibratedModel, HeuristicResult result, PlayerCombatSnapshot snapshot) {
        StringBuilder reason = new StringBuilder();
        if (result.snap > 0.18D || result.maxSnap >= settings.getChecks().getRotationSnapThreshold()) {
            reason.append("snap,");
        }
        if (result.silentAim > 0.12D) {
            reason.append("silent,");
        }
        if (result.microCorrection > 0.18D) {
            reason.append("micro,");
        }
        if (snapshot.getAimLockTicks() >= settings.getChecks().getAimLockDurationTicks()) {
            reason.append("lock,");
        }
        if (result.smoothness > settings.getChecks().getImpossibleSmoothnessThreshold()) {
            reason.append("smooth,");
        }
        if (reason.length() == 0) {
            reason.append("tracking,");
        }
        reason.append(" rnn=").append(format(model));
        reason.append(" cal=").append(format(calibratedModel));
        reason.append(" heur=").append(format(result.score));
        return reason.toString();
    }

    private double calibrateModel(double model) {
        if (model <= 0.40D) {
            return model * 0.92D;
        }
        if (model <= 0.78D) {
            return model * 0.98D;
        }
        return Math.min(1.0D, model * 0.995D + 0.01D);
    }

    private String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static final class HeuristicResult {
        private double score;
        private double snap;
        private double smoothness;
        private double consistency;
        private double cps;
        private double silentAim;
        private double microCorrection;
        private double maxSnap;
    }
}
