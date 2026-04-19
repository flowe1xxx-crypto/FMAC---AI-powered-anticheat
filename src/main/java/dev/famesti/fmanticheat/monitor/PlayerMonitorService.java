package dev.famesti.fmanticheat.monitor;

import dev.famesti.fmanticheat.config.Settings;
import dev.famesti.fmanticheat.dataset.CombatFeatureFrame;
import dev.famesti.fmanticheat.detection.AimFeatureExtractor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerMonitorService {

    private final JavaPlugin plugin;
    private final Settings settings;
    private final AimFeatureExtractor extractor = new AimFeatureExtractor();
    private final Map<UUID, PlayerCombatSnapshot> snapshots = new ConcurrentHashMap<UUID, PlayerCombatSnapshot>();
    private CombatFrameSink frameSink;
    private BukkitTask task;
    private volatile long tickCounter;

    public PlayerMonitorService(JavaPlugin plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void start() {
        int period = Math.max(1, settings.getPlugin().getMonitorIntervalTicks());
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tickCounter++;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerCombatSnapshot snapshot = getOrCreate(player);
                    snapshot.updateMovement(player);
                    if (snapshot.isInCombatWindow()) {
                        CombatFeatureFrame frame = extractor.extract(player, snapshot, tickCounter);
                        snapshot.pushFrame(frame, settings.getModels().getSequenceLength() * 4);
                        if (frameSink != null) {
                            frameSink.handle(player, frame);
                        }
                    }
                }
            }
        }, 1L, period);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        snapshots.clear();
    }

    public PlayerCombatSnapshot getOrCreate(Player player) {
        PlayerCombatSnapshot snapshot = snapshots.get(player.getUniqueId());
        if (snapshot == null) {
            snapshot = new PlayerCombatSnapshot(player);
            snapshots.put(player.getUniqueId(), snapshot);
        }
        return snapshot;
    }

    public void remove(Player player) {
        snapshots.remove(player.getUniqueId());
    }

    public Collection<CombatFeatureFrame> getFrames(Player player) {
        PlayerCombatSnapshot snapshot = snapshots.get(player.getUniqueId());
        return snapshot == null ? Collections.<CombatFeatureFrame>emptyList() : snapshot.getRecentFrames();
    }

    public Map<UUID, PlayerCombatSnapshot> getSnapshots() {
        return snapshots;
    }

    public void setFrameSink(CombatFrameSink frameSink) {
        this.frameSink = frameSink;
    }

    public interface CombatFrameSink {
        void handle(Player player, CombatFeatureFrame frame);
    }
}
