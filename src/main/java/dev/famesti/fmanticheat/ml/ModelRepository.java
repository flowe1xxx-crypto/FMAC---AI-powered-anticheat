package dev.famesti.fmanticheat.ml;

import dev.famesti.fmanticheat.config.Settings;
import dev.famesti.fmanticheat.data.PluginDirectories;
import dev.famesti.fmanticheat.ml.model.FamestiRnnModel;
import dev.famesti.fmanticheat.ml.preprocess.CleanerStage;
import dev.famesti.fmanticheat.ml.preprocess.FilterStage;
import dev.famesti.fmanticheat.ml.preprocess.VerifierStage;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class ModelRepository {

    private final JavaPlugin plugin;
    private final PluginDirectories directories;
    private final Settings settings;
    private FilterStage filterStage;
    private CleanerStage cleanerStage;
    private VerifierStage verifierStage;
    private FamestiRnnModel mainModel;
    private long lastUpdated;

    public ModelRepository(JavaPlugin plugin, PluginDirectories directories, Settings settings) {
        this.plugin = plugin;
        this.directories = directories;
        this.settings = settings;
    }

    public void loadOrCreateDefaults() {
        ensureBundledModelsPresent();
        this.filterStage = (FilterStage) read(settings.getModels().getFilterFile(), FilterStage.class);
        if (filterStage == null) {
            filterStage = new FilterStage(settings.getModels().getFilterSmoothing());
            write(settings.getModels().getFilterFile(), filterStage);
        }
        this.cleanerStage = (CleanerStage) read(settings.getModels().getCleanerFile(), CleanerStage.class);
        if (cleanerStage == null) {
            cleanerStage = new CleanerStage(settings.getModels().getCleanerOutlierClamp());
            write(settings.getModels().getCleanerFile(), cleanerStage);
        }
        this.verifierStage = (VerifierStage) read(settings.getModels().getVerifierFile(), VerifierStage.class);
        if (verifierStage == null) {
            verifierStage = new VerifierStage(settings.getModels().getInputSize());
            write(settings.getModels().getVerifierFile(), verifierStage);
        }
        this.mainModel = (FamestiRnnModel) read(settings.getModels().getMainFile(), FamestiRnnModel.class);
        if (mainModel == null
                || mainModel.getInputSize() != settings.getModels().getInputSize()
                || mainModel.getHiddenSize() != settings.getModels().getHiddenSize()) {
            if (mainModel != null && !settings.getPlugin().isMinimalStartupOutput()) {
                plugin.getLogger().info("Rebuilding main model to match configured shape: input="
                        + settings.getModels().getInputSize() + " hidden=" + settings.getModels().getHiddenSize());
            }
            mainModel = new FamestiRnnModel(settings.getModels().getInputSize(), settings.getModels().getHiddenSize(), 55_881L);
            write(settings.getModels().getMainFile(), mainModel);
        }
        this.lastUpdated = System.currentTimeMillis();
    }

    public void saveAll() {
        write(settings.getModels().getFilterFile(), filterStage);
        write(settings.getModels().getCleanerFile(), cleanerStage);
        write(settings.getModels().getVerifierFile(), verifierStage);
        write(settings.getModels().getMainFile(), mainModel);
        lastUpdated = System.currentTimeMillis();
    }

    public FilterStage getFilterStage() { return filterStage; }
    public CleanerStage getCleanerStage() { return cleanerStage; }
    public VerifierStage getVerifierStage() { return verifierStage; }
    public FamestiRnnModel getMainModel() { return mainModel; }
    public long getLastUpdated() { return lastUpdated; }

    private void ensureBundledModelsPresent() {
        exportBundledModel(settings.getModels().getFilterFile());
        exportBundledModel(settings.getModels().getCleanerFile());
        exportBundledModel(settings.getModels().getVerifierFile());
        exportBundledModel(settings.getModels().getMainFile());
    }

    private void exportBundledModel(String fileName) {
        File file = new File(directories.getModelsFolder(), fileName);
        if (file.exists()) {
            return;
        }
        InputStream input = plugin.getResource("models/" + fileName);
        if (input == null) {
            return;
        }
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
            if (!settings.getPlugin().isMinimalStartupOutput()) {
                plugin.getLogger().info("Loaded bundled base model: " + fileName);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to export bundled model " + fileName + ": " + ex.getMessage());
        } finally {
            try {
                input.close();
            } catch (Exception ignored) {
            }
            if (output != null) {
                try {
                    output.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private Object read(String fileName, Class<?> expected) {
        File file = new File(directories.getModelsFolder(), fileName);
        if (!file.exists()) {
            return null;
        }
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(file));
            Object object = in.readObject();
            return expected.isInstance(object) ? object : null;
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to read model " + fileName + ": " + ex.getMessage());
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void write(String fileName, Object object) {
        File file = new File(directories.getModelsFolder(), fileName);
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(object);
            out.flush();
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to write model " + fileName + ": " + ex.getMessage());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
