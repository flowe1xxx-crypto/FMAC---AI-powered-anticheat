package dev.famesti.fmanticheat.ml.preprocess;

import java.io.Serializable;

public final class FilterStage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final double smoothing;
    private transient float[] previous;

    public FilterStage(double smoothing) {
        this.smoothing = smoothing;
    }

    public float[] apply(float[] input) {
        if (previous == null || previous.length != input.length) {
            previous = input.clone();
            return input.clone();
        }
        float[] result = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = (float) (previous[i] * smoothing + input[i] * (1.0D - smoothing));
        }
        previous = result.clone();
        return result;
    }

    public void resetState() {
        previous = null;
    }

    public double getSmoothing() {
        return smoothing;
    }
}
