package dev.famesti.fmanticheat.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class Settings {

    private final JavaPlugin plugin;
    private PluginSection pluginSection;
    private DatasetSection datasetSection;
    private ModelSection modelSection;
    private CheckSection checkSection;
    private MessageSection messageSection;

    public Settings(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration c = plugin.getConfig();
        this.pluginSection = new PluginSection(
                c.getString("plugin.prefix", "&#ff2d55&lFM &#ffd166&lAC &8» "),
                c.getBoolean("plugin.debug-default", false),
                c.getBoolean("plugin.alerts-default", true),
                c.getInt("plugin.action-bar-interval-ticks", 20),
                c.getInt("plugin.monitor-interval-ticks", 2),
                c.getInt("plugin.probability-broadcast-interval-ticks", 30),
                c.getInt("plugin.async-training-threads", 1),
                c.getBoolean("plugin.banner-enabled", true),
                c.getBoolean("plugin.minimal-startup-output", true)
        );
        this.datasetSection = new DatasetSection(
                c.getString("datasets.folder", "datasets"),
                c.getInt("datasets.target-size-bytes", 17408),
                c.getInt("datasets.min-frames-to-save", 24),
                c.getBoolean("datasets.auto-stop-when-full", true),
                c.getInt("datasets.max-concurrent-recordings", 16),
                c.getInt("datasets.combat-window-ticks", 12),
                c.getBoolean("datasets.actionbar.enabled", true),
                c.getString("datasets.actionbar.template", "&6Samples: &f%samples% &8| &eProgress: &f%progress%%")
        );
        this.modelSection = new ModelSection(
                c.getInt("models.training-batch-size", 16),
                c.getDouble("models.learning-rate", 0.0035D),
                c.getDouble("models.decay", 0.00005D),
                c.getInt("models.sequence-length", 24),
                c.getInt("models.input-size", 32),
                c.getInt("models.hidden-size", 196),
                c.getDouble("models.clip-gradient", 2.4D),
                c.getDouble("models.verifier-threshold", 0.61D),
                c.getDouble("models.cleaner-outlier-clamp", 3.75D),
                c.getDouble("models.filter-smoothing", 0.72D),
                c.getBoolean("models.autosave-after-train", true),
                c.getDouble("models.initial-threshold", 0.72D),
                c.getInt("models.punish-after-flags", 3),
                c.getDouble("models.verbose-probability-log-threshold", 0.65D),
                c.getString("models.model-files.filter", "filter.dat"),
                c.getString("models.model-files.cleaner", "cleaner.dat"),
                c.getString("models.model-files.verifier", "verifier.dat"),
                c.getString("models.model-files.main", "main.dat"),
                c.getStringList("models.punish-commands")
        );
        this.checkSection = new CheckSection(
                c.getBoolean("checks.enabled", true),
                c.getDouble("checks.threshold", 0.65D),
                c.getInt("checks.minimum-combat-frames", 10),
                c.getLong("checks.cooldown-between-alerts-ms", 1200L),
                c.getDouble("checks.rotation-snap-threshold", 58.0D),
                c.getDouble("checks.impossible-smoothness-threshold", 0.985D),
                c.getDouble("checks.silent-aim-angle-threshold", 22.5D),
                c.getDouble("checks.tracking-consistency-threshold", 0.87D),
                c.getInt("checks.aim-lock-duration-ticks", 8),
                c.getInt("checks.cps-hard-limit", 25),
                c.getBoolean("checks.setback-enabled", false),
                c.getBoolean("checks.punish-enabled", true),
                c.getString("checks.punishment-mode", "ban"),
                c.getLong("checks.minimum-prediction-interval-ms", 90L),
                c.getDouble("checks.probability-smoothing-factor", 0.78D),
                c.getInt("checks.minimum-alert-streak", 1),
                c.getInt("checks.watch-threshold-percent", 65),
                c.getInt("checks.medium-threshold-percent", 70),
                c.getInt("checks.terminal-threshold-percent", 94),
                c.getInt("checks.watch-violation-gain", 20),
                c.getInt("checks.medium-violation-gain", 60)
        );
        this.messageSection = new MessageSection(
                c.getString("messages.no-permission", "&#ff2d55&lFMAC &8| &cНедостаточно прав."),
                c.getString("messages.reloaded", "&#00f5d4&lFMAC &8| &fКонфиг и модели &aуспешно перезагружены&f."),
                c.getString("messages.debug-on", "&#fee440&lDEBUG &8| &fРежим отладки &aвключен&f."),
                c.getString("messages.debug-off", "&#adb5bd&lDEBUG &8| &fРежим отладки &7выключен&f."),
                c.getString("messages.alerts-on", "&#00f5d4&lALERTS &8| &fГлобальные алерты &aвключены&f."),
                c.getString("messages.alerts-off", "&#ff2d55&lALERTS &8| &fГлобальные алерты &cвыключены&f."),
                c.getString("messages.record-start", "&#80ed99&lDATASET &8| &fЗапись началась для &e%label%&f."),
                c.getString("messages.record-stop", "&#ffd166&lDATASET &8| &fЗапись завершена: &e%file%"),
                c.getString("messages.record-full", "&#ff8a00&lDATASET &8| &fЦелевой размер датасета достигнут."),
                c.getString("messages.train-start", "&#00bbf9&lTRAIN &8| &fОбучение модели &e%model% &fзапущено на &e%epochs% &fэпох."),
                c.getString("messages.train-finish", "&#00f5d4&lTRAIN &8| &fОбучение модели &a%model% &fзавершено."),
                c.getString("messages.prob-line", "&#7f5af0&lFM HUD &8| &f%player% &8| &f%prob%%% &8| &#ffd166LIVE"),
                c.getString("messages.alert-format", "&#ff2d55&lALERT &8[&#ffd166&lFMAC&8] &f%player% &7-> &#ff8a00&l%prob%% &8( &#f15bb5%reason% &8)"),
                c.getString("messages.ml-header", "&#7f5af0&l==== FM ML CORE ==== "),
                c.getString("messages.ml-entry", "&#f15bb5%name% &8| &floaded=&a%loaded% &8| &fupdated=&e%updated%"),
                c.getString("messages.sanction-reason", "Система подозревает вас в ЧИТАХ!\n                 Ложно? @flowe1x")
        );
    }

    public PluginSection getPlugin() {
        return pluginSection;
    }

    public DatasetSection getDatasets() {
        return datasetSection;
    }

    public ModelSection getModels() {
        return modelSection;
    }

    public CheckSection getChecks() {
        return checkSection;
    }

    public MessageSection getMessages() {
        return messageSection;
    }

    public static final class PluginSection {
        private final String prefix;
        private final boolean debugDefault;
        private final boolean alertsDefault;
        private final int actionBarIntervalTicks;
        private final int monitorIntervalTicks;
        private final int probabilityBroadcastIntervalTicks;
        private final int asyncTrainingThreads;
        private final boolean bannerEnabled;
        private final boolean minimalStartupOutput;

        public PluginSection(String prefix, boolean debugDefault, boolean alertsDefault, int actionBarIntervalTicks,
                             int monitorIntervalTicks, int probabilityBroadcastIntervalTicks,
                             int asyncTrainingThreads, boolean bannerEnabled, boolean minimalStartupOutput) {
            this.prefix = prefix;
            this.debugDefault = debugDefault;
            this.alertsDefault = alertsDefault;
            this.actionBarIntervalTicks = actionBarIntervalTicks;
            this.monitorIntervalTicks = monitorIntervalTicks;
            this.probabilityBroadcastIntervalTicks = probabilityBroadcastIntervalTicks;
            this.asyncTrainingThreads = asyncTrainingThreads;
            this.bannerEnabled = bannerEnabled;
            this.minimalStartupOutput = minimalStartupOutput;
        }

        public String getPrefix() { return prefix; }
        public boolean isDebugDefault() { return debugDefault; }
        public boolean isAlertsDefault() { return alertsDefault; }
        public int getActionBarIntervalTicks() { return actionBarIntervalTicks; }
        public int getMonitorIntervalTicks() { return monitorIntervalTicks; }
        public int getProbabilityBroadcastIntervalTicks() { return probabilityBroadcastIntervalTicks; }
        public int getAsyncTrainingThreads() { return asyncTrainingThreads; }
        public boolean isBannerEnabled() { return bannerEnabled; }
        public boolean isMinimalStartupOutput() { return minimalStartupOutput; }
    }

    public static final class DatasetSection {
        private final String folder;
        private final int targetSizeBytes;
        private final int minFramesToSave;
        private final boolean autoStopWhenFull;
        private final int maxConcurrentRecordings;
        private final int combatWindowTicks;
        private final boolean actionBarEnabled;
        private final String actionBarTemplate;

        public DatasetSection(String folder, int targetSizeBytes, int minFramesToSave, boolean autoStopWhenFull,
                              int maxConcurrentRecordings, int combatWindowTicks, boolean actionBarEnabled,
                              String actionBarTemplate) {
            this.folder = folder;
            this.targetSizeBytes = targetSizeBytes;
            this.minFramesToSave = minFramesToSave;
            this.autoStopWhenFull = autoStopWhenFull;
            this.maxConcurrentRecordings = maxConcurrentRecordings;
            this.combatWindowTicks = combatWindowTicks;
            this.actionBarEnabled = actionBarEnabled;
            this.actionBarTemplate = actionBarTemplate;
        }

        public String getFolder() { return folder; }
        public int getTargetSizeBytes() { return targetSizeBytes; }
        public int getMinFramesToSave() { return minFramesToSave; }
        public boolean isAutoStopWhenFull() { return autoStopWhenFull; }
        public int getMaxConcurrentRecordings() { return maxConcurrentRecordings; }
        public int getCombatWindowTicks() { return combatWindowTicks; }
        public boolean isActionBarEnabled() { return actionBarEnabled; }
        public String getActionBarTemplate() { return actionBarTemplate; }
    }

    public static final class ModelSection {
        private final int batchSize;
        private final double learningRate;
        private final double decay;
        private final int sequenceLength;
        private final int inputSize;
        private final int hiddenSize;
        private final double clipGradient;
        private final double verifierThreshold;
        private final double cleanerOutlierClamp;
        private final double filterSmoothing;
        private final boolean autoSaveAfterTrain;
        private final double initialThreshold;
        private final int punishAfterFlags;
        private final double verboseProbabilityLogThreshold;
        private final String filterFile;
        private final String cleanerFile;
        private final String verifierFile;
        private final String mainFile;
        private final List<String> punishCommands;

        public ModelSection(int batchSize, double learningRate, double decay, int sequenceLength, int inputSize,
                            int hiddenSize, double clipGradient, double verifierThreshold,
                            double cleanerOutlierClamp, double filterSmoothing, boolean autoSaveAfterTrain,
                            double initialThreshold, int punishAfterFlags, double verboseProbabilityLogThreshold,
                            String filterFile, String cleanerFile, String verifierFile, String mainFile,
                            List<String> punishCommands) {
            this.batchSize = batchSize;
            this.learningRate = learningRate;
            this.decay = decay;
            this.sequenceLength = sequenceLength;
            this.inputSize = inputSize;
            this.hiddenSize = hiddenSize;
            this.clipGradient = clipGradient;
            this.verifierThreshold = verifierThreshold;
            this.cleanerOutlierClamp = cleanerOutlierClamp;
            this.filterSmoothing = filterSmoothing;
            this.autoSaveAfterTrain = autoSaveAfterTrain;
            this.initialThreshold = initialThreshold;
            this.punishAfterFlags = punishAfterFlags;
            this.verboseProbabilityLogThreshold = verboseProbabilityLogThreshold;
            this.filterFile = filterFile;
            this.cleanerFile = cleanerFile;
            this.verifierFile = verifierFile;
            this.mainFile = mainFile;
            this.punishCommands = punishCommands;
        }

        public int getBatchSize() { return batchSize; }
        public double getLearningRate() { return learningRate; }
        public double getDecay() { return decay; }
        public int getSequenceLength() { return sequenceLength; }
        public int getInputSize() { return inputSize; }
        public int getHiddenSize() { return hiddenSize; }
        public double getClipGradient() { return clipGradient; }
        public double getVerifierThreshold() { return verifierThreshold; }
        public double getCleanerOutlierClamp() { return cleanerOutlierClamp; }
        public double getFilterSmoothing() { return filterSmoothing; }
        public boolean isAutoSaveAfterTrain() { return autoSaveAfterTrain; }
        public double getInitialThreshold() { return initialThreshold; }
        public int getPunishAfterFlags() { return punishAfterFlags; }
        public double getVerboseProbabilityLogThreshold() { return verboseProbabilityLogThreshold; }
        public String getFilterFile() { return filterFile; }
        public String getCleanerFile() { return cleanerFile; }
        public String getVerifierFile() { return verifierFile; }
        public String getMainFile() { return mainFile; }
        public List<String> getPunishCommands() { return punishCommands; }
    }

    public static final class CheckSection {
        private final boolean enabled;
        private final double threshold;
        private final int minimumCombatFrames;
        private final long cooldownBetweenAlertsMs;
        private final double rotationSnapThreshold;
        private final double impossibleSmoothnessThreshold;
        private final double silentAimAngleThreshold;
        private final double trackingConsistencyThreshold;
        private final int aimLockDurationTicks;
        private final int cpsHardLimit;
        private final boolean setbackEnabled;
        private final boolean punishEnabled;
        private final String punishmentMode;
        private final long minimumPredictionIntervalMs;
        private final double probabilitySmoothingFactor;
        private final int minimumAlertStreak;
        private final int watchThresholdPercent;
        private final int mediumThresholdPercent;
        private final int terminalThresholdPercent;
        private final int watchViolationGain;
        private final int mediumViolationGain;

        public CheckSection(boolean enabled, double threshold, int minimumCombatFrames, long cooldownBetweenAlertsMs,
                            double rotationSnapThreshold, double impossibleSmoothnessThreshold,
                            double silentAimAngleThreshold, double trackingConsistencyThreshold,
                            int aimLockDurationTicks, int cpsHardLimit, boolean setbackEnabled,
                            boolean punishEnabled, String punishmentMode, long minimumPredictionIntervalMs,
                            double probabilitySmoothingFactor, int minimumAlertStreak, int watchThresholdPercent,
                            int mediumThresholdPercent, int terminalThresholdPercent, int watchViolationGain,
                            int mediumViolationGain) {
            this.enabled = enabled;
            this.threshold = threshold;
            this.minimumCombatFrames = minimumCombatFrames;
            this.cooldownBetweenAlertsMs = cooldownBetweenAlertsMs;
            this.rotationSnapThreshold = rotationSnapThreshold;
            this.impossibleSmoothnessThreshold = impossibleSmoothnessThreshold;
            this.silentAimAngleThreshold = silentAimAngleThreshold;
            this.trackingConsistencyThreshold = trackingConsistencyThreshold;
            this.aimLockDurationTicks = aimLockDurationTicks;
            this.cpsHardLimit = cpsHardLimit;
            this.setbackEnabled = setbackEnabled;
            this.punishEnabled = punishEnabled;
            this.punishmentMode = punishmentMode;
            this.minimumPredictionIntervalMs = minimumPredictionIntervalMs;
            this.probabilitySmoothingFactor = probabilitySmoothingFactor;
            this.minimumAlertStreak = minimumAlertStreak;
            this.watchThresholdPercent = watchThresholdPercent;
            this.mediumThresholdPercent = mediumThresholdPercent;
            this.terminalThresholdPercent = terminalThresholdPercent;
            this.watchViolationGain = watchViolationGain;
            this.mediumViolationGain = mediumViolationGain;
        }

        public boolean isEnabled() { return enabled; }
        public double getThreshold() { return threshold; }
        public int getMinimumCombatFrames() { return minimumCombatFrames; }
        public long getCooldownBetweenAlertsMs() { return cooldownBetweenAlertsMs; }
        public double getRotationSnapThreshold() { return rotationSnapThreshold; }
        public double getImpossibleSmoothnessThreshold() { return impossibleSmoothnessThreshold; }
        public double getSilentAimAngleThreshold() { return silentAimAngleThreshold; }
        public double getTrackingConsistencyThreshold() { return trackingConsistencyThreshold; }
        public int getAimLockDurationTicks() { return aimLockDurationTicks; }
        public int getCpsHardLimit() { return cpsHardLimit; }
        public boolean isSetbackEnabled() { return setbackEnabled; }
        public boolean isPunishEnabled() { return punishEnabled; }
        public String getPunishmentMode() { return punishmentMode; }
        public long getMinimumPredictionIntervalMs() { return minimumPredictionIntervalMs; }
        public double getProbabilitySmoothingFactor() { return probabilitySmoothingFactor; }
        public int getMinimumAlertStreak() { return minimumAlertStreak; }
        public int getWatchThresholdPercent() { return watchThresholdPercent; }
        public int getMediumThresholdPercent() { return mediumThresholdPercent; }
        public int getTerminalThresholdPercent() { return terminalThresholdPercent; }
        public int getWatchViolationGain() { return watchViolationGain; }
        public int getMediumViolationGain() { return mediumViolationGain; }
    }

    public static final class MessageSection {
        private final String noPermission;
        private final String reloaded;
        private final String debugOn;
        private final String debugOff;
        private final String alertsOn;
        private final String alertsOff;
        private final String recordStart;
        private final String recordStop;
        private final String recordFull;
        private final String trainStart;
        private final String trainFinish;
        private final String probabilityLine;
        private final String alertFormat;
        private final String mlHeader;
        private final String mlEntry;
        private final String sanctionReason;

        public MessageSection(String noPermission, String reloaded, String debugOn, String debugOff,
                              String alertsOn, String alertsOff, String recordStart, String recordStop,
                              String recordFull, String trainStart, String trainFinish, String probabilityLine,
                              String alertFormat, String mlHeader, String mlEntry, String sanctionReason) {
            this.noPermission = noPermission;
            this.reloaded = reloaded;
            this.debugOn = debugOn;
            this.debugOff = debugOff;
            this.alertsOn = alertsOn;
            this.alertsOff = alertsOff;
            this.recordStart = recordStart;
            this.recordStop = recordStop;
            this.recordFull = recordFull;
            this.trainStart = trainStart;
            this.trainFinish = trainFinish;
            this.probabilityLine = probabilityLine;
            this.alertFormat = alertFormat;
            this.mlHeader = mlHeader;
            this.mlEntry = mlEntry;
            this.sanctionReason = sanctionReason;
        }

        public String getNoPermission() { return noPermission; }
        public String getReloaded() { return reloaded; }
        public String getDebugOn() { return debugOn; }
        public String getDebugOff() { return debugOff; }
        public String getAlertsOn() { return alertsOn; }
        public String getAlertsOff() { return alertsOff; }
        public String getRecordStart() { return recordStart; }
        public String getRecordStop() { return recordStop; }
        public String getRecordFull() { return recordFull; }
        public String getTrainStart() { return trainStart; }
        public String getTrainFinish() { return trainFinish; }
        public String getProbabilityLine() { return probabilityLine; }
        public String getAlertFormat() { return alertFormat; }
        public String getMlHeader() { return mlHeader; }
        public String getMlEntry() { return mlEntry; }
        public String getSanctionReason() { return sanctionReason; }
    }
}
