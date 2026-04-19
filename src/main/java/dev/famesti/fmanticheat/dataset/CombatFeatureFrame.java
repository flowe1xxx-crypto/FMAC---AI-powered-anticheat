package dev.famesti.fmanticheat.dataset;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class CombatFeatureFrame {

    private final long tick;
    private final float[] features;
    private final float yawDelta;
    private final float pitchDelta;
    private final float cps;
    private final float targetDistance;
    private final float trackingConsistency;
    private final float smoothness;

    public CombatFeatureFrame(long tick, float[] features, float yawDelta, float pitchDelta,
                              float cps, float targetDistance, float trackingConsistency,
                              float smoothness) {
        this.tick = tick;
        this.features = features;
        this.yawDelta = yawDelta;
        this.pitchDelta = pitchDelta;
        this.cps = cps;
        this.targetDistance = targetDistance;
        this.trackingConsistency = trackingConsistency;
        this.smoothness = smoothness;
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeLong(tick);
        out.writeInt(features.length);
        for (float feature : features) {
            out.writeFloat(feature);
        }
        out.writeFloat(yawDelta);
        out.writeFloat(pitchDelta);
        out.writeFloat(cps);
        out.writeFloat(targetDistance);
        out.writeFloat(trackingConsistency);
        out.writeFloat(smoothness);
    }

    public static CombatFeatureFrame read(DataInputStream in) throws IOException {
        long tick = in.readLong();
        int len = in.readInt();
        float[] values = new float[len];
        for (int i = 0; i < len; i++) {
            values[i] = in.readFloat();
        }
        return new CombatFeatureFrame(
                tick,
                values,
                in.readFloat(),
                in.readFloat(),
                in.readFloat(),
                in.readFloat(),
                in.readFloat(),
                in.readFloat()
        );
    }

    public long getTick() { return tick; }
    public float[] getFeatures() { return features; }
    public float getYawDelta() { return yawDelta; }
    public float getPitchDelta() { return pitchDelta; }
    public float getCps() { return cps; }
    public float getTargetDistance() { return targetDistance; }
    public float getTrackingConsistency() { return trackingConsistency; }
    public float getSmoothness() { return smoothness; }
}
