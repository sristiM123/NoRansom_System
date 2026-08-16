# RansomGuard_System

A lightweight, behavior-based ransomware detection system for IoT and edge environments.
It detects ransomware by correlating short bursts of abnormal filesystem activity,
high-entropy writes, rename storms, mass deletion, rather than relying on signatures
or machine learning.

## What problem it solves

IoT devices and small organizations are frequent ransomware targets but often cannot
run heavy endpoint security tools. RansomGuard_System provides early detection and containment
using simple, explainable, and efficient logic suitable for constrained systems.

## How it works

1. A lightweight, dependency-free Java agent watches device folders recursively
   (create / modify / delete, with renames inferred from atomic-move event pairs).
2. On file writes, the agent computes **Shannon entropy** over the file head;
   values ≥ 7.5 bits/byte (typical of encrypted output) raise an `entropy_high` signal.
3. Events stream to a Spring Boot controller via REST (`POST /api/ingest`).
4. The controller keeps per-device rolling windows, scores each event
   (see `docs/scoring-model.md`), and correlates bursts.
5. Burst score thresholds trigger alerts of escalating severity, with cooldown
   and dedupe to prevent alert spam.
6. Devices can be quarantined/released from the dashboard.

## Components

| Module | What it is |
|---|---|
| `controller/` | Spring Boot backend: ingest API, scoring, correlation, alerts, dashboard |
| `agent-lite/` | JDK-only filesystem watcher agent (no external dependencies) |
| `simulator-safe/` | Two-phase workload generator (normal traffic, then a contained ransomware-like burst) |

## Alert types

- `entropy_spike` — high-entropy writes combined with modification bursts
- `rename_storm` — many renames in a short window
- `mass_deletion` — many deletions in a short window
- `ransomware_warning` / `ransomware_high` / `ransomware_critical` — burst-score severity tiers
- `quarantine` / `release` — containment actions

## Build & run

Requires Java 17+ and Maven.

```bash
# build everything
mvn -q package

# 1. start the controller (dashboard at http://localhost:9004)
cd controller && mvn spring-boot:run

# 2. start the agent (new terminal, from repo root)
java -jar agent-lite/target/agent-lite.jar
#    optional args: [rootDir] [ingestUrl]

# 3. run the safe attack simulation (new terminal, from repo root)
java -jar simulator-safe/target/simulator-safe.jar
#    optional args: [rootDir] [normalSeconds] [attackSeconds]
```

Watch the dashboard: during the normal phase, low-severity activity; within seconds of
the ransom-like phase, entropy and rename alerts escalate and the device can be quarantined.

The simulator only ever writes inside `iot_test/DeviceA` (enforced by a `SafePath`
guard) — it never touches real data.

## Known limitations (by design, for a local demo)

- **No authentication** on the API — do not expose the controller beyond localhost.
- **In-memory stores** — events, alerts, and device state reset on restart.
- Entropy analysis reads the file head (first 4 KB) — a deliberate trade-off for
  low overhead on constrained devices.
- Detection thresholds are static; see `docs/scoring-model.md` for tuning notes.

## Docs

- [`docs/threat-model.md`](docs/threat-model.md) — what NoRansom defends against, and what it doesn't
- [`docs/scoring-model.md`](docs/scoring-model.md) — event points, windows, and thresholds
- [`docs/demo-step.md`](docs/demo-step.md) — step-by-step demo walkthrough

## License

MIT — see [LICENSE](LICENSE).
