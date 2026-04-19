package dev.famesti.fmanticheat.command;

import dev.famesti.fmanticheat.FmAntiCheatPlugin;
import dev.famesti.fmanticheat.config.Settings;
import dev.famesti.fmanticheat.dataset.CombatDatasetWriter;
import dev.famesti.fmanticheat.dataset.DatasetLabel;
import dev.famesti.fmanticheat.dataset.DatasetSummary;
import dev.famesti.fmanticheat.detection.AlertService;
import dev.famesti.fmanticheat.detection.PunishmentMode;
import dev.famesti.fmanticheat.ml.ModelRepository;
import dev.famesti.fmanticheat.ml.train.TrainingService;
import dev.famesti.fmanticheat.monitor.PlayerCombatSnapshot;
import dev.famesti.fmanticheat.monitor.ProbabilityWatchService;
import dev.famesti.fmanticheat.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public final class FmCommand implements CommandExecutor, TabCompleter {

    private final FmAntiCheatPlugin plugin;
    private final Settings settings;
    private final ModelRepository modelRepository;
    private final CombatDatasetWriter datasetWriter;
    private final TrainingService trainingService;
    private final ProbabilityWatchService probabilityWatchService;
    private final AlertService alertService;

    public FmCommand(FmAntiCheatPlugin plugin, Settings settings, ModelRepository modelRepository,
                     CombatDatasetWriter datasetWriter, TrainingService trainingService,
                     ProbabilityWatchService probabilityWatchService, AlertService alertService) {
        this.plugin = plugin;
        this.settings = settings;
        this.modelRepository = modelRepository;
        this.datasetWriter = datasetWriter;
        this.trainingService = trainingService;
        this.probabilityWatchService = probabilityWatchService;
        this.alertService = alertService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("train".equals(sub)) {
            if (!sender.hasPermission("fmanticheat.train")) {
                ColorUtil.send(sender, settings.getMessages().getNoPermission());
                return true;
            }
            if (args.length < 3) {
                ColorUtil.send(sender, "&cИспользование: /fm train <model_index> <epochs>");
                return true;
            }
            int modelIndex = parseInt(args[1], 4);
            int epochs = parseInt(args[2], 10);
            ColorUtil.send(sender, settings.getMessages().getTrainStart()
                    .replace("%model%", trainingService.resolveModelName(modelIndex))
                    .replace("%epochs%", String.valueOf(epochs)));
            trainingService.trainAsync(sender, modelIndex, epochs);
            return true;
        }
        if ("record".equals(sub)) {
            if (!sender.hasPermission("fmanticheat.record")) {
                ColorUtil.send(sender, settings.getMessages().getNoPermission());
                return true;
            }
            handleRecord(sender, args);
            return true;
        }
        if ("prob".equals(sub)) {
            if (!sender.hasPermission("fmanticheat.prob")) {
                ColorUtil.send(sender, settings.getMessages().getNoPermission());
                return true;
            }
            if (args.length < 2) {
                ColorUtil.send(sender, "&cИспользование: /fm prob <player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                ColorUtil.send(sender, "&cИгрок не найден.");
                return true;
            }
            probabilityWatchService.toggle(sender, target);
            return true;
        }
        if ("debug".equals(sub)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Команда только для игрока.");
                return true;
            }
            if (!sender.hasPermission("fmanticheat.debug")) {
                ColorUtil.send(sender, settings.getMessages().getNoPermission());
                return true;
            }
            Player player = (Player) sender;
            PlayerCombatSnapshot snapshot = plugin.getMonitorService().getOrCreate(player);
            snapshot.setDebug(!snapshot.isDebug());
            ColorUtil.send(player, snapshot.isDebug() ? settings.getMessages().getDebugOn() : settings.getMessages().getDebugOff());
            return true;
        }
        if ("alert".equals(sub)) {
            if (!sender.hasPermission("fmanticheat.alert")) {
                ColorUtil.send(sender, settings.getMessages().getNoPermission());
                return true;
            }
            boolean enabled = alertService.toggleAlerts(sender);
            ColorUtil.send(sender, enabled ? settings.getMessages().getAlertsOn() : settings.getMessages().getAlertsOff());
            return true;
        }
        if ("detect".equals(sub)) {
            if (!sender.hasPermission("fmanticheat.reload")) {
                ColorUtil.send(sender, settings.getMessages().getNoPermission());
                return true;
            }
            handleDetect(sender, args);
            return true;
        }
        if ("reload".equals(sub)) {
            if (!sender.hasPermission("fmanticheat.reload")) {
                ColorUtil.send(sender, settings.getMessages().getNoPermission());
                return true;
            }
            plugin.reloadPlugin();
            ColorUtil.send(sender, settings.getMessages().getReloaded());
            return true;
        }
        if ("ml".equals(sub)) {
            if (!sender.hasPermission("fmanticheat.ml")) {
                ColorUtil.send(sender, settings.getMessages().getNoPermission());
                return true;
            }
            handleMl(sender, args);
            return true;
        }
        sendHelp(sender);
        return true;
    }

    private void handleMl(CommandSender sender, String[] args) {
        ColorUtil.send(sender, settings.getMessages().getMlHeader());
        if (args.length == 1 || "list".equalsIgnoreCase(args[1])) {
            sendModelLine(sender, "filter.dat");
            sendModelLine(sender, "cleaner.dat");
            sendModelLine(sender, "verifier.dat");
            sendModelLine(sender, "main.dat / Famesti-publik-45k params=" + modelRepository.getMainModel().parameterCount());
            ColorUtil.send(sender, "&7Датасетов: &f" + datasetWriter.countDatasetFiles());
            return;
        }
        if ("save".equalsIgnoreCase(args[1])) {
            modelRepository.saveAll();
            ColorUtil.send(sender, "&aВсе модели сохранены в папку plugins/FM-AntiCheat/models.");
            return;
        }
        if ("reload".equalsIgnoreCase(args[1])) {
            modelRepository.loadOrCreateDefaults();
            ColorUtil.send(sender, "&aМодели перезагружены с диска.");
            return;
        }
        if ("hyper".equalsIgnoreCase(args[1])) {
            ColorUtil.send(sender, "&eLR=&f" + settings.getModels().getLearningRate()
                    + " &8| &eSeq=&f" + settings.getModels().getSequenceLength()
                    + " &8| &eHidden=&f" + settings.getModels().getHiddenSize()
                    + " &8| &eThreshold=&f" + settings.getChecks().getThreshold());
            return;
        }
        ColorUtil.send(sender, "&7Подкоманды: list, save, reload, hyper");
    }

    private void sendModelLine(CommandSender sender, String name) {
        String updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(modelRepository.getLastUpdated()));
        ColorUtil.send(sender, settings.getMessages().getMlEntry()
                .replace("%name%", name)
                .replace("%loaded%", "true")
                .replace("%updated%", updated));
    }

    private void sendHelp(CommandSender sender) {
        ColorUtil.send(sender, "&#1f2235&l&m-----------------------------------------------------");
        ColorUtil.send(sender, "&#ff2d55&lFM ANTI-CHEAT &#ffd166&lCOMMANDS");
        ColorUtil.send(sender, "&#ff8a00/fm train &f<model_index> <epochs>");
        ColorUtil.send(sender, "&#ff8a00/fm ml &f<list|save|reload|hyper>");
        ColorUtil.send(sender, "&#ff8a00/fm record &f<legit|cheat> [duration]");
        ColorUtil.send(sender, "&#ff8a00/fm record &f<player...> <legit|cheat> [duration]");
        ColorUtil.send(sender, "&#ff8a00/fm record stop &f<player...|all>");
        ColorUtil.send(sender, "&#ff8a00/fm prob &f<player>");
        ColorUtil.send(sender, "&#ff8a00/fm debug");
        ColorUtil.send(sender, "&#ff8a00/fm alert");
        ColorUtil.send(sender, "&#ff8a00/fm detect &f<ban|kick|alert>");
        ColorUtil.send(sender, "&#ff8a00/fm reload");
        ColorUtil.send(sender, "&#1f2235&l&m-----------------------------------------------------");
    }

    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("train", "ml", "record", "prob", "debug", "alert", "detect", "reload"), args[0]);
        }
        if (args.length == 2 && "record".equalsIgnoreCase(args[0])) {
            List<String> result = new ArrayList<String>();
            result.add("stop");
            result.add("legit");
            result.add("легит");
            result.add("cheat");
            result.add("чит");
            for (Player player : Bukkit.getOnlinePlayers()) {
                result.add(player.getName());
            }
            return filter(result, args[1]);
        }
        if (args.length >= 3 && "record".equalsIgnoreCase(args[0])) {
            List<String> result = new ArrayList<String>();
            if (isStopToken(args[1])) {
                result.add("all");
                result.add("все");
            } else {
                result.add("legit");
                result.add("легит");
                result.add("cheat");
                result.add("чит");
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                result.add(player.getName());
            }
            return filter(result, args[args.length - 1]);
        }
        if (args.length == 2 && "ml".equalsIgnoreCase(args[0])) {
            return filter(Arrays.asList("list", "save", "reload", "hyper"), args[1]);
        }
        if (args.length == 2 && "prob".equalsIgnoreCase(args[0])) {
            List<String> result = new ArrayList<String>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                result.add(player.getName());
            }
            return filter(result, args[1]);
        }
        if (args.length == 2 && "detect".equalsIgnoreCase(args[0])) {
            return filter(Arrays.asList("ban", "kick", "alert"), args[1]);
        }
        return new ArrayList<String>();
    }

    private List<String> filter(List<String> base, String input) {
        List<String> result = new ArrayList<String>();
        for (String value : base) {
            if (value.toLowerCase().startsWith(input.toLowerCase())) {
                result.add(value);
            }
        }
        return result;
    }

    private void handleRecord(CommandSender sender, String[] args) {
        if (args.length < 2) {
            ColorUtil.send(sender, "&cИспользование: /fm record <player...> <legit|cheat> [duration]");
            return;
        }
        if (isStopToken(args[1])) {
            handleRecordStop(sender, args);
            return;
        }
        DatasetLabel selfLabel = DatasetLabel.parse(args[1]);
        if (selfLabel != null) {
            if (!(sender instanceof Player)) {
                ColorUtil.send(sender, "&cИз консоли укажи игроков: /fm record <player...> <legit|cheat> [duration]");
                return;
            }
            List<Player> targets = new ArrayList<Player>();
            targets.add((Player) sender);
            startRecordings(sender, targets, selfLabel, args.length >= 3 ? parseInt(args[2], 0) : 0);
            return;
        }

        int labelIndex = -1;
        DatasetLabel label = null;
        for (int i = 1; i < args.length; i++) {
            DatasetLabel parsed = DatasetLabel.parse(args[i]);
            if (parsed != null) {
                labelIndex = i;
                label = parsed;
                break;
            }
        }
        if (labelIndex <= 1 || label == null) {
            ColorUtil.send(sender, "&cИспользование: /fm record <player...> <legit|cheat> [duration]");
            return;
        }
        if (args.length > labelIndex + 2) {
            ColorUtil.send(sender, "&cСлишком много аргументов. Формат: /fm record <player...> <legit|cheat> [duration]");
            return;
        }

        List<Player> targets = new ArrayList<Player>();
        for (int i = 1; i < labelIndex; i++) {
            Player target = Bukkit.getPlayerExact(args[i]);
            if (target == null) {
                ColorUtil.send(sender, "&cИгрок &f" + args[i] + " &cне найден.");
                return;
            }
            if (!targets.contains(target)) {
                targets.add(target);
            }
        }
        if (targets.isEmpty()) {
            ColorUtil.send(sender, "&cУкажи хотя бы одного игрока.");
            return;
        }
        int duration = args.length == labelIndex + 2 ? parseInt(args[labelIndex + 1], 0) : 0;
        startRecordings(sender, targets, label, duration);
    }

    private void handleDetect(CommandSender sender, String[] args) {
        if (args.length < 2) {
            ColorUtil.send(sender, "&eТекущий режим наказания: &f" + alertService.getPunishmentMode().getId());
            ColorUtil.send(sender, "&cИспользование: /fm detect <ban|kick|alert>");
            return;
        }
        PunishmentMode mode = alertService.setPunishmentMode(args[1]);
        ColorUtil.send(sender, "&aРежим наказания обновлен: &f" + mode.getId()
                + " &7(при 100 VL)");
    }

    private void handleRecordStop(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ColorUtil.send(sender, "&cИспользование: /fm record stop <player...|all>");
            return;
        }
        if (isAllToken(args[2])) {
            int stopped = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (datasetWriter.isRecording(player)) {
                    stopRecording(sender, player);
                    stopped++;
                }
            }
            if (stopped == 0) {
                ColorUtil.send(sender, "&eСейчас нет активных записей.");
            }
            return;
        }
        for (int i = 2; i < args.length; i++) {
            Player target = Bukkit.getPlayerExact(args[i]);
            if (target == null) {
                ColorUtil.send(sender, "&cИгрок &f" + args[i] + " &cне найден.");
                continue;
            }
            stopRecording(sender, target);
        }
    }

    private void startRecordings(CommandSender sender, List<Player> targets, DatasetLabel label, int duration) {
        for (Player player : targets) {
            String error = datasetWriter.start(player, label, duration);
            if (error != null) {
                ColorUtil.send(sender, "&c" + error);
                continue;
            }
            String line = decorate(settings.getMessages().getRecordStart())
                    .replace("%player%", player.getName())
                    .replace("%label%", label.name().toLowerCase())
                    .replace("%file%", "pending")
                    .replace("%index%", "1");
            ColorUtil.send(sender, line);
            if (!sender.equals(player)) {
                ColorUtil.send(player, line);
            }
        }
    }

    private void stopRecording(CommandSender sender, Player player) {
        if (!datasetWriter.isRecording(player)) {
            ColorUtil.send(sender, "&eДля игрока &f" + player.getName() + " &eактивной записи нет.");
            return;
        }
        DatasetSummary summary = datasetWriter.stop(player, true);
        if (summary != null) {
            String line = decorate(settings.getMessages().getRecordStop())
                    .replace("%player%", player.getName())
                    .replace("%label%", "unknown")
                    .replace("%index%", "?")
                    .replace("%file%", summary.getFile().getName());
            ColorUtil.send(sender, line);
            if (!sender.equals(player)) {
                ColorUtil.send(player, line);
            }
            return;
        }
        String line = decorate("&7Запись для &f%player% &7остановлена без сохранения: мало кадров.")
                .replace("%player%", player.getName());
        ColorUtil.send(sender, line);
        if (!sender.equals(player)) {
            ColorUtil.send(player, line);
        }
    }

    private boolean isStopToken(String raw) {
        return "stop".equalsIgnoreCase(raw) || "стоп".equalsIgnoreCase(raw);
    }

    private boolean isAllToken(String raw) {
        return "all".equalsIgnoreCase(raw) || "все".equalsIgnoreCase(raw);
    }

    private String decorate(String message) {
        return "&#ff2d55&l[FMAC] &r" + message;
    }
}
