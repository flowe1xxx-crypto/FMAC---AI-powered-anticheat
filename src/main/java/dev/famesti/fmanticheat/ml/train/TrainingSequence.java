package dev.famesti.fmanticheat.ml.train;

public final class TrainingSequence {

    private final float[][] sequence;
    private final int label;

    public TrainingSequence(float[][] sequence, int label) {
        this.sequence = sequence;
        this.label = label;
    }

    public float[][] getSequence() {
        return sequence;
    }

    public int getLabel() {
        return label;
    }
}
