package dev.famesti.fmanticheat.ml.train;

import dev.famesti.fmanticheat.dataset.CombatFeatureFrame;
import dev.famesti.fmanticheat.dataset.DatasetLabel;
import dev.famesti.fmanticheat.data.PluginDirectories;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DatasetLoader {

    private final JavaPlugin plugin;
    private final PluginDirectories directories;

    public DatasetLoader(JavaPlugin plugin, PluginDirectories directories) {
        this.plugin = plugin;
        this.directories = directories;
    }

    public List<TrainingSequence> loadSequences(int sequenceLength) {
        File[] files = directories.getDatasetsFolder().listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<TrainingSequence> sequences = new ArrayList<TrainingSequence>();
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".dat")) {
                readFile(file, sequenceLength, sequences);
            }
        }
        return sequences;
    }

    private void readFile(File file, int sequenceLength, List<TrainingSequence> sequences) {
        DataInputStream in = null;
        try {
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            String header = in.readUTF();
            if (!"FMAC-DATASET-v1".equals(header)) {
                return;
            }
            int labelId = in.readInt();
            in.readLong();
            int frames = in.readInt();
            List<CombatFeatureFrame> buffer = new ArrayList<CombatFeatureFrame>(frames);
            for (int i = 0; i < frames; i++) {
                buffer.add(CombatFeatureFrame.read(in));
            }
            int label = labelId == DatasetLabel.CHEAT.getId() ? 1 : 0;
            for (int offset = 0; offset + sequenceLength <= buffer.size(); offset += Math.max(1, sequenceLength / 3)) {
                float[][] sequence = new float[sequenceLength][];
                for (int i = 0; i < sequenceLength; i++) {
                    sequence[i] = buffer.get(offset + i).getFeatures();
                }
                sequences.add(new TrainingSequence(sequence, label));
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to parse dataset " + file.getName() + ": " + ex.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
