package dev.famesti.fmanticheat;

import dev.famesti.fmanticheat.command.FmCommand;
import dev.famesti.fmanticheat.config.Settings;
import dev.famesti.fmanticheat.data.PluginDirectories;
import dev.famesti.fmanticheat.dataset.CombatDatasetWriter;
import dev.famesti.fmanticheat.detection.AlertService;
import dev.famesti.fmanticheat.detection.DetectionService;
import dev.famesti.fmanticheat.detection.PunishmentAnimationService;
import dev.famesti.fmanticheat.listener.CombatListener;
import dev.famesti.fmanticheat.listener.JoinListener;
import dev.famesti.fmanticheat.listener.MovementListener;
import dev.famesti.fmanticheat.listener.QuitListener;
import dev.famesti.fmanticheat.ml.ModelPipeline;
import dev.famesti.fmanticheat.ml.ModelRepository;
import dev.famesti.fmanticheat.ml.train.TrainingService;
import dev.famesti.fmanticheat.monitor.PlayerMonitorService;
import dev.famesti.fmanticheat.monitor.ProbabilityWatchService;
import dev.famesti.fmanticheat.ui.BannerPrinter;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FmAntiCheatPlugin extends JavaPlugin {

    private Settings settings;
    private PluginDirectories directories;
    private ModelRepository modelRepository;
    private ModelPipeline modelPipeline;
    private CombatDatasetWriter datasetWriter;
    private PlayerMonitorService monitorService;
    private DetectionService detectionService;
    private AlertService alertService;
    private PunishmentAnimationService punishmentAnimationService;
    private ProbabilityWatchService probabilityWatchService;
    private TrainingService trainingService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = new Settings(this);
        this.directories = new PluginDirectories(this, settings);
        directories.ensure();

        this.modelRepository = new ModelRepository(this, directories, settings);
        this.modelRepository.loadOrCreateDefaults();
        if (settings.getPlugin().isBannerEnabled()) {
            BannerPrinter.print(modelRepository);
        }
        this.modelPipeline = new ModelPipeline(this, modelRepository, settings);
        this.datasetWriter = new CombatDatasetWriter(this, directories, settings);
        this.punishmentAnimationService = new PunishmentAnimationService(this, settings);
        this.alertService = new AlertService(this, settings, punishmentAnimationService);
        this.monitorService = new PlayerMonitorService(this, settings);
        this.detectionService = new DetectionService(this, settings, modelPipeline, monitorService, alertService);
        this.monitorService.setFrameSink(new PlayerMonitorService.CombatFrameSink() {
            @Override
            public void handle(org.bukkit.entity.Player player, dev.famesti.fmanticheat.dataset.CombatFeatureFrame frame) {
                if (datasetWriter.isRecording(player)) {
                    datasetWriter.append(player, frame);
                }
                detectionService.evaluate(player);
            }
        });
        this.probabilityWatchService = new ProbabilityWatchService(this, settings, detectionService);
        this.trainingService = new TrainingService(this, settings, directories, modelRepository, modelPipeline);
        exportBundledResources();

        getServer().getPluginManager().registerEvents(new JoinListener(punishmentAnimationService), this);
        getServer().getPluginManager().registerEvents(new MovementListener(monitorService, punishmentAnimationService), this);
        getServer().getPluginManager().registerEvents(new CombatListener(settings, monitorService, datasetWriter), this);
        getServer().getPluginManager().registerEvents(new QuitListener(monitorService, datasetWriter, probabilityWatchService, punishmentAnimationService), this);

        FmCommand fmCommand = new FmCommand(this, settings, modelRepository, datasetWriter, trainingService, probabilityWatchService, alertService);
        PluginCommand command = getCommand("fm");
        if (command != null) {
            command.setExecutor(fmCommand);
            command.setTabCompleter(fmCommand);
        }

        monitorService.start();
        probabilityWatchService.start();
        if (!settings.getPlugin().isMinimalStartupOutput()) {
            getLogger().info("FM AntiCheat enabled successfully.");
        }
    }

    @Override
    public void onDisable() {
        if (trainingService != null) {
            trainingService.shutdown();
        }
        if (datasetWriter != null) {
            datasetWriter.shutdown();
        }
        if (punishmentAnimationService != null) {
            punishmentAnimationService.shutdown();
        }
        if (monitorService != null) {
            monitorService.shutdown();
        }
        if (modelRepository != null) {
            modelRepository.saveAll();
        }
        getLogger().info("FM AntiCheat disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        this.settings.reload();
        this.directories.ensure();
        this.modelRepository.loadOrCreateDefaults();
    }

    private void exportBundledResources() {
        saveResourceSilently("docs/feature-manual.md");
        saveResourceSilently("docs/dataset-format.md");
        saveResourceSilently("reference/aim-pattern-library.csv");
        saveResourceSilently("reference/pretrain-seed.bin");
    }

    private void saveResourceSilently(String path) {
        try {
            saveResource(path, false);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public ModelPipeline getModelPipeline() {
        return modelPipeline;
    }

    public PlayerMonitorService getMonitorService() {
        return monitorService;
    }
}
