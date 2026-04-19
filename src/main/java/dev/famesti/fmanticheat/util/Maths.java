package dev.famesti.fmanticheat.util;

public final class Maths {

    private Maths() {
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double sigmoid(double value) {
        if (value < -30.0D) {
            return 0.0D;
        }
        if (value > 30.0D) {
            return 1.0D;
        }
        return 1.0D / (1.0D + Math.exp(-value));
    }

    public static double tanh(double value) {
        return Math.tanh(value);
    }

    public static double angleDistance(double a, double b) {
        double diff = Math.abs(a - b) % 360.0D;
        return diff > 180.0D ? 360.0D - diff : diff;
    }

    public static double safeDiv(double a, double b) {
        return Math.abs(b) < 1.0E-6D ? 0.0D : a / b;
    }
}
