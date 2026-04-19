package dev.famesti.fmanticheat.detection;

import dev.famesti.fmanticheat.config.Settings;
import dev.famesti.fmanticheat.ui.InterfaceTheme;
import dev.famesti.fmanticheat.util.ColorUtil;
import dev.famesti.fmanticheat.util.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PunishmentAnimationService {

    private static final int DURATION_TICKS = 18;
    private static final long TASK_PERIOD_TICKS = 1L;
    private static final double TOTAL_LIFT_HEIGHT = 4.6D;

    private final JavaPlugin plugin;
    private final Settings settings;
    private final Map<UUID, AnimationSession> sessions = new ConcurrentHashMap<UUID, AnimationSession>();
    private final Map<UUID, String> pendingKickMessages = new ConcurrentHashMap<UUID, String>();

    public PunishmentAnimationService(JavaPlugin plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public boolean start(Player player, double probability, String reason, PunishmentMode mode, int violationLevel) {
        if (player == null || !player.isOnline() || player.hasPermission("fmanticheat.bypass")) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        if (sessions.containsKey(playerId)) {
            return false;
        }
        AnimationSession session = new AnimationSession(
                playerId,
                player.getName(),
                player.getWalkSpeed(),
                player.getFlySpeed(),
                player.getLocation().getY(),
                player.getLocation().getX(),
                player.getLocation().getZ(),
                FormatUtil.percentInt(probability),
                buildKickMessage(probability, reason, mode, violationLevel),
                mode
        );
        sessions.put(playerId, session);
        beginEffects(player);
        session.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tick(session);
            }
        }, 1L, TASK_PERIOD_TICKS);
        return true;
    }

    public boolean isAnimating(Player player) {
        return player != null && sessions.containsKey(player.getUniqueId());
    }

    public Location lockMovement(Player player, Location from, Location to) {
        if (player == null || from == null || to == null) {
            return to;
        }
        AnimationSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return to;
        }
        Location locked = to.clone();
        locked.setX(session.lockedX);
        locked.setZ(session.lockedZ);
        return locked;
    }

    public void handleQuit(Player player) {
        if (player == null) {
            return;
        }
        AnimationSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        cancelTask(session);
        executePunishCommands(session.playerName, session.mode, session.kickMessage);
        if (session.mode == PunishmentMode.KICK) {
            pendingKickMessages.put(session.playerId, session.kickMessage);
        }
    }

    public void handleJoin(final Player player) {
        if (player == null) {
            return;
        }
        final String message = pendingKickMessages.remove(player.getUniqueId());
        if (message == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.kickPlayer(ColorUtil.colorize(message));
                }
            }
        });
    }

    public void shutdown() {
        for (AnimationSession session : sessions.values()) {
            cancelTask(session);
        }
        sessions.clear();
        pendingKickMessages.clear();
    }

    private void tick(AnimationSession session) {
        Player player = Bukkit.getPlayer(session.playerId);
        if (player == null || !player.isOnline()) {
            sessions.remove(session.playerId);
            cancelTask(session);
            pendingKickMessages.put(session.playerId, session.kickMessage);
            return;
        }
        session.elapsedTicks += TASK_PERIOD_TICKS;
        double progress = Math.min(1.0D, session.elapsedTicks / (double) DURATION_TICKS);
        double easedHeight = easeOutCubic(progress) * TOTAL_LIFT_HEIGHT;
        double hoverOffset = Math.sin(progress * Math.PI * 4.0D) * 0.08D * (1.0D - progress);
        Location lifted = player.getLocation().clone();
        lifted.setX(session.lockedX);
        lifted.setZ(session.lockedZ);
        lifted.setY(session.startY + easedHeight + hoverOffset);
        player.teleport(lifted);
        player.setFallDistance(0.0F);
        player.setVelocity(player.getVelocity().setX(0.0D).setY(0.0D).setZ(0.0D));
        playVisuals(player, session);
        ColorUtil.action(player, InterfaceTheme.formatPunishmentAction((int) Math.round(progress * 100.0D), session.probabilityPercent));
        if (session.elapsedTicks % 10 == 0) {
            player.sendTitle(
                    ColorUtil.colorize("&#ff4d6d&lFMAC"),
                    ColorUtil.colorize("&#f8f9fa&lSanction Lock"),
                    0,
                    12,
                    6
            );
        }
        if (session.elapsedTicks >= DURATION_TICKS) {
            finish(session, player, true);
        }
    }

    private void beginEffects(Player player) {
        player.setWalkSpeed(0.0F);
        player.setFlySpeed(0.0F);
        player.setGlowing(true);
        player.setFallDistance(0.0F);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, DURATION_TICKS + 20, 10, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, DURATION_TICKS + 20, 200, false, false, false));
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6F, 1.35F);
    }

    private void playVisuals(Player player, AnimationSession session) {
        World world = player.getWorld();
        Location center = player.getLocation().clone().add(0.0D, 1.05D, 0.0D);
        double phase = session.elapsedTicks * 0.33D;
        for (int i = 0; i < 12; i++) {
            double angle = phase + (Math.PI * 2.0D * i / 12.0D);
            double radius = 0.95D + Math.sin(phase + i * 0.45D) * 0.18D;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = 0.15D + ((i % 4) * 0.22D);
            world.spawnParticle(Particle.FIREWORKS_SPARK, center.clone().add(x, y, z), 3, 0.03D, 0.03D, 0.03D, 0.01D);
        }
        if (session.elapsedTicks % 6 == 0) {
            world.spawnParticle(Particle.END_ROD, center, 12, 0.22D, 0.32D, 0.22D, 0.01D);
            world.spawnParticle(Particle.SMOKE_NORMAL, center, 6, 0.14D, 0.24D, 0.14D, 0.01D);
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.32F, 1.7F);
        }
    }

    private void finish(AnimationSession session, Player player, boolean executeCommands) {
        sessions.remove(session.playerId);
        cancelTask(session);
        clearEffects(player, session);
        if (executeCommands) {
            executePunishCommands(session.playerName, session.mode, session.kickMessage);
        }
        if (player.isOnline()) {
            player.kickPlayer(ColorUtil.colorize(session.kickMessage));
        } else if (session.mode == PunishmentMode.KICK) {
            pendingKickMessages.put(session.playerId, session.kickMessage);
        }
    }

    private void clearEffects(Player player, AnimationSession session) {
        if (player == null) {
            return;
        }
        player.setWalkSpeed(session.originalWalkSpeed);
        player.setFlySpeed(session.originalFlySpeed);
        player.setGlowing(false);
        player.removePotionEffect(PotionEffectType.SLOW);
        player.removePotionEffect(PotionEffectType.JUMP);
    }

    private void cancelTask(AnimationSession session) {
        if (session.task != null) {
            session.task.cancel();
            session.task = null;
        }
    }

    private void executePunishCommands(String playerName, PunishmentMode mode, String kickMessage) {
        if (mode == PunishmentMode.KICK) {
            Player target = Bukkit.getPlayerExact(playerName);
            if (target != null && target.isOnline()) {
                target.kickPlayer(ColorUtil.colorize(kickMessage));
            }
            return;
        }
        if (mode == PunishmentMode.ALERT) {
            return;
        }
        if (settings.getModels().getPunishCommands().isEmpty()) {
            String inlineReason = settings.getMessages().getSanctionReason().replace('\n', ' ').trim();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + playerName + " " + inlineReason);
            return;
        }
        for (String command : settings.getModels().getPunishCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", playerName));
        }
    }

    private String buildKickMessage(double probability, String reason, PunishmentMode mode, int violationLevel) {
        return SanctionMessageFormatter.format(
                settings.getMessages().getSanctionReason(),
                probability,
                reason,
                violationLevel,
                mode
        );
    }

    private double easeOutCubic(double progress) {
        double inverse = 1.0D - progress;
        return 1.0D - (inverse * inverse * inverse);
    }

    private static final class AnimationSession {
        private final UUID playerId;
        private final String playerName;
        private final float originalWalkSpeed;
        private final float originalFlySpeed;
        private final double startY;
        private final double lockedX;
        private final double lockedZ;
        private final int probabilityPercent;
        private final String kickMessage;
        private final PunishmentMode mode;
        private int elapsedTicks;
        private BukkitTask task;

        private AnimationSession(UUID playerId, String playerName, float originalWalkSpeed, float originalFlySpeed,
                                 double startY, double lockedX, double lockedZ, int probabilityPercent,
                                 String kickMessage, PunishmentMode mode) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.originalWalkSpeed = originalWalkSpeed;
            this.originalFlySpeed = originalFlySpeed;
            this.startY = startY;
            this.lockedX = lockedX;
            this.lockedZ = lockedZ;
            this.probabilityPercent = probabilityPercent;
            this.kickMessage = kickMessage;
            this.mode = mode;
        }
    }
}
