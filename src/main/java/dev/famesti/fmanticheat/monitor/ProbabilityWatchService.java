package dev.famesti.fmanticheat.monitor;

import dev.famesti.fmanticheat.config.Settings;
import dev.famesti.fmanticheat.detection.DetectionService;
import dev.famesti.fmanticheat.ui.InterfaceTheme;
import dev.famesti.fmanticheat.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProbabilityWatchService {

    private final JavaPlugin plugin;
    private final Settings settings;
    private final DetectionService detectionService;
    private final Map<UUID, UUID> watchers = new ConcurrentHashMap<UUID, UUID>();
    private BukkitTask task;

    public ProbabilityWatchService(JavaPlugin plugin, Settings settings, DetectionService detectionService) {
        this.plugin = plugin;
        this.settings = settings;
        this.detectionService = detectionService;
    }

    public void start() {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, UUID> entry : watchers.entrySet()) {
                    Player viewer = Bukkit.getPlayer(entry.getKey());
                    Player target = Bukkit.getPlayer(entry.getValue());
                    if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline()) {
                        watchers.remove(entry.getKey());
                        continue;
                    }
                    double probability = detectionService.getProbability(target);
                    ColorUtil.action(viewer, InterfaceTheme.formatWatcherHud(target.getName(), probability));
                }
            }
        }, 20L, Math.max(20L, settings.getPlugin().getProbabilityBroadcastIntervalTicks()));
    }

    public void toggle(CommandSender sender, Player target) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда доступна только игроку.");
            return;
        }
        Player viewer = (Player) sender;
        UUID viewerId = viewer.getUniqueId();
        if (watchers.containsKey(viewerId) && watchers.get(viewerId).equals(target.getUniqueId())) {
            watchers.remove(viewerId);
            ColorUtil.send(viewer, "&7Просмотр вероятности остановлен.");
            return;
        }
        watchers.put(viewerId, target.getUniqueId());
        ColorUtil.send(viewer, "&aТеперь показывается вероятность для &f" + target.getName());
    }

    public void remove(Player player) {
        watchers.remove(player.getUniqueId());
        for (Map.Entry<UUID, UUID> entry : watchers.entrySet()) {
            if (entry.getValue().equals(player.getUniqueId())) {
                watchers.remove(entry.getKey());
            }
        }
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        watchers.clear();
    }
}
