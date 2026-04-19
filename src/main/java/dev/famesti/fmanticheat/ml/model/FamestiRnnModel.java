package dev.famesti.fmanticheat.ml.model;

import dev.famesti.fmanticheat.util.Maths;

import java.io.Serializable;
import java.util.Random;

public final class FamestiRnnModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int inputSize;
    private final int hiddenSize;
    private final double[][] wxh;
    private final double[][] whh;
    private final double[] bh;
    private final double[] why;
    private double by;

    public FamestiRnnModel(int inputSize, int hiddenSize, long seed) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.wxh = new double[hiddenSize][inputSize];
        this.whh = new double[hiddenSize][hiddenSize];
        this.bh = new double[hiddenSize];
        this.why = new double[hiddenSize];
        init(seed);
    }

    private void init(long seed) {
        Random random = new Random(seed);
        for (int h = 0; h < hiddenSize; h++) {
            for (int i = 0; i < inputSize; i++) {
                wxh[h][i] = (random.nextDouble() - 0.5D) * 0.08D;
            }
            for (int j = 0; j < hiddenSize; j++) {
                whh[h][j] = (random.nextDouble() - 0.5D) * 0.04D;
            }
            why[h] = (random.nextDouble() - 0.5D) * 0.08D;
            bh[h] = 0.0D;
        }
        by = 0.0D;
    }

    public int parameterCount() {
        return hiddenSize * inputSize + hiddenSize * hiddenSize + hiddenSize + hiddenSize + 1;
    }

    public int getInputSize() {
        return inputSize;
    }

    public int getHiddenSize() {
        return hiddenSize;
    }

    public RnnState forward(float[][] sequence) {
        double[] prev = new double[hiddenSize];
        double[] next = new double[hiddenSize];
        for (int t = 0; t < sequence.length; t++) {
            float[] input = sequence[t];
            for (int h = 0; h < hiddenSize; h++) {
                double sum = bh[h];
                for (int i = 0; i < inputSize; i++) {
                    sum += wxh[h][i] * input[i];
                }
                for (int j = 0; j < hiddenSize; j++) {
                    sum += whh[h][j] * prev[j];
                }
                next[h] = Maths.tanh(sum);
            }
            System.arraycopy(next, 0, prev, 0, hiddenSize);
        }
        double out = by;
        for (int h = 0; h < hiddenSize; h++) {
            out += why[h] * prev[h];
        }
        return new RnnState(prev, Maths.sigmoid(out));
    }

    public double predict(float[][] sequence) {
        return forward(sequence).getOutput();
    }

    public double computeLoss(float[][] sequence, int label) {
        double prediction = predict(sequence);
        return -(label * Math.log(Math.max(1.0E-8D, prediction))
                + (1 - label) * Math.log(Math.max(1.0E-8D, 1.0D - prediction)));
    }

    public double trainSequence(float[][] sequence, int label, double learningRate, double clip) {
        int time = sequence.length;
        double[][] hidden = new double[time + 1][hiddenSize];
        for (int t = 0; t < time; t++) {
            for (int h = 0; h < hiddenSize; h++) {
                double sum = bh[h];
                for (int i = 0; i < inputSize; i++) {
                    sum += wxh[h][i] * sequence[t][i];
                }
                for (int j = 0; j < hiddenSize; j++) {
                    sum += whh[h][j] * hidden[t][j];
                }
                hidden[t + 1][h] = Maths.tanh(sum);
            }
        }

        double logit = by;
        for (int h = 0; h < hiddenSize; h++) {
            logit += why[h] * hidden[time][h];
        }
        double prediction = Maths.sigmoid(logit);
        double loss = -(label * Math.log(Math.max(1.0E-8D, prediction)) + (1 - label) * Math.log(Math.max(1.0E-8D, 1.0D - prediction)));
        double dLogit = prediction - label;

        double[] dWhy = new double[hiddenSize];
        double dBy = dLogit;
        double[] dh = new double[hiddenSize];
        for (int h = 0; h < hiddenSize; h++) {
            dWhy[h] = dLogit * hidden[time][h];
            dh[h] = dLogit * why[h];
        }

        double[][] dWxh = new double[hiddenSize][inputSize];
        double[][] dWhh = new double[hiddenSize][hiddenSize];
        double[] dBh = new double[hiddenSize];

        for (int t = time - 1; t >= 0; t--) {
            double[] dhRaw = new double[hiddenSize];
            for (int h = 0; h < hiddenSize; h++) {
                dhRaw[h] = (1.0D - hidden[t + 1][h] * hidden[t + 1][h]) * dh[h];
                dBh[h] += dhRaw[h];
                for (int i = 0; i < inputSize; i++) {
                    dWxh[h][i] += dhRaw[h] * sequence[t][i];
                }
                for (int j = 0; j < hiddenSize; j++) {
                    dWhh[h][j] += dhRaw[h] * hidden[t][j];
                }
            }

            double[] nextDh = new double[hiddenSize];
            for (int j = 0; j < hiddenSize; j++) {
                double sum = 0.0D;
                for (int h = 0; h < hiddenSize; h++) {
                    sum += dhRaw[h] * whh[h][j];
                }
                nextDh[j] = sum;
            }
            dh = nextDh;
        }

        clip(dWhy, clip);
        clip(dBh, clip);
        for (int h = 0; h < hiddenSize; h++) {
            clip(dWxh[h], clip);
            clip(dWhh[h], clip);
        }
        dBy = Math.max(-clip, Math.min(clip, dBy));

        for (int h = 0; h < hiddenSize; h++) {
            why[h] -= learningRate * dWhy[h];
            bh[h] -= learningRate * dBh[h];
            for (int i = 0; i < inputSize; i++) {
                wxh[h][i] -= learningRate * dWxh[h][i];
            }
            for (int j = 0; j < hiddenSize; j++) {
                whh[h][j] -= learningRate * dWhh[h][j];
            }
        }
        by -= learningRate * dBy;
        return loss;
    }

    public FamestiRnnModel copy() {
        FamestiRnnModel snapshot = new FamestiRnnModel(inputSize, hiddenSize, 1L);
        snapshot.copyFrom(this);
        return snapshot;
    }

    public void copyFrom(FamestiRnnModel other) {
        if (other == null || other.inputSize != inputSize || other.hiddenSize != hiddenSize) {
            throw new IllegalArgumentException("Incompatible RNN model snapshot");
        }
        for (int h = 0; h < hiddenSize; h++) {
            System.arraycopy(other.wxh[h], 0, this.wxh[h], 0, inputSize);
            System.arraycopy(other.whh[h], 0, this.whh[h], 0, hiddenSize);
            this.bh[h] = other.bh[h];
            this.why[h] = other.why[h];
        }
        this.by = other.by;
    }

    private void clip(double[] array, double clip) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] > clip) {
                array[i] = clip;
            } else if (array[i] < -clip) {
                array[i] = -clip;
            }
        }
    }
}
