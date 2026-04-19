package dev.famesti.fmanticheat.monitor;

import dev.famesti.fmanticheat.dataset.CombatFeatureFrame;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public final class PlayerCombatSnapshot {

    private final UUID playerId;
    private final Deque<Long> clickTimes = new ArrayDeque<Long>();
    private final Deque<CombatFeatureFrame> recentFrames = new ArrayDeque<CombatFeatureFrame>();
    private Location lastLocation;
    private Vector lastVelocity = new Vector();
    private float lastYaw;
    private float lastPitch;
    private float lastYawDelta;
    private float lastPitchDelta;
    private float lastAcceleration;
    private Entity currentTarget;
    private long lastHitMillis;
    private int combatTicks;
    private int aimLockTicks;
    private double latestProbability;
    private double smoothedProbability;
    private double suspicionBuffer;
    private String latestReason = "warmup";
    private long lastEvaluationMillis;
    private int stableAlertStreak;
    private boolean debug;

    public PlayerCombatSnapshot(Player player) {
        this.playerId = player.getUniqueId();
        this.lastLocation = player.getLocation().clone();
        this.lastVelocity = player.getVelocity().clone();
        this.lastYaw = lastLocation.getYaw();
        this.lastPitch = lastLocation.getPitch();
    }

    public void updateMovement(Player player) {
        Location next = player.getLocation();
        Vector velocity = player.getVelocity();
        this.lastYawDelta = wrap(next.getYaw() - lastYaw);
        this.lastPitchDelta = next.getPitch() - lastPitch;
        this.lastAcceleration = (float) velocity.clone().subtract(lastVelocity).length();
        this.lastVelocity = velocity.clone();
        this.lastYaw = next.getYaw();
        this.lastPitch = next.getPitch();
        this.lastLocation = next.clone();
        if (combatTicks > 0) {
            combatTicks--;
        }
    }

    public void registerClick(long now) {
        clickTimes.addLast(now);
        while (!clickTimes.isEmpty() && now - clickTimes.peekFirst() > 1000L) {
            clickTimes.removeFirst();
        }
    }

    public void registerHit(Entity target, int combatWindowTicks) {
        this.currentTarget = target;
        this.lastHitMillis = System.currentTimeMillis();
        this.combatTicks = Math.max(combatTicks, combatWindowTicks);
    }

    public boolean isInCombatWindow() {
        return combatTicks > 0 && currentTarget != null && currentTarget.isValid();
    }

    public float getCps() {
        return (float) clickTimes.size();
    }

    public float getLastYawDelta() { return lastYawDelta; }
    public float getLastPitchDelta() { return lastPitchDelta; }
    public float getLastAcceleration() { return lastAcceleration; }
    public Location getLastLocation() { return lastLocation; }
    public Vector getLastVelocity() { return lastVelocity; }
    public Entity getCurrentTarget() { return currentTarget; }
    public long getLastHitMillis() { return lastHitMillis; }
    public UUID getPlayerId() { return playerId; }

    public void pushFrame(CombatFeatureFrame frame, int maxHistory) {
        recentFrames.addLast(frame);
        while (recentFrames.size() > maxHistory) {
            recentFrames.removeFirst();
        }
    }

    public Deque<CombatFeatureFrame> getRecentFrames() {
        return recentFrames;
    }

    public int getAimLockTicks() { return aimLockTicks; }

    public void setAimLockTicks(int aimLockTicks) {
        this.aimLockTicks = aimLockTicks;
    }

    public double getLatestProbability() { return latestProbability; }

    public void setLatestProbability(double latestProbability) {
        this.latestProbability = latestProbability;
    }

    public double updateSmoothedProbability(double rawProbability, double blendFactor) {
        double factor = Math.max(0.0D, Math.min(1.0D, blendFactor));
        double clampedRawProbability = Math.max(0.0D, Math.min(1.0D, rawProbability));
        if (smoothedProbability <= 0.0D) {
            smoothedProbability = clampedRawProbability;
        } else {
            smoothedProbability = smoothedProbability * (1.0D - factor) + clampedRawProbability * factor;
        }
        smoothedProbability = Math.max(0.0D, Math.min(1.0D, smoothedProbability));
        latestProbability = smoothedProbability;
        return smoothedProbability;
    }

    public boolean canEvaluate(long now, long minimumIntervalMs) {
        if (lastEvaluationMillis > 0L && now - lastEvaluationMillis < minimumIntervalMs) {
            return false;
        }
        lastEvaluationMillis = now;
        return true;
    }

    public double getSuspicionBuffer() {
        return suspicionBuffer;
    }

    public void pushSuspicion(double sample) {
        this.suspicionBuffer = Math.max(0.0D, Math.min(1.0D, this.suspicionBuffer * 0.80D + sample * 0.22D));
    }

    public void coolDownSuspicion() {
        this.suspicionBuffer *= 0.84D;
        if (this.suspicionBuffer < 0.001D) {
            this.suspicionBuffer = 0.0D;
        }
    }

    public String getLatestReason() { return latestReason; }

    public void setLatestReason(String latestReason) {
        this.latestReason = latestReason;
    }

    public int updateStableAlertStreak(boolean suspicious) {
        if (suspicious) {
            stableAlertStreak++;
        } else {
            stableAlertStreak = Math.max(0, stableAlertStreak - 1);
        }
        return stableAlertStreak;
    }

    public int getStableAlertStreak() {
        return stableAlertStreak;
    }

    public boolean isDebug() { return debug; }

    public void setDebug(boolean debug) { this.debug = debug; }

    private float wrap(float angle) {
        float value = angle;
        while (value <= -180.0F) {
            value += 360.0F;
        }
        while (value > 180.0F) {
            value -= 360.0F;
        }
        return value;
    }
}
