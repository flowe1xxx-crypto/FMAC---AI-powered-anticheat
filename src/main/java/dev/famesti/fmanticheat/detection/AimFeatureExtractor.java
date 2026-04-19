package dev.famesti.fmanticheat.detection;

import dev.famesti.fmanticheat.dataset.CombatFeatureFrame;
import dev.famesti.fmanticheat.monitor.PlayerCombatSnapshot;
import dev.famesti.fmanticheat.util.Maths;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class AimFeatureExtractor {

    private static final int INPUT_SIZE = 32;

    public CombatFeatureFrame extract(Player player, PlayerCombatSnapshot snapshot, long tick) {
        Entity targetEntity = snapshot.getCurrentTarget();
        Location playerLocation = player.getLocation();
        Location targetLocation = targetEntity != null ? targetEntity.getLocation().clone() : playerLocation.clone();
        if (targetEntity instanceof LivingEntity) {
            targetLocation.add(0.0D, ((LivingEntity) targetEntity).getEyeHeight() * 0.85D, 0.0D);
        }

        Vector toTarget = targetLocation.toVector().subtract(playerLocation.toVector());
        double horizontal = Math.sqrt(toTarget.getX() * toTarget.getX() + toTarget.getZ() * toTarget.getZ());
        float idealYaw = (float) Math.toDegrees(Math.atan2(-toTarget.getX(), toTarget.getZ()));
        float idealPitch = (float) Math.toDegrees(-Math.atan2(toTarget.getY(), Math.max(0.0001D, horizontal)));
        float yawError = (float) Maths.angleDistance(playerLocation.getYaw(), idealYaw);
        float pitchError = Math.abs(playerLocation.getPitch() - idealPitch);
        float yawDelta = snapshot.getLastYawDelta();
        float pitchDelta = snapshot.getLastPitchDelta();
        float cps = snapshot.getCps();
        float distance = (float) playerLocation.distance(targetLocation);
        Vector velocity = player.getVelocity();
        float horizontalSpeed = (float) Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
        float verticalSpeed = (float) velocity.getY();
        float acceleration = snapshot.getLastAcceleration();
        float smoothness = 1.0F - (float) Maths.clamp(Math.abs(yawDelta - pitchDelta) / 180.0D, 0.0D, 1.0D);
        float trackingConsistency = 1.0F - (float) Maths.clamp((yawError + pitchError) / 180.0D, 0.0D, 1.0D);
        float snapFactor = (float) Maths.clamp(Math.abs(yawDelta) / 180.0D, 0.0D, 1.0D);
        float strafeBias = (float) Maths.clamp(Math.abs(velocity.getX()) + Math.abs(velocity.getZ()), 0.0D, 1.5D);
        float verticalAimRatio = (float) Maths.safeDiv(Math.abs(pitchDelta), Math.abs(yawDelta) + 1.0D);
        float[] features = new float[INPUT_SIZE];
        features[0] = normalizeAngle(playerLocation.getYaw());
        features[1] = normalizePitch(playerLocation.getPitch());
        features[2] = normalizeAngle(yawDelta);
        features[3] = normalizePitch(pitchDelta);
        features[4] = cps / 25.0F;
        features[5] = horizontalSpeed;
        features[6] = verticalSpeed;
        features[7] = acceleration;
        features[8] = yawError / 180.0F;
        features[9] = pitchError / 90.0F;
        features[10] = trackingConsistency;
        features[11] = smoothness;
        features[12] = distance / 8.0F;
        features[13] = snapFactor;
        features[14] = strafeBias;
        features[15] = verticalAimRatio;
        features[16] = (float) Maths.clamp(playerLocation.getX() % 1.0D, -1.0D, 1.0D);
        features[17] = (float) Maths.clamp(playerLocation.getY() % 1.0D, -1.0D, 1.0D);
        features[18] = (float) Maths.clamp(playerLocation.getZ() % 1.0D, -1.0D, 1.0D);
        features[19] = (float) Math.sin(Math.toRadians(playerLocation.getYaw()));
        features[20] = (float) Math.cos(Math.toRadians(playerLocation.getYaw()));
        features[21] = (float) Math.sin(Math.toRadians(playerLocation.getPitch()));
        features[22] = (float) Math.cos(Math.toRadians(playerLocation.getPitch()));
        features[23] = (float) Maths.clamp(toTarget.getX() / 8.0D, -1.0D, 1.0D);
        features[24] = (float) Maths.clamp(toTarget.getY() / 8.0D, -1.0D, 1.0D);
        features[25] = (float) Maths.clamp(toTarget.getZ() / 8.0D, -1.0D, 1.0D);
        features[26] = snapshot.getAimLockTicks() / 20.0F;
        features[27] = (float) Maths.clamp((System.currentTimeMillis() - snapshot.getLastHitMillis()) / 1000.0D, 0.0D, 1.0D);
        features[28] = (float) Maths.clamp(Math.abs(yawDelta) / (Math.abs(pitchDelta) + 1.0D), 0.0D, 5.0D) / 5.0F;
        features[29] = (float) Maths.clamp(Math.abs(pitchDelta) / (Math.abs(yawDelta) + 1.0D), 0.0D, 5.0D) / 5.0F;
        features[30] = trackingConsistency * smoothness;
        features[31] = (float) Maths.clamp((25.0D - cps) / 25.0D, 0.0D, 1.0D);

        return new CombatFeatureFrame(tick, features, yawDelta, pitchDelta, cps, distance, trackingConsistency, smoothness);
    }

    private float normalizeAngle(float angle) {
        return angle / 180.0F;
    }

    private float normalizePitch(float pitch) {
        return pitch / 90.0F;
    }
}
