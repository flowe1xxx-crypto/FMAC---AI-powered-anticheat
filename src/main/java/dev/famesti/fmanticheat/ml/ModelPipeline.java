package dev.famesti.fmanticheat.ml;

import dev.famesti.fmanticheat.config.Settings;
import dev.famesti.fmanticheat.dataset.CombatFeatureFrame;
import dev.famesti.fmanticheat.ml.model.RnnState;
import dev.famesti.fmanticheat.monitor.PlayerCombatSnapshot;
import dev.famesti.fmanticheat.util.Maths;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public final class ModelPipeline {

    private final JavaPlugin plugin;
    private final ModelRepository repository;
    private final Settings settings;

    public ModelPipeline(JavaPlugin plugin, ModelRepository repository, Settings settings) {
        this.plugin = plugin;
        this.repository = repository;
        this.settings = settings;
    }

    public double evaluate(PlayerCombatSnapshot snapshot) {
        if (snapshot.getRecentFrames().size() < settings.getChecks().getMinimumCombatFrames()) {
            return 0.0D;
        }
        repository.getFilterStage().resetState();
        Deque<float[]> processed = new ArrayDeque<float[]>();
        Iterator<CombatFeatureFrame> iterator = snapshot.getRecentFrames().descendingIterator();
        while (iterator.hasNext() && processed.size() < settings.getModels().getSequenceLength()) {
            CombatFeatureFrame frame = iterator.next();
            float[] filtered = repository.getFilterStage().apply(frame.getFeatures());
            float[] cleaned = repository.getCleanerStage().apply(filtered);
            processed.addFirst(cleaned);
        }
        float[][] sequence = new float[processed.size()][];
        int index = 0;
        double verifierAccumulator = 0.0D;
        for (float[] vector : processed) {
            sequence[index++] = vector;
            verifierAccumulator += repository.getVerifierStage().predict(vector);
        }
        RnnState state = repository.getMainModel().forward(sequence);
        double verifierScore = verifierAccumulator / Math.max(1, sequence.length);
        double result = state.getOutput() * 0.88D + verifierScore * 0.12D;
        if (verifierScore < settings.getModels().getVerifierThreshold()) {
            result *= 0.92D;
        } else {
            result = Math.min(1.0D, result + (verifierScore - settings.getModels().getVerifierThreshold()) * 0.12D);
        }
        if (state.getOutput() < settings.getChecks().getThreshold() && verifierScore < settings.getModels().getVerifierThreshold() + 0.03D) {
            result *= 0.94D;
        }
        if (result >= settings.getModels().getVerboseProbabilityLogThreshold()) {
            plugin.getLogger().fine("FM pipeline suspicious probability=" + result);
        }
        return Maths.clamp(result, 0.0D, 1.0D);
    }
}
