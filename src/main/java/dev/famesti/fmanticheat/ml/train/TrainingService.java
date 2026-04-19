package dev.famesti.fmanticheat.ml.train;

import dev.famesti.fmanticheat.config.Settings;
import dev.famesti.fmanticheat.data.PluginDirectories;
import dev.famesti.fmanticheat.ml.ModelPipeline;
import dev.famesti.fmanticheat.ml.ModelRepository;
import dev.famesti.fmanticheat.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TrainingService {

    private static final double VALIDATION_SPLIT = 0.18D;
    private static final double MIN_LOSS_IMPROVEMENT = 1.0E-4D;

    private final JavaPlugin plugin;
    private final Settings settings;
    private final PluginDirectories directories;
    private final ModelRepository repository;
    private final ModelPipeline pipeline;
    private final DatasetLoader loader;
    private final ExecutorService executor;

    public TrainingService(JavaPlugin plugin, Settings settings, PluginDirectories directories,
                           ModelRepository repository, ModelPipeline pipeline) {
        this.plugin = plugin;
        this.settings = settings;
        this.directories = directories;
        this.repository = repository;
        this.pipeline = pipeline;
        this.loader = new DatasetLoader(plugin, directories);
        this.executor = Executors.newFixedThreadPool(Math.max(1, settings.getPlugin().getAsyncTrainingThreads()));
    }

    public void trainAsync(final CommandSender sender, final int modelIndex, final int epochs) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                List<TrainingSequence> sequences = loader.loadSequences(settings.getModels().getSequenceLength());
                if (sequences.isEmpty()) {
                    send(sender, "&cНет датасетов для обучения.");
                    logConsole("train model=" + resolveModelName(modelIndex) + " aborted=no_datasets");
                    return;
                }
                logConsole("train model=" + resolveModelName(modelIndex)
                        + " epochs=" + epochs
                        + " sequences=" + sequences.size()
                        + " lr=" + formatLoss(settings.getModels().getLearningRate())
                        + " clip=" + formatLoss(settings.getModels().getClipGradient()));
                send(sender, "&8[&6FM&8] &fЗагружено последовательностей: &e" + sequences.size());
                if (modelIndex == 4) {
                    trainMainModelWithBestEpoch(sender, sequences, epochs);
                    return;
                }
                if (modelIndex == 3) {
                    trainVerifierWithBestEpoch(sender, sequences, epochs);
                    return;
                }
                for (int epoch = 1; epoch <= epochs; epoch++) {
                    Collections.shuffle(sequences);
                    double loss = runEpoch(modelIndex, sequences);
                    send(sender, "&8[&6FM-Train&8] &7epoch=&e" + epoch + "&8/&6" + epochs
                            + " &8| &7loss=&a" + String.format("%.5f", loss));
                    logConsole("epoch=" + epoch
                            + "/" + epochs
                            + " model=" + resolveModelName(modelIndex)
                            + " train_loss=" + formatLoss(loss)
                            + " mode=simple"
                            + " sequences=" + sequences.size());
                }
                if (settings.getModels().isAutoSaveAfterTrain()) {
                    repository.saveAll();
                }
                logConsole("train_complete model=" + resolveModelName(modelIndex)
                        + " epochs=" + epochs
                        + " autosave=" + settings.getModels().isAutoSaveAfterTrain());
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        send(sender, settings.getMessages().getTrainFinish().replace("%model%", resolveModelName(modelIndex)));
                    }
                });
            }
        });
    }

    private void trainMainModelWithBestEpoch(CommandSender sender, List<TrainingSequence> sequences, int epochs) {
        DatasetSplit split = splitSequences(sequences);
        send(sender, "&8[&6FM&8] &fTrain: &e" + split.train.size() + " &8| &fValidation: &6" + split.validation.size());
        logConsole("split model=" + resolveModelName(4)
                + " train_sequences=" + split.train.size()
                + " validation_sequences=" + split.validation.size()
                + " validation_enabled=" + (!split.validation.isEmpty())
                + " select_best_after_all_epochs=true");

        double bestLoss = Double.MAX_VALUE;
        double bestAuc = Double.NaN;
        int bestEpoch = 0;
        int staleEpochs = 0;
        boolean useValidation = !split.validation.isEmpty();
        dev.famesti.fmanticheat.ml.model.FamestiRnnModel workingModel = repository.getMainModel().copy();
        dev.famesti.fmanticheat.ml.model.FamestiRnnModel bestSnapshot = workingModel.copy();

        for (int epoch = 1; epoch <= epochs; epoch++) {
            Collections.shuffle(split.train);
            double trainLoss = runMainEpoch(workingModel, split.train);
            ValidationMetrics metrics = useValidation
                    ? evaluateMainMetrics(workingModel, split.validation)
                    : new ValidationMetrics(trainLoss, Double.NaN);
            double monitoredLoss = metrics.loss;
            boolean improved = monitoredLoss + MIN_LOSS_IMPROVEMENT < bestLoss;
            if (improved) {
                bestLoss = monitoredLoss;
                bestAuc = metrics.rocAuc;
                bestEpoch = epoch;
                staleEpochs = 0;
                bestSnapshot.copyFrom(workingModel);
            } else {
                staleEpochs++;
            }

            send(sender, "&8[&6FM-Train&8] &7epoch=&e" + epoch + "&8/&6" + epochs
                    + " &8| &7train=&a" + formatLoss(trainLoss)
                    + " &8| &7val=&6" + formatLoss(monitoredLoss)
                    + " &8| &7auc=&b" + formatMetric(metrics.rocAuc)
                    + (improved ? " &8| &aBEST" : " &8| &7stale=&c" + staleEpochs));
            logConsole("epoch=" + epoch
                    + "/" + epochs
                    + " model=" + resolveModelName(4)
                    + " train_loss=" + formatLoss(trainLoss)
                    + " val_loss=" + formatLoss(monitoredLoss)
                    + " val_auc=" + formatMetric(metrics.rocAuc)
                    + " best_loss=" + formatLoss(bestLoss)
                    + " best_auc=" + formatMetric(bestAuc)
                    + " best_epoch=" + bestEpoch
                    + " stale=" + staleEpochs
                    + " improved=" + improved
                    + " train_sequences=" + split.train.size()
                    + " validation_sequences=" + split.validation.size());
        }

        repository.getMainModel().copyFrom(bestSnapshot);
        if (settings.getModels().isAutoSaveAfterTrain()) {
            repository.saveAll();
        }
        final int bestEpochFinal = bestEpoch == 0 ? 1 : bestEpoch;
        ValidationMetrics fallbackMetrics = bestLoss == Double.MAX_VALUE
                ? evaluateMainMetrics(bestSnapshot, split.train)
                : new ValidationMetrics(bestLoss, bestAuc);
        final double bestLossFinal = fallbackMetrics.loss;
        final double bestAucFinal = fallbackMetrics.rocAuc;
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                send(sender, "&8[&6FM&8] &fСохранена лучшая эпоха: &e" + bestEpochFinal
                        + " &8| &fbest_loss=&6" + formatLoss(bestLossFinal)
                        + " &8| &fbest_auc=&b" + formatMetric(bestAucFinal));
                send(sender, settings.getMessages().getTrainFinish().replace("%model%", resolveModelName(4)));
            }
        });
        logConsole("train_complete model=" + resolveModelName(4)
                + " best_epoch=" + bestEpochFinal
                + " best_loss=" + formatLoss(bestLossFinal)
                + " best_auc=" + formatMetric(bestAucFinal)
                + " autosave=" + settings.getModels().isAutoSaveAfterTrain());
    }

    private void trainVerifierWithBestEpoch(CommandSender sender, List<TrainingSequence> sequences, int epochs) {
        DatasetSplit split = splitSequences(sequences);
        send(sender, "&8[&6FM&8] &fTrain: &e" + split.train.size() + " &8| &fValidation: &6" + split.validation.size());
        logConsole("split model=" + resolveModelName(3)
                + " train_sequences=" + split.train.size()
                + " validation_sequences=" + split.validation.size()
                + " validation_enabled=" + (!split.validation.isEmpty())
                + " select_best_after_all_epochs=true");

        double bestLoss = Double.MAX_VALUE;
        double bestAuc = Double.NaN;
        int bestEpoch = 0;
        int staleEpochs = 0;
        boolean useValidation = !split.validation.isEmpty();
        dev.famesti.fmanticheat.ml.preprocess.VerifierStage workingVerifier = repository.getVerifierStage().copy();
        dev.famesti.fmanticheat.ml.preprocess.VerifierStage bestSnapshot = workingVerifier.copy();

        for (int epoch = 1; epoch <= epochs; epoch++) {
            Collections.shuffle(split.train);
            double trainLoss = runVerifierEpoch(workingVerifier, split.train);
            ValidationMetrics metrics = useValidation
                    ? evaluateVerifierMetrics(workingVerifier, split.validation)
                    : new ValidationMetrics(trainLoss, Double.NaN);
            double monitoredLoss = metrics.loss;
            boolean improved = monitoredLoss + MIN_LOSS_IMPROVEMENT < bestLoss;
            if (improved) {
                bestLoss = monitoredLoss;
                bestAuc = metrics.rocAuc;
                bestEpoch = epoch;
                staleEpochs = 0;
                bestSnapshot.copyFrom(workingVerifier);
            } else {
                staleEpochs++;
            }

            send(sender, "&8[&6FM-Train&8] &7epoch=&e" + epoch + "&8/&6" + epochs
                    + " &8| &7train=&a" + formatLoss(trainLoss)
                    + " &8| &7val=&6" + formatLoss(monitoredLoss)
                    + " &8| &7auc=&b" + formatMetric(metrics.rocAuc)
                    + (improved ? " &8| &aBEST" : " &8| &7stale=&c" + staleEpochs));
            logConsole("epoch=" + epoch
                    + "/" + epochs
                    + " model=" + resolveModelName(3)
                    + " train_loss=" + formatLoss(trainLoss)
                    + " val_loss=" + formatLoss(monitoredLoss)
                    + " val_auc=" + formatMetric(metrics.rocAuc)
                    + " best_loss=" + formatLoss(bestLoss)
                    + " best_auc=" + formatMetric(bestAuc)
                    + " best_epoch=" + bestEpoch
                    + " stale=" + staleEpochs
                    + " improved=" + improved
                    + " train_sequences=" + split.train.size()
                    + " validation_sequences=" + split.validation.size());
        }

        repository.getVerifierStage().copyFrom(bestSnapshot);
        if (settings.getModels().isAutoSaveAfterTrain()) {
            repository.saveAll();
        }
        final int bestEpochFinal = bestEpoch == 0 ? 1 : bestEpoch;
        ValidationMetrics fallbackMetrics = bestLoss == Double.MAX_VALUE
                ? evaluateVerifierMetrics(bestSnapshot, split.train)
                : new ValidationMetrics(bestLoss, bestAuc);
        final double bestLossFinal = fallbackMetrics.loss;
        final double bestAucFinal = fallbackMetrics.rocAuc;
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                send(sender, "&8[&6FM&8] &fСохранена лучшая эпоха: &e" + bestEpochFinal
                        + " &8| &fbest_loss=&6" + formatLoss(bestLossFinal)
                        + " &8| &fbest_auc=&b" + formatMetric(bestAucFinal));
                send(sender, settings.getMessages().getTrainFinish().replace("%model%", resolveModelName(3)));
            }
        });
        logConsole("train_complete model=" + resolveModelName(3)
                + " best_epoch=" + bestEpochFinal
                + " best_loss=" + formatLoss(bestLossFinal)
                + " best_auc=" + formatMetric(bestAucFinal)
                + " autosave=" + settings.getModels().isAutoSaveAfterTrain());
    }

    private double runEpoch(int modelIndex, List<TrainingSequence> sequences) {
        double totalLoss = 0.0D;
        double lr = settings.getModels().getLearningRate();
        int totalSamples = 0;
        for (TrainingSequence sequence : sequences) {
            if (modelIndex == 1) {
                repository.getFilterStage().resetState();
                totalLoss += 0.01D;
                totalSamples++;
            } else if (modelIndex == 2) {
                totalLoss += 0.01D;
                totalSamples++;
            } else if (modelIndex == 3) {
                for (float[] input : sequence.getSequence()) {
                    totalLoss += repository.getVerifierStage().computeLoss(input, sequence.getLabel());
                    repository.getVerifierStage().train(input, sequence.getLabel(), lr * 0.5D);
                    totalSamples++;
                }
            } else {
                totalLoss += repository.getMainModel().trainSequence(sequence.getSequence(), sequence.getLabel(), lr, settings.getModels().getClipGradient());
                totalSamples++;
            }
        }
        return totalLoss / Math.max(1, totalSamples);
    }

    private double runMainEpoch(dev.famesti.fmanticheat.ml.model.FamestiRnnModel model, List<TrainingSequence> sequences) {
        double totalLoss = 0.0D;
        double lr = settings.getModels().getLearningRate();
        for (TrainingSequence sequence : sequences) {
            totalLoss += model.trainSequence(sequence.getSequence(), sequence.getLabel(), lr, settings.getModels().getClipGradient());
        }
        return totalLoss / Math.max(1, sequences.size());
    }

    private double runVerifierEpoch(dev.famesti.fmanticheat.ml.preprocess.VerifierStage verifier, List<TrainingSequence> sequences) {
        double totalLoss = 0.0D;
        double lr = settings.getModels().getLearningRate();
        int totalSamples = 0;
        for (TrainingSequence sequence : sequences) {
            for (float[] input : sequence.getSequence()) {
                totalLoss += verifier.computeLoss(input, sequence.getLabel());
                verifier.train(input, sequence.getLabel(), lr * 0.5D);
                totalSamples++;
            }
        }
        return totalLoss / Math.max(1, totalSamples);
    }

    public String resolveModelName(int modelIndex) {
        switch (modelIndex) {
            case 1:
                return "filter.dat";
            case 2:
                return "cleaner.dat";
            case 3:
                return "verifier.dat";
            case 4:
            default:
                return "main.dat (Famesti-publik-45k)";
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void send(CommandSender sender, String message) {
        ColorUtil.send(sender, message);
    }

    private void logConsole(String message) {
        plugin.getLogger().info("[FM-Train] " + message);
    }

    private DatasetSplit splitSequences(List<TrainingSequence> sequences) {
        List<TrainingSequence> shuffled = new ArrayList<TrainingSequence>(sequences);
        Collections.shuffle(shuffled);
        if (shuffled.size() < 8) {
            return new DatasetSplit(shuffled, Collections.<TrainingSequence>emptyList());
        }
        int validationSize = Math.max(1, (int) Math.round(shuffled.size() * VALIDATION_SPLIT));
        if (validationSize >= shuffled.size()) {
            validationSize = shuffled.size() - 1;
        }
        List<TrainingSequence> validation = new ArrayList<TrainingSequence>(shuffled.subList(0, validationSize));
        List<TrainingSequence> train = new ArrayList<TrainingSequence>(shuffled.subList(validationSize, shuffled.size()));
        return new DatasetSplit(train, validation);
    }

    private ValidationMetrics evaluateMainMetrics(dev.famesti.fmanticheat.ml.model.FamestiRnnModel model, List<TrainingSequence> sequences) {
        double totalLoss = 0.0D;
        List<ScoredLabel> samples = new ArrayList<ScoredLabel>(sequences.size());
        for (TrainingSequence sequence : sequences) {
            double prediction = model.predict(sequence.getSequence());
            totalLoss += computeBinaryCrossEntropy(prediction, sequence.getLabel());
            samples.add(new ScoredLabel(prediction, sequence.getLabel()));
        }
        return new ValidationMetrics(
                totalLoss / Math.max(1, sequences.size()),
                computeRocAuc(samples)
        );
    }

    private ValidationMetrics evaluateVerifierMetrics(dev.famesti.fmanticheat.ml.preprocess.VerifierStage verifier, List<TrainingSequence> sequences) {
        double totalLoss = 0.0D;
        int totalSamples = 0;
        List<ScoredLabel> samples = new ArrayList<ScoredLabel>();
        for (TrainingSequence sequence : sequences) {
            for (float[] input : sequence.getSequence()) {
                double prediction = verifier.predict(input);
                totalLoss += computeBinaryCrossEntropy(prediction, sequence.getLabel());
                totalSamples++;
                samples.add(new ScoredLabel(prediction, sequence.getLabel()));
            }
        }
        return new ValidationMetrics(
                totalLoss / Math.max(1, totalSamples),
                computeRocAuc(samples)
        );
    }

    private String formatLoss(double loss) {
        return String.format(Locale.US, "%.5f", loss);
    }

    private String formatMetric(double value) {
        if (Double.isNaN(value)) {
            return "n/a";
        }
        return String.format(Locale.US, "%.5f", value);
    }

    private double computeBinaryCrossEntropy(double prediction, int label) {
        return -(label * Math.log(Math.max(1.0E-8D, prediction))
                + (1 - label) * Math.log(Math.max(1.0E-8D, 1.0D - prediction)));
    }

    private double computeRocAuc(List<ScoredLabel> samples) {
        if (samples.isEmpty()) {
            return Double.NaN;
        }
        int positives = 0;
        int negatives = 0;
        for (ScoredLabel sample : samples) {
            if (sample.label == 1) {
                positives++;
            } else {
                negatives++;
            }
        }
        if (positives == 0 || negatives == 0) {
            return Double.NaN;
        }

        Collections.sort(samples);
        double rankSum = 0.0D;
        int index = 0;
        while (index < samples.size()) {
            int end = index + 1;
            while (end < samples.size() && Double.compare(samples.get(index).score, samples.get(end).score) == 0) {
                end++;
            }
            double averageRank = ((index + 1) + end) / 2.0D;
            for (int i = index; i < end; i++) {
                if (samples.get(i).label == 1) {
                    rankSum += averageRank;
                }
            }
            index = end;
        }

        return (rankSum - (positives * (positives + 1) / 2.0D)) / (positives * (double) negatives);
    }

    private static final class DatasetSplit {
        private final List<TrainingSequence> train;
        private final List<TrainingSequence> validation;

        private DatasetSplit(List<TrainingSequence> train, List<TrainingSequence> validation) {
            this.train = train;
            this.validation = validation;
        }
    }

    private static final class ValidationMetrics {
        private final double loss;
        private final double rocAuc;

        private ValidationMetrics(double loss, double rocAuc) {
            this.loss = loss;
            this.rocAuc = rocAuc;
        }
    }

    private static final class ScoredLabel implements Comparable<ScoredLabel> {
        private final double score;
        private final int label;

        private ScoredLabel(double score, int label) {
            this.score = score;
            this.label = label;
        }

        @Override
        public int compareTo(ScoredLabel other) {
            return Double.compare(this.score, other.score);
        }
    }
}
