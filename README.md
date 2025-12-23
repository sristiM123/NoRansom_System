# NoRansom_System

It is a lightweight, behavior-based ransomware detection system designed for IoT and edge environments.  
It detects ransomware by correlating short bursts of abnormal filesystem activity rather than relying on signatures or machine learning.

---

## 🚩 What Problem It Solves

IoT devices and small organizations are frequent ransomware targets but often cannot run heavy endpoint security tools.  
It provides early detection and containment using simple, explainable, and efficient logic suitable for constrained systems.

---

## 🧠 How It Works

1. A lightweight Java agent monitors filesystem activity (create, modify, delete, rename).
2. Events are sent to a Spring Boot controller via REST APIs.
3. Events are scored and correlated within short rolling time windows.
4. Abnormal burst behavior triggers automatic alerts with escalating severity.
5. Devices can be quarantined to prevent further damage or lateral spread.

---

## 🏗️ Components

- **Controller**: Spring Boot backend handling events, alerts, scoring, and dashboard.
- **Agent**: Java-based filesystem watcher for IoT gateways or storage locations.
- **Dashboard**: Displays devices, events, alerts, and quarantine status.

---

## 🚨 Alerting Logic

ChronoDefend generates different alert levels based on behavior intensity:

- `ransomware_warning` – early suspicious activity
- `ransomware_high` – strong ransomware indication
- `ransomware_critical` – confirmed ransomware-like behavior
- `manual_quarantine` / `auto_quarantine` – containment actions

Severity is derived from aggregated behavior, not fixed values.

---

## ▶️ Run the Project

### Start Controller
bash
cd controller
mvn spring-boot:run

# DashBoard
http://localhost:9004

# Start Agent
cd agent-lite
java -jar agent.jar demo/deviceA

# Simulate Ransomware
powershell ./demo/simulate_ransomware.ps1


