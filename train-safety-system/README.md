# Train Safety Expert System

Implementation of the project proposal *Ekspertski sistem za bezbednost
železničkog saobraćaja*. A Drools-based on-board safety system that
continuously monitors a simulated train, computes the ETCS-style braking
curves (W / SBD / EBD), and intervenes when needed.

## Build & run

Requires Java 11 (the project uses Drools 7.49 + Spring Boot 2.7).

```bash
cd train-safety-system/
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
./mvnw clean install -DskipTests
./mvnw spring-boot:run -pl service 
```

Then open **http://localhost:8080/**.

## Modules

| Module | Contents |
|--------|---------|
| `model`   | Facts (`TrainStatus`, `Infrastructure`, `MovementAuthority`, `BrakingCurve`, `Car`, `Coupling`, `TrainProfile`, `AdhesionConditions`, …) and CEP events (`Warning`, `DriverActivity`, `WheelSlipEvent`, `*BrakeCommand`, …) |
| `kjar`    | `.drl` rule files in 4 packages (forward / accumulate / cep / backward) plus `.drt` rule templates (Rules 2 & 14) |
| `service` | Spring Boot application, simulation engine (200 ms tick), REST controllers, static DMI frontend (HTML/CSS/JS) |

## Rule layout (matches proposal §1–§5)

| Section | Files | Rules |
|--------|-------|-------|
| Forward chaining   | `rules/forward/forward-rules.drl`       | Rules 1, 3–7 |
| Accumulate         | `rules/accumulate/accumulate-rules.drl` | Rule 10 |
| CEP (stream mode)  | `rules/cep/cep-rules.drl`               | Rules 11–13, 14b |
| Backward chaining  | `rules/backward/backward-queries.drl`   | Rules 15–16 (with recursive sub-queries) |
| Templates          | `templates/templates.drt`               | Rules 2 & 14 |

## REST API

| Endpoint | Description |
|---------|-------------|
| `GET  /api/state`                           | Full simulation snapshot (polled at 5 Hz by the frontend) |
| `POST /api/sim/{start,stop,reset}`          | Lifecycle |
| `POST /api/driver/throttle?value=0..1`      | Throttle |
| `POST /api/driver/brake?value=0..1`         | Driver brake handle |
| `POST /api/driver/alertness`                | SIFA reset button |
| `POST /api/config/weather?value=Dry|Wet|Snow|Ice` | Switch active weather condition (Rule 2 & 14) |
| `POST /api/config/profile?value=Cargo|Passenger|HighSpeed` | Switch active TrainProfile (Rule 2 & 14) |
| `POST /api/scenario/open-doors?carId=...`   | Open doors on a car (triggers Rule 6 + Rule 7) |
| `POST /api/scenario/lock-doors`             | Lock all doors |
| `POST /api/scenario/fail-brake-test?carId=...` | Mark a car's brake test as FAILED |
| `POST /api/scenario/pass-brake-tests`       | Reset to PASSED on all cars |
| `POST /api/scenario/damage-coupling?couplingId=...` | Mark coupling DAMAGED (Rule 16 query will detect) |
| `POST /api/scenario/repair-couplings`       | Reset couplings |
| `POST /api/scenario/wheel-slip?count=N&magnitude=M` | Inject N slip events of magnitude M (Rule 14) |
| `POST /api/scenario/gsmr/{on|off}`          | GSM-R / RBC handshake toggle |
| `POST /api/scenario/clear-breaches`         | Clear `CriticalSafetyBreach` + re-arm traction |
| `GET  /api/checks/safe-to-depart`           | Rule 15 query (with per-subgoal breakdown) |
| `GET  /api/checks/safe-route-tree`           | Rule 16 query (`isSafeRouteTree` with breakdown) |
