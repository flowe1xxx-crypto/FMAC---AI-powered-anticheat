# FM AntiCheat Feature Manual

FM AntiCheat ships with a custom combat telemetry pipeline focused on Kill-Aim and Aimbot analysis.

## Input Features

1. Yaw and pitch absolute values.
2. Yaw and pitch deltas.
3. Clicks per second sampled in a rolling one-second window.
4. Horizontal and vertical velocity.
5. Acceleration spikes between consecutive samples.
6. Target alignment error in yaw and pitch.
7. Tracking consistency.
8. Rotation smoothness and snap factor.
9. Distance to target.
10. Trigonometric rotation embedding.
11. Fractional position markers.
12. Relative target vector axes.
13. Aim lock duration estimate.
14. Hit recency.
15. Rotation ratio indicators.
16. Composite smooth-tracking score.

## Model Stack

The shipped pipeline is intentionally split into multiple stages.

- filter.dat: temporal smoothing and noise suppression.
- cleaner.dat: clamps outliers and stabilizes the feature range.
- verifier.dat: lightweight logistic verification layer.
- main.dat: Famesti-publik-56k recurrent model.

## Training Notes

- Record both legit and cheat data.
- Use multiple players, sensitivities and ping conditions.
- Avoid mixing PvE-only samples into a PvP-focused training set.
- Keep sequence length aligned with the configuration.
- Retrain verifier and main model after collecting at least several dozen dataset files.

## Operational Notes

- Action bars are rate-limited by scheduler intervals.
- Monitoring is synchronous for Bukkit entity safety.
- Training is asynchronous to reduce TPS pressure.
- Model persistence uses serialized .dat files.
