# FM AntiCheat

> AI-powered anti-cheat for Bukkit / Spigot / Paper with bundled base models, fast sanctioning, styled HUD, and open-source training workflows.

## ✨ Highlights

- ⚡ Real-time suspicion scoring with accelerated reaction time
- 🧠 Bundled base neural models inside the jar: `filter.dat`, `cleaner.dat`, `verifier.dat`, `main.dat`
- 🔥 Tiered enforcement model:
  - `65%` confidence -> `+20 VL`
  - `70%` confidence -> `+60 VL`
  - `94%+` confidence -> immediate terminal punishment path
- 🎨 Minimal startup output for clean GitHub screenshots
- 👁 Modern watcher HUD, styled punish animation, custom severity themes
- 🧪 Built-in unit tests and lightweight performance validation
- 🛠 Open-source friendly structure for retraining, replacing, or extending the base models

## 🧠 Bundled Base Models

This repository now ships with the four base model files directly inside the plugin jar:

- `filter.dat`
- `cleaner.dat`
- `verifier.dat`
- `main.dat`

On first server start, FM AntiCheat automatically copies these bundled models into the runtime `models/` folder if they are missing.

That means:

- the jar is usable out of the box
- GitHub contains the same open-source base model pack
- users can retrain, replace, tune, or fork the model stack however they want

## 🏗 Architecture

FM AntiCheat is organized as a layered detection pipeline:

1. `listener/`
   Captures movement, combat hits, joins, quits, and operator actions.
2. `monitor/`
   Builds `PlayerCombatSnapshot` state, tracks combat windows, click rate, rotation changes, suspicion buffers, and smoothed probability values.
3. `dataset/`
   Serializes feature frames for training and validation datasets.
4. `ml/`
   Runs the model repository, preprocessing stages, and the custom recurrent pipeline:
   `filter.dat -> cleaner.dat -> verifier.dat -> main.dat`
5. `detection/`
   Blends heuristic evidence and model output, then applies alerts, VL escalation, animation, kick, or ban.
6. `ui/`
   Renders the startup banner, themed HUD, styled action bars, and compact operator-facing output.

## 🎯 Detection Strategy

The detector evaluates multiple signals per combat session:

- rotation snap intensity
- aim smoothness outside human bounds
- target tracking consistency
- CPS pressure and burst behavior
- silent aim style low-angle correction
- micro-correction clustering
- aim-lock duration and combat pressure
- RNN probability calibration and smoothing

The final probability is a blended score from:

- heuristic combat evidence
- calibrated model output
- suspicion buffer history
- smoothing logic that reduces noise without making the system feel slow

## 🚨 Sanction Model

Default sanction policy in this repository:

- `65%` or more -> `+20 VL`
- `70%` or more -> `+60 VL`
- `94%` or more -> terminal path with immediate ban handling

Kick / ban reason:

```text
Система подозревает вас в ЧИТАХ!
                 Ложно? @flowe1x
```

This policy is configurable in `src/main/resources/config.yml`.

## 🎮 Visual Experience

The repository includes a visual-first presentation layer for public releases:

- clean startup banner with only banner + model info
- compact severity-based watcher HUD
- themed punish action bar
- styled sanction message layout
- minimal console noise for nicer GitHub screenshots and videos

## 📦 Commands

- `/fm train <model_index> <epochs>`
- `/fm ml <list|save|reload|hyper>`
- `/fm record <legit|cheat> [duration]`
- `/fm record <player...> <legit|cheat> [duration]`
- `/fm record stop <player...|all>`
- `/fm prob <player>`
- `/fm debug`
- `/fm alert`
- `/fm detect <ban|kick|alert>`
- `/fm reload`

## 🚀 Installation

1. Build the plugin:

```powershell
mvn clean test package
```

2. Copy `target/fm-anticheat.jar` into your server `plugins/` directory.
3. Start the server once.
4. The bundled base models are auto-exported into the plugin runtime `models/` folder.
5. Review and tune `plugins/FM-AntiCheat/config.yml`.
6. Restart or run `/fm reload`.

## ⚙️ Configuration Notes

Important tuning keys:

- `checks.threshold`
- `checks.cooldown-between-alerts-ms`
- `checks.minimum-prediction-interval-ms`
- `checks.probability-smoothing-factor`
- `checks.watch-threshold-percent`
- `checks.medium-threshold-percent`
- `checks.terminal-threshold-percent`
- `checks.watch-violation-gain`
- `checks.medium-violation-gain`
- `messages.sanction-reason`
- `plugin.minimal-startup-output`
- `models.model-files.filter`
- `models.model-files.cleaner`
- `models.model-files.verifier`
- `models.model-files.main`

## 🔓 Open Source Workflow

The default model pack is intentionally included so the repository is useful immediately.

You can:

- use the shipped base models as-is
- replace them with your own trained versions
- retrain from your own datasets
- fork the project and experiment with different detection pipelines
- publish tuned variants for your own servers

## 🧪 Testing

The project includes validation for:

- sanction thresholds at `65%`, `70%`, and `94%+`
- kick / ban message formatting
- UI theme rendering across severity bands
- lightweight performance checks for repeated threshold resolution and HUD rendering

Run tests with:

```powershell
mvn test
```

## 🧩 Compatibility

- primary API target: `Spigot 1.16.5`
- broad compatibility strategy for `1.13+`
- intended for `Spigot`, `Paper`, and similar Bukkit-family engines
- Java build target: `release 8`

If you run the plugin on a newer engine branch, validate combat events, action bar behavior, and punish command semantics on a staging server first.

## 📁 Repository Layout

- `src/main/java/dev/famesti/fmanticheat` -> plugin source
- `src/main/resources/config.yml` -> default configuration
- `src/main/resources/models` -> bundled base neural models packed into the jar
- `src/main/resources/docs` -> bundled technical docs
- `src/main/resources/reference` -> reference assets and seed pack
- `src/test/java` -> threshold, formatting, and performance validation tests
- `release/` -> packaged build artifact for quick deployment

## 🤝 Collaboration

If you want to improve model quality together, share your own labeled datasets or combat recordings through Telegram: `@VIadyaso`.

Useful collaboration directions:

- legit vs cheat combat samples
- edge cases from modern server forks
- false positive reproductions
- annotated aim-assist or silent-aim traces

The goal is simple: better shared datasets -> better models -> fewer false positives -> stronger open-source anti-cheat tooling for everyone.
