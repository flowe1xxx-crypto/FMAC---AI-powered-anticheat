package dev.famesti.fmanticheat.data;

import dev.famesti.fmanticheat.config.Settings;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class PluginDirectories {

    private final JavaPlugin plugin;
    private final Settings settings;
    private File dataFolder;
    private File datasetsFolder;
    private File modelsFolder;
    private File docsFolder;

    public PluginDirectories(JavaPlugin plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void ensure() {
        this.dataFolder = plugin.getDataFolder();
        this.datasetsFolder = new File(dataFolder, settings.getDatasets().getFolder());
        this.modelsFolder = new File(dataFolder, "models");
        this.docsFolder = new File(dataFolder, "docs");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        datasetsFolder.mkdirs();
        modelsFolder.mkdirs();
        docsFolder.mkdirs();
    }

    public File getDataFolder() { return dataFolder; }
    public File getDatasetsFolder() { return datasetsFolder; }
    public File getModelsFolder() { return modelsFolder; }
    public File getDocsFolder() { return docsFolder; }
}
