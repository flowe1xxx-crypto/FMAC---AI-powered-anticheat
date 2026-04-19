package dev.famesti.fmanticheat.dataset;

import dev.famesti.fmanticheat.config.Settings;
import dev.famesti.fmanticheat.data.PluginDirectories;
import dev.famesti.fmanticheat.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatDatasetWriter {

    private final JavaPlugin plugin;
    private final PluginDirectories directories;
    private final Settings settings;
    private final Map<UUID, RecordingSession> sessions = new ConcurrentHashMap<UUID, RecordingSession>();

    public CombatDatasetWriter(JavaPlugin plugin, PluginDirectories directories, Settings settings) {
        this.plugin = plugin;
        this.directories = directories;
        this.settings = settings;
    }

    public boolean isRecording(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public String start(Player player, DatasetLabel label, int durationSeconds) {
        if (sessions.size() >= settings.getDatasets().getMaxConcurrentRecordings()) {
            return "Достигнут лимит одновременных записей.";
        }
        if (isRecording(player)) {
            return "Для игрока " + player.getName() + " уже идет запись. Используй /fm record stop " + player.getName();
        }
        File file = new File(directories.getDatasetsFolder(), buildName(player.getName(), label, 1));
        RecordingSession session = new RecordingSession(player.getUniqueId(), player.getName(), label, file, System.currentTimeMillis(), durationSeconds);
        sessions.put(player.getUniqueId(), session);
        return null;
    }

    public DatasetSummary stop(Player player, boolean save) {
        RecordingSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return null;
        }
        if (!save || session.frames.size() < settings.getDatasets().getMinFramesToSave()) {
            if (session.file.exists()) {
                session.file.delete();
            }
            return null;
        }
        return writeSession(session);
    }

    public DatasetSummary append(Player player, CombatFeatureFrame frame) {
        RecordingSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return null;
        }
        if (session.lastTick == frame.getTick()) {
            return null;
        }
        session.lastTick = frame.getTick();
        session.frames.add(frame);
        session.rotationExamples++;
        maybeUpdateActionBar(player, session);
        long elapsedSeconds = (System.currentTimeMillis() - session.startedAt) / 1000L;
        if (session.durationSeconds > 0 && elapsedSeconds >= session.durationSeconds) {
            DatasetSummary summary = stop(player, true);
            if (summary != null) {
                ColorUtil.send(player, settings.getMessages().getRecordStop().replace("%file%", summary.getFile().getName()));
            }
            return summary;
        }
        if (settings.getDatasets().isAutoStopWhenFull() && estimateSize(session) >= settings.getDatasets().getTargetSizeBytes()) {
            DatasetSummary summary = writeSession(session);
            if (summary != null) {
                ColorUtil.send(player, decorate(settings.getMessages().getRecordFull())
                        .replace("%player%", session.playerName)
                        .replace("%label%", session.label.name().toLowerCase())
                        .replace("%file%", summary.getFile().getName())
                        .replace("%index%", String.valueOf(session.fileIndex)));
                ColorUtil.send(player, decorate(settings.getMessages().getRecordStop())
                        .replace("%player%", session.playerName)
                        .replace("%label%", session.label.name().toLowerCase())
                        .replace("%file%", summary.getFile().getName())
                        .replace("%index%", String.valueOf(session.fileIndex)));
            }
            rotateSession(player, session);
            return summary;
        }
        return null;
    }

    public void updateActionBar(Player player) {
        RecordingSession session = sessions.get(player.getUniqueId());
        if (session == null || !settings.getDatasets().isActionBarEnabled()) {
            return;
        }
        session.lastActionBarAt = System.currentTimeMillis();
        sendActionBar(player, session);
    }

    private void maybeUpdateActionBar(Player player, RecordingSession session) {
        if (!settings.getDatasets().isActionBarEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - session.lastActionBarAt >= 900L) {
            session.lastActionBarAt = now;
            sendActionBar(player, session);
        }
    }

    private void sendActionBar(Player player, RecordingSession session) {
        int progress = (int) Math.min(100.0D, (estimateSize(session) * 100.0D) / settings.getDatasets().getTargetSizeBytes());
        String message = settings.getDatasets().getActionBarTemplate()
                .replace("%player%", session.playerName)
                .replace("%samples%", String.valueOf(countDatasetFiles()))
                .replace("%progress%", String.valueOf(progress))
                .replace("%rotations%", String.valueOf(session.rotationExamples))
                .replace("%label%", session.label.name().toLowerCase())
                .replace("%part%", String.valueOf(session.fileIndex))
                .replace("%bar%", ColorUtil.progressBar(progress));
        ColorUtil.action(player, message);
    }

    public int countDatasetFiles() {
        File[] files = directories.getDatasetsFolder().listFiles();
        if (files == null) {
            return 0;
        }
        int count = 0;
        for (File file : files) {
            if (file.getName().endsWith(".dat")) {
                count++;
            }
        }
        return count;
    }

    public List<DatasetSummary> listDatasets() {
        File[] files = directories.getDatasetsFolder().listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<DatasetSummary> result = new ArrayList<DatasetSummary>();
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".dat")) {
                DatasetLabel label = file.getName().contains("_cheat_") ? DatasetLabel.CHEAT : DatasetLabel.LEGIT;
                result.add(new DatasetSummary(file, label, 0, file.length()));
            }
        }
        return result;
    }

    public void shutdown() {
        sessions.clear();
    }

    private DatasetSummary writeSession(RecordingSession session) {
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(session.file)));
            out.writeUTF("FMAC-DATASET-v1");
            out.writeInt(session.label.getId());
            out.writeLong(session.startedAt);
            out.writeInt(session.frames.size());
            for (CombatFeatureFrame frame : session.frames) {
                frame.write(out);
            }
            out.flush();
            return new DatasetSummary(session.file, session.label, session.frames.size(), session.file.length());
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to write dataset " + session.file.getName() + ": " + ex.getMessage());
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private int estimateSize(RecordingSession session) {
        return 32 + session.frames.size() * 88;
    }

    private void rotateSession(Player player, RecordingSession session) {
        session.fileIndex++;
        session.file = new File(directories.getDatasetsFolder(), buildName(session.playerName, session.label, session.fileIndex));
        session.frames.clear();
        session.rotationExamples = 0;
        session.lastTick = Long.MIN_VALUE;
        session.lastActionBarAt = 0L;
        ColorUtil.send(player, decorate(settings.getMessages().getRecordStart())
                .replace("%player%", session.playerName)
                .replace("%label%", session.label.name().toLowerCase())
                .replace("%file%", session.file.getName())
                .replace("%index%", String.valueOf(session.fileIndex)));
    }

    private String buildName(String playerName, DatasetLabel label, int index) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return timestamp + "_" + label.name().toLowerCase() + "_" + playerName + "_p" + formatIndex(index) + ".dat";
    }

    private String formatIndex(int index) {
        return index < 10 ? "0" + index : String.valueOf(index);
    }

    private String decorate(String message) {
        return "&8[&6FM&8] &r" + message;
    }

    private static final class RecordingSession {
        private final UUID playerId;
        private final String playerName;
        private final DatasetLabel label;
        private File file;
        private final long startedAt;
        private final int durationSeconds;
        private final List<CombatFeatureFrame> frames = new ArrayList<CombatFeatureFrame>();
        private int rotationExamples;
        private long lastActionBarAt;
        private long lastTick = Long.MIN_VALUE;
        private int fileIndex = 1;

        private RecordingSession(UUID playerId, String playerName, DatasetLabel label, File file, long startedAt, int durationSeconds) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.label = label;
            this.file = file;
            this.startedAt = startedAt;
            this.durationSeconds = durationSeconds;
        }
    }
}
