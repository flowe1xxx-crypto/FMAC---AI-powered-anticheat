package dev.famesti.fmanticheat.ml.model;

public final class RnnState {

    private final double[] hidden;
    private final double output;

    public RnnState(double[] hidden, double output) {
        this.hidden = hidden;
        this.output = output;
    }

    public double[] getHidden() {
        return hidden;
    }

    public double getOutput() {
        return output;
    }
}
