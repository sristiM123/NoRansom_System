# Threat model

## In scope

NoRansom targets the *impact stage* of ransomware on IoT gateways and small file
stores: rapid encryption of files on disk. The observable behaviors it detects:

1. **High-entropy writes** — encrypted output is statistically near-random
   (~8.0 bits/byte Shannon entropy vs. 3.5–5.5 for typical text/telemetry).
2. **Rename storms** — mass extension changes (e.g. `*.bin` → `*.locked`).
3. **Burst activity** — many file operations across many files in seconds,
   unlike slow, periodic IoT telemetry.
4. **Mass deletion** — destructive wipers and some ransomware families.

## Out of scope

- Initial access, lateral movement, C2 traffic (network-level controls needed).
- In-memory or exfiltration-only attacks that never write encrypted data locally.
- Slow, throttled encryption designed to stay under burst thresholds
  (raising detection cost for the attacker is still a win).
- Kernel-level evasion of filesystem notifications.

## Assumptions

- The agent host is not yet fully compromised at detection time (detection races
  encryption; the goal is early containment, not prevention).
- The controller runs on a trusted management network. The demo API is
  unauthenticated by design and must not be internet-exposed.
