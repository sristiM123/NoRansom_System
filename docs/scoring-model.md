# Scoring model

## Per-event points (ScoringService)

| Event type | Points | Rationale |
|---|---|---|
| `file_created` | 1 | Normal activity, weak signal alone |
| `file_modified` | 2 | Slightly stronger — content churn |
| `file_deleted` | 3 | Destructive operation |
| `file_renamed` | 4 | Extension changes are a classic ransomware tell |
| `burst` signals | 4 | Volume anomaly |
| `entropy_high` | 5 | Strongest single indicator: encrypted-looking content |

Events tagged `_sim_attack` receive +2 (demo aid only).

## Windows

- **Scoring window:** rolling 120 s per device — the score passed to correlation
  is the sum of points in this window.
- **Correlation window:** rolling 10 s per device — burst features (op counts,
  unique files, entropy/rename signals) are computed here.

## Alert thresholds (CorrelationService)

| Burst score | Alert |
|---|---|
| ≥ 8 | `ransomware_warning` (severity 6) |
| ≥ 12 | `ransomware_high` (severity 8) |
| ≥ 18 | `ransomware_critical` (severity 10) |

Feature-based classification takes precedence when evidence is specific:
`entropy_spike`, `rename_storm` (≥3 renames), `mass_deletion` (≥3 deletes).

## Anti-spam

- 20 s cooldown between auto-alerts per device.
- Identical consecutive burst signatures are deduped.

## Tuning notes

- Entropy threshold 7.5 bits/byte balances sensitivity against compressed-file
  false positives (JPEG/ZIP also sit near 8.0 — a known trade-off; pair with
  rename/burst evidence before quarantining).
- Thresholds are static; per-device baselining is the natural next step.
