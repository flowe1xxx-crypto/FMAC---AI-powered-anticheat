package dev.famesti.fmanticheat.dataset;

import java.io.File;

public final class DatasetSummary {

    private final File file;
    private final DatasetLabel label;
    private final int frames;
    private final long bytes;

    public DatasetSummary(File file, DatasetLabel label, int frames, long bytes) {
        this.file = file;
        this.label = label;
        this.frames = frames;
        this.bytes = bytes;
    }

    public File getFile() { return file; }
    public DatasetLabel getLabel() { return label; }
    public int getFrames() { return frames; }
    public long getBytes() { return bytes; }
}
