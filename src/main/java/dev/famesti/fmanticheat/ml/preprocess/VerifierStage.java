package dev.famesti.fmanticheat.ml.preprocess;

import dev.famesti.fmanticheat.util.Maths;

import java.io.Serializable;
import java.util.Random;

public final class VerifierStage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final float[] weights;
    private float bias;

    public VerifierStage(int inputSize) {
        this.weights = new float[inputSize];
        Random random = new Random(91L);
        for (int i = 0; i < weights.length; i++) {
            weights[i] = (random.nextFloat() - 0.5F) * 0.15F;
        }
        this.bias = 0.0F;
    }

    public double predict(float[] input) {
        double sum = bias;
        for (int i = 0; i < Math.min(weights.length, input.length); i++) {
            sum += weights[i] * input[i];
        }
        return Maths.sigmoid(sum);
    }

    public double computeLoss(float[] input, int label) {
        double prediction = predict(input);
        return -(label * Math.log(Math.max(1.0E-8D, prediction))
                + (1 - label) * Math.log(Math.max(1.0E-8D, 1.0D - prediction)));
    }

    public void train(float[] input, int label, double learningRate) {
        double prediction = predict(input);
        double error = prediction - label;
        for (int i = 0; i < Math.min(weights.length, input.length); i++) {
            weights[i] -= learningRate * error * input[i];
        }
        bias -= learningRate * error;
    }

    public float[] getWeights() {
        return weights;
    }

    public float getBias() {
        return bias;
    }

    public VerifierStage copy() {
        VerifierStage snapshot = new VerifierStage(weights.length);
        snapshot.copyFrom(this);
        return snapshot;
    }

    public void copyFrom(VerifierStage other) {
        if (other == null || other.weights.length != weights.length) {
            throw new IllegalArgumentException("Incompatible verifier snapshot");
        }
        System.arraycopy(other.weights, 0, this.weights, 0, weights.length);
        this.bias = other.bias;
    }
}
