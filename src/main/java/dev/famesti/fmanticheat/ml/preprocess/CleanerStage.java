package dev.famesti.fmanticheat.ml.preprocess;

import dev.famesti.fmanticheat.util.Maths;

import java.io.Serializable;

public final class CleanerStage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final double clamp;

    public CleanerStage(double clamp) {
        this.clamp = clamp;
    }

    public float[] apply(float[] input) {
        float[] result = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = (float) Maths.clamp(input[i], -clamp, clamp);
        }
        return result;
    }

    public double getClamp() {
        return clamp;
    }
}
