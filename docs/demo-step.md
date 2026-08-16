# Demo walkthrough

1. **Build:** `mvn -q package` from the repo root.
2. **Controller:** `cd controller && mvn spring-boot:run`, then open
   http://localhost:9004 — empty dashboard, no devices yet.
3. **Agent:** `java -jar agent-lite/target/agent-lite.jar` — five devices appear
   (heartbeats) within seconds.
4. **Simulation:** `java -jar simulator-safe/target/simulator-safe.jar`
   - *Phase 1 (normal):* slow sensor-log writes on DeviceA. Low-severity events;
     no alerts — small text files are filtered out of entropy analysis by size,
     and burst scores stay under thresholds.
   - *Phase 2 (ransom-like):* bursts of high-entropy files plus renames to
     `.locked`, contained to `iot_test/DeviceA/_sim_attack`.
5. **Observe:** within ~10 s of phase 2, alerts escalate
   (`entropy_spike` / `rename_storm` / `ransomware_*`).
6. **Contain:** select DeviceA → Quarantine. Status flips to QUARANTINED and a
   `quarantine` alert is logged. Release when done.
7. **Reset:** restart the controller (in-memory stores) and delete `iot_test/`.
