# FM AntiCheat

FM AntiCheat is a machine-assisted anti-cheat plugin for Bukkit-compatible servers. It combines handcrafted combat heuristics, player telemetry smoothing, and a custom Java RNN pipeline to detect Kill-Aim, Aimbot, aim-lock, silent aim, and suspicious tracking patterns with fast in-game enforcement.

## Highlights

- Real-time suspicion scoring with accelerated sanction flow
- Tiered enforcement model:
  - `65%` confidence -> `+20 VL`
  - `70%` confidence -> `+60 VL`
  - `94%+` confidence -> immediate terminal punishment path
- Custom minimal startup banner intended for clean GitHub screenshots and public releases
- Modern watch HUD, styled punish animation, and compact operator-facing alerts
- Dataset recording and model lifecycle commands for iterative tuning

## Architecture

FM AntiCheat is organized as a layered detection pipeline:

1. `listener/`
   Captures movement, combat hits, joins, quits, and operator actions.
2. `monitor/`
   Builds `PlayerCombatSnapshot` state, tracks combat windows, click rate, rotation changes, suspicion buffers, and smoothed probability values.
3. `dataset/`
   Serializes feature frames for training and validation datasets.
4. `ml/`
   Runs the model repository, preprocessing stages, and the custom recurrent model pipeline:
   `filter.dat -> cleaner.dat -> verifier.dat -> main.dat`
5. `detection/`
   Merges heuristic evidence and model output, then applies alerting, VL escalation, animation, kick, or ban policy.
6. `ui/`
   Renders the startup banner, themed action bar HUD, and styled operator-facing output.

## Detection Strategy

The detector evaluates multiple signals per combat session:

- Rotation snap intensity
- Aim smoothness outside human bounds
- Target tracking consistency
- CPS pressure and burst behavior
- Silent aim style low-angle correction
- Micro-correction clustering
- Aim-lock duration and combat pressure
- RNN probability calibration and smoothing

The final probability is a blended score from:

- Heuristic combat evidence
- Calibrated model output
- Suspicion buffer history
- Probability smoothing to reduce noise without slowing reaction too much

## Sanction Model

Default sanction policy in this repository:

- `65%` or more -> `+20 VL`
- `70%` or more -> `+60 VL`
- `94%` or more -> terminal path with immediate ban handling
- Kick/ban reason message:

```text
Система подозревает вас в ЧИТАХ!
                 Ложно? @flowe1x
```

This policy is configurable in `src/main/resources/config.yml`.

## Commands

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

## Installation

1. Build the plugin with Maven:

```powershell
mvn clean test package
```

2. Copy `target/fm-anticheat.jar` into your server `plugins/` directory.
3. Start the server once to generate runtime folders and model files.
4. Review and tune `plugins/FM-AntiCheat/config.yml`.
5. Restart or run `/fm reload`.

## Configuration Notes

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

## Compatibility

- Primary API target: `Spigot 1.16.5`
- Conservative Bukkit API usage for broad compatibility with `1.13+`
- Intended to run on modern Bukkit-family engines such as `Spigot`, `Paper`, and similar forks that keep the relevant APIs stable
- Java build target: `release 8`

If you plan to run on a newer engine version, validate combat events, action bar output, and punish command semantics on a staging server before production rollout.

## Testing

The project includes unit-level validation for:

- Sanction thresholds at `65%`, `70%`, and `94%+`
- Kick/ban message formatting
- UI theme rendering across severity bands
- Lightweight performance checks for repeated threshold resolution and HUD rendering

Run tests with:

```powershell
mvn test
```

## Repository Layout

- `src/main/java/dev/famesti/fmanticheat` -> plugin source
- `src/main/resources/config.yml` -> default configuration
- `src/main/resources/docs` -> bundled technical docs
- `src/main/resources/reference` -> reference assets and seed pack
- `src/test/java` -> threshold, formatting, and performance validation tests
- `github-ready/` -> curated publication folder for GitHub upload

## Collaboration

If you want to improve model quality together, share your own labeled datasets or combat recordings through Telegram: `@VIadyaso`.

Useful contribution directions:

- legit vs cheat combat samples
- edge cases from modern server forks
- false positive reproductions
- annotated aim-assist or silent-aim traces

The goal is a mutually beneficial loop: better datasets improve the model, and a better model reduces false positives for everyone using the project.
