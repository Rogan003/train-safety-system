package com.ftn.sbnz.train.service.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ftn.sbnz.train.model.enums.BrakeTestStatus;
import com.ftn.sbnz.train.model.enums.DoorStatus;
import com.ftn.sbnz.train.model.enums.InteractionType;
import com.ftn.sbnz.train.model.enums.SifaStatus;
import com.ftn.sbnz.train.model.events.DriverActivity;
import com.ftn.sbnz.train.model.events.EmergencyBrakeCommand;
import com.ftn.sbnz.train.model.events.SandingCommand;
import com.ftn.sbnz.train.model.events.ServiceBrakeCommand;
import com.ftn.sbnz.train.model.events.Warning;
import com.ftn.sbnz.train.model.events.WheelSlipEvent;
import com.ftn.sbnz.train.model.facts.Balise;
import com.ftn.sbnz.train.model.facts.BrakingCurve;
import com.ftn.sbnz.train.model.facts.Car;
import com.ftn.sbnz.train.model.facts.CriticalSafetyBreach;
import com.ftn.sbnz.train.model.facts.GradientSegment;
import com.ftn.sbnz.train.model.facts.Infrastructure;
import com.ftn.sbnz.train.model.facts.MovementAuthority;
import com.ftn.sbnz.train.model.facts.RouteNode;
import com.ftn.sbnz.train.model.facts.TrainStatus;

/**
 * Owns the single long-lived {@link KieSession} for the running simulation
 * and steps physics on a fixed 200 ms tick. Each tick:
 *   1. Integrates kinematics (speed/position) honouring active executive commands
 *   2. Calls {@link KieSession#fireAllRules()} — Drools sees the new state
 *      and may emit Warning events / *BrakeCommand events / modify facts
 *   3. Drains and applies executive commands back to the physics state
 *
 * Synchronised so REST handlers safely mutate facts between ticks.
 */
@Component
public class SimulationEngine {

    private static final long TICK_MS = 200;
    private static final double G = 9.81; // m/s^2

    private final KieContainer kieContainer;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Drools session + handles
    private KieSession ksession;
    private FactHandle trainHandle;
    private FactHandle infraHandle;
    private FactHandle maHandle;
    private FactHandle bcHandle;
    private final List<FactHandle> carHandles    = new ArrayList<>();
    private final List<FactHandle> gradHandles   = new ArrayList<>();
    private final List<FactHandle> routeNodeHandles = new ArrayList<>();

    // Mirror references (so REST can mutate without searching working memory)
    private TrainStatus train;
    private Infrastructure infra;
    private MovementAuthority ma;
    private BrakingCurve bc;
    private final List<Car> cars = new ArrayList<>();
    private final List<Balise> balises = new ArrayList<>();
    private final List<GradientSegment> gradients = new ArrayList<>();
    private final List<RouteNode> routeNodes = new ArrayList<>();

    // Runtime state
    private volatile boolean running = false;
    private volatile boolean emergencyActive = false;
    private volatile boolean serviceBrakeActive = false;
    private volatile double serviceBrakeIntensity = 0.0;
    private volatile boolean sanding = false;
    private final CopyOnWriteArrayList<EventLog> eventLog = new CopyOnWriteArrayList<>();
    private ScheduledFuture<?> tickFuture;

    @Autowired
    public SimulationEngine(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    @PostConstruct
    public synchronized void init() {
        ksession = kieContainer.newKieSession("trainKsession");
        resetWorld();
        // Pre-fire to bootstrap template-derived TrainProfile/AdhesionConditions facts
        ksession.fireAllRules();
        appendLog("System initialised");
    }

    @PreDestroy
    public synchronized void shutdown() {
        running = false;
        if (tickFuture != null) tickFuture.cancel(false);
        scheduler.shutdownNow();
        if (ksession != null) ksession.dispose();
    }

    // ------------------------------------------------------------------
    // SIMULATION LIFECYCLE
    // ------------------------------------------------------------------

    public synchronized void start() {
        if (running) return;
        running = true;
        tickFuture = scheduler.scheduleAtFixedRate(this::safeTick, 0, TICK_MS, TimeUnit.MILLISECONDS);
        appendLog("Simulation started");
    }

    public synchronized void stop() {
        running = false;
        if (tickFuture != null) tickFuture.cancel(false);
        appendLog("Simulation stopped");
    }

    public synchronized void reset() {
        stop();
        if (ksession != null) ksession.dispose();
        ksession = kieContainer.newKieSession("trainKsession");
        eventLog.clear();
        emergencyActive = false;
        serviceBrakeActive = false;
        serviceBrakeIntensity = 0;
        sanding = false;
        resetWorld();
        ksession.fireAllRules();
        appendLog("Simulation reset");
    }

    public boolean isRunning() { return running; }

    // ------------------------------------------------------------------
    // INITIAL WORLD
    // ------------------------------------------------------------------

    /** Re-creates a default scenario: locomotive + 3 wagons, 2 km of track. */
    private void resetWorld() {
        cars.clear(); carHandles.clear();
        balises.clear();
        gradients.clear(); gradHandles.clear();
        routeNodes.clear(); routeNodeHandles.clear();

        // --- Composition: locomotive + 3 wagons --------------------------------
        Car loco = new Car("LOC-1", true, 90000, 95);
        loco.setBrakeTest(BrakeTestStatus.PASSED);
        loco.setDoorStatus(DoorStatus.LOCKED);

        Car w1 = new Car("WGN-1", false, 60000, 95);
        w1.setBrakeTest(BrakeTestStatus.PASSED);
        w1.setDoorStatus(DoorStatus.LOCKED);

        Car w2 = new Car("WGN-2", false, 60000, 95);
        w2.setBrakeTest(BrakeTestStatus.PASSED);
        w2.setDoorStatus(DoorStatus.LOCKED);

        Car w3 = new Car("WGN-3", false, 60000, 95);
        w3.setBrakeTest(BrakeTestStatus.PASSED);
        w3.setDoorStatus(DoorStatus.LOCKED);

        loco.setNextCar(w1);
        w1.setNextCar(w2);
        w2.setNextCar(w3);
        cars.add(loco); cars.add(w1); cars.add(w2); cars.add(w3);

        // --- Couplings (linked list, isFirst on the head) ----------------------
        // --- Gradient segments (per-mille) ------------------------------------
        gradients.add(new GradientSegment(   0,  500,  0));    // level
        gradients.add(new GradientSegment( 500, 1000,  5));    // 5‰ uphill
        gradients.add(new GradientSegment(1000, 1500, -8));    // 8‰ downhill — harder braking
        gradients.add(new GradientSegment(1500, 2500,  0));

        // --- Aggregate facts ---------------------------------------------------
        train = new TrainStatus();
        train.setSpeed(0);
        train.setPosition(0);
        train.setTrainType("Passenger");
        train.setSifaStatus(SifaStatus.OPERATIONAL);
        train.setTractionActive(false);

        infra = new Infrastructure();
        infra.setAdhesion(0.35);    // matches "Dry" default
        infra.setWeather("Dry");

        // --- Route Network (Tree topology) ---
        RouteNode leaf1 = new RouteNode("Platform 1", true);
        RouteNode leaf2 = new RouteNode("Platform 2", true);
        RouteNode leaf3 = new RouteNode("Platform 3", true);
        
        RouteNode switch2 = new RouteNode("Switch 2", false); // e.g. occupied or broken
        switch2.setLeft(leaf2);
        switch2.setRight(leaf3);
        
        RouteNode switch1 = new RouteNode("Switch 1", true);
        switch1.setLeft(leaf1);
        switch1.setRight(switch2);
        
        RouteNode startNode = new RouteNode("Station Entry", true);
        startNode.setLeft(switch1);
        
        routeNodes.add(leaf1); routeNodes.add(leaf2); routeNodes.add(leaf3);
        routeNodes.add(switch2); routeNodes.add(switch1); routeNodes.add(startNode);
        for (RouteNode n : routeNodes) routeNodeHandles.add(ksession.insert(n));

        // Balises embedded in Infrastructure per spec (linked to the Route Tree)
        balises.add(new Balise("BAL-001",  500,  3000, 120, startNode));
        balises.add(new Balise("BAL-002", 2000,  6000,  80, startNode));
        balises.add(new Balise("BAL-003", 5000,  8000,  60, startNode));
        balises.add(new Balise("BAL-004", 7500, 10000,  40, startNode));
        balises.add(new Balise("BAL-005", 9000, 10000,  20, startNode));
        balises.add(new Balise("BAL-006", 9800, 10000,   0, startNode));
        infra.setBalises(new ArrayList<>(balises));

        ma = new MovementAuthority(3000, 80);
        bc = new BrakingCurve();

        // --- Insert into Drools working memory --------------------------------
        trainHandle = ksession.insert(train);
        infraHandle = ksession.insert(infra);
        maHandle    = ksession.insert(ma);
        bcHandle    = ksession.insert(bc);
        for (Car c : cars)          carHandles.add(ksession.insert(c));
        for (GradientSegment g : gradients) gradHandles.add(ksession.insert(g));
    }

    // ------------------------------------------------------------------
    // TICK
    // ------------------------------------------------------------------

    private synchronized void safeTick() {
        try { tick(); } catch (Exception ex) {
            ex.printStackTrace();
            appendLog("ERROR: " + ex.getMessage());
        }
    }

    private void tick() {
        if (!running) return;

        // Random simulation events
        if (Math.random() < 0.001) {
            induceWheelSlip(3, 5.0);
        }
        if (Math.random() < 0.0001 && train.getSpeed() > 10) {
            openRandomDoor();
        }
        if (Math.random() < 0.0001) {
            blockRandomRouteNode();
        }

        // 1) Drain commands issued in the PREVIOUS rule cycle
        drainCommands();

        // 2) Integrate physics over TICK_MS
        double dtSec = TICK_MS / 1000.0;
        applyPhysics(dtSec);

        // 3) Update Drools that the state changed
        ksession.update(trainHandle, train);
        ksession.update(infraHandle, infra);
        ksession.update(maHandle, ma);
        ksession.update(bcHandle, bc);

        // 4) Run rules
        ksession.fireAllRules();
    }

    private void applyPhysics(double dt) {
        // Effective deceleration from braking-curve fact (already computed by Rule 2)
        double aBrake = Math.max(0.05, bc.getEffectiveDeceleration());

        // Gradient acceleration component (downhill speeds up, uphill slows)
        double gradAccel = -G * (infra.getGradient() / 1000.0); // positive grad = uphill → negative accel

        double throttleAccel = train.isTractionActive() ? (0.8 * train.getThrottle()) : 0.0;

        // Braking: emergency > service brake (auto) > driver brake handle
        double brakeAccel = 0.0;
        if (emergencyActive) {
            brakeAccel = -aBrake * 4.0;
        } else if (serviceBrakeActive) {
            brakeAccel = -aBrake * (serviceBrakeIntensity * 3.0);
        } else if (train.getBrake() > 0) {
            brakeAccel = -aBrake * (train.getBrake() * 3.0);
        }

        double totalAccel = throttleAccel + brakeAccel + gradAccel;
        double vMs = train.getSpeed() / 3.6;
        vMs += totalAccel * dt;
        if (vMs < 0) vMs = 0;
        double newSpeed = vMs * 3.6;
        double newPos   = train.getPosition() + vMs * dt;

        train.setSpeed(newSpeed);
        train.setPosition(newPos);

        // Wheel speed slowly tracks train speed — diverges when sanding/icy/skidding
        double wheelSpeed = train.getWheelSpeed();
        // adhesion-driven creep: lower mu = wheels more easily desync
        double targetWheel = newSpeed;
        // If user injected a slip, wheel speed will be higher than train (spinning).
        // Decay the difference back toward equilibrium each tick.
        if (Math.abs(wheelSpeed - targetWheel) > 0.5) {
            double convergence = Math.min(1.0, dt * (infra.getAdhesion() * 4.0));
            wheelSpeed += (targetWheel - wheelSpeed) * convergence;
        } else {
            wheelSpeed = targetWheel;
        }
        train.setWheelSpeed(wheelSpeed);

        // If sanding is active, slowly restore wheel-rail adhesion
        if (sanding) {
            double newMu = Math.min(0.35, infra.getAdhesion() + 0.02 * dt);
            infra.setAdhesion(newMu);
        }
    }

    private void drainCommands() {
        boolean hasEmergency = false;
        boolean hasService = false;
        double maxServiceIntensity = 0;
        boolean hasSanding = false;

        Collection<?> facts = ksession.getObjects();
        for (Object o : facts) {
            if (o instanceof EmergencyBrakeCommand) {
                hasEmergency = true;
                if (!emergencyActive) {
                    appendLog("EMERGENCY BRAKE active: " + ((EmergencyBrakeCommand) o).getReason());
                }
            } else if (o instanceof ServiceBrakeCommand) {
                hasService = true;
                ServiceBrakeCommand sbc = (ServiceBrakeCommand) o;
                if (sbc.getIntensity() > maxServiceIntensity) {
                    maxServiceIntensity = sbc.getIntensity();
                }
            } else if (o instanceof SandingCommand) {
                hasSanding = true;
            }
        }

        if (hasEmergency) {
            if (!emergencyActive) {
                emergencyActive = true;
                serviceBrakeActive = false;
                train.setTractionActive(false);
                train.setThrottle(0);
            }
        } else {
            if (emergencyActive && train.getSpeed() <= 0.01) {
                emergencyActive = false;
                appendLog("Train stopped — emergency brake released");
            }
        }

        if (!hasEmergency) {
            if (hasService) {
                if (!serviceBrakeActive || maxServiceIntensity > serviceBrakeIntensity) {
                    serviceBrakeActive = true;
                    serviceBrakeIntensity = maxServiceIntensity;
                    appendLog("Service brake active (" + maxServiceIntensity + ")");
                }
            } else if (serviceBrakeActive && train.getSpeed() < bc.getvServiceBrake() - 5) {
                serviceBrakeActive = false;
                serviceBrakeIntensity = 0;
                appendLog("Service brake released");
            }
        }

        if (hasSanding) {
            if (!sanding) {
                sanding = true;
                appendLog("Sanding ON");
            }
        } else if (sanding && !hasWheelSlipWarning()) {
            sanding = false;
            appendLog("Sanding OFF");
        }
    }

    private boolean hasActiveServiceCommand() {
        for (Object o : ksession.getObjects()) if (o instanceof ServiceBrakeCommand) return true;
        return false;
    }
    private boolean hasActiveSandingCommand() {
        for (Object o : ksession.getObjects()) if (o instanceof SandingCommand) return true;
        return false;
    }
    private boolean hasWheelSlipWarning() {
        for (Object o : ksession.getObjects())
            if (o instanceof Warning && ((Warning) o).getType() == com.ftn.sbnz.train.model.enums.WarningType.WHEEL_SLIP) return true;
        return false;
    }

    // ------------------------------------------------------------------
    // DRIVER CONTROLS  (called from REST controllers)
    // ------------------------------------------------------------------

    public synchronized void setThrottle(double t) {
        if (emergencyActive) return;
        train.setThrottle(Math.max(0, Math.min(1, t)));
        markInteraction(t > train.getThrottle() ? InteractionType.THROTTLE_UP : InteractionType.THROTTLE_DOWN);
    }

    public synchronized boolean armTraction() {
        if (!running) return false;
        
        if (emergencyActive) {
            appendLog("Cannot arm traction: Emergency brake active.");
            return false;
        }

        // 1) Check for any un-cleared critical breaches
        for (Object o : ksession.getObjects()) {
            if (o instanceof CriticalSafetyBreach) {
                appendLog("Cannot arm traction: Critical safety breach active.");
                return false;
            }
        }
        
        // 2) Run safeToDepart check
        org.kie.api.runtime.rule.QueryResults results = ksession.getQueryResults("safeToDepart");
        if (results.size() == 0) {
            appendLog("Cannot arm traction: safeToDepart check failed.");
            return false;
        }

        train.setTractionActive(true);
        ksession.update(trainHandle, train);
        appendLog("Traction armed successfully.");
        return true;
    }

    public synchronized void setBrake(double b) {
        train.setBrake(Math.max(0, Math.min(1, b)));
        markInteraction(b > 0 ? InteractionType.BRAKE_APPLY : InteractionType.BRAKE_RELEASE);
        if (b > 0) {
            // Releasing latched emergency brake is impossible mid-motion;
            // a driver-applied service brake however should clear an auto
            // service-brake order if it's already higher.
            // (No-op here; engine reads $t.brake in Rule 4 guard.)
        }
    }

    public synchronized void pressAlertness() {
        markInteraction(InteractionType.SIFA_RESET);
        appendLog("Driver pressed SIFA reset");
    }

    private void markInteraction(InteractionType type) {
        ksession.insert(new DriverActivity(type));
        ksession.update(trainHandle, train);
    }

    // ------------------------------------------------------------------
    // SCENARIOS / FAILURE INJECTION
    // ------------------------------------------------------------------

    private synchronized void openRandomDoor() {
        int idx = (int)(Math.random() * cars.size());
        Car c = cars.get(idx);
        c.setDoorStatus(DoorStatus.OPEN);
        ksession.update(carHandles.get(idx), c);
        appendLog("Doors randomly OPENED on " + c.getId());

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            closeDoor(idx);
        }, java.util.concurrent.CompletableFuture.delayedExecutor(5, java.util.concurrent.TimeUnit.SECONDS));
    }

    private synchronized void closeDoor(int idx) {
        if (idx < 0 || idx >= cars.size()) return;
        Car c = cars.get(idx);
        if (c.getDoorStatus() == DoorStatus.OPEN) {
            c.setDoorStatus(DoorStatus.LOCKED);
            ksession.update(carHandles.get(idx), c);
            appendLog("Doors automatically CLOSED on " + c.getId());
        }
    }

    private synchronized void blockRandomRouteNode() {
        if (routeNodes.isEmpty()) return;
        int idx = (int)(Math.random() * routeNodes.size());
        RouteNode node = routeNodes.get(idx);
        if (node.isClear()) {
            node.setClear(false);
            ksession.update(routeNodeHandles.get(idx), node);
            appendLog("RANDOM EVENT: Obstacle detected! RouteNode '" + node.getId() + "' is now BLOCKED!");
        }
    }

    private synchronized void induceWheelSlip(int eventCount, double slipMagnitude) {
        for (int i = 0; i < eventCount; i++) {
            ksession.insert(new WheelSlipEvent(slipMagnitude));
        }
        train.setWheelSpeed(train.getSpeed() + slipMagnitude);
        ksession.update(trainHandle, train);
        appendLog("Wheel slip induced (" + eventCount + " events, magnitude " + slipMagnitude + ")");
    }

    // ------------------------------------------------------------------
    // BACKWARD-CHAINING CHECKS (Rules 15 & 16)
    // ------------------------------------------------------------------

    public synchronized CheckResult checkSafeToDepart() {
        CheckResult cr = new CheckResult();
        cr.overall = !runQuery("safeToDepart").isEmpty();
        cr.subgoals.put("MAReceived",         !runQuery("maReceived").isEmpty());
        cr.subgoals.put("DoorsLocked",        !runQuery("doorsLockedAll").isEmpty());
        cr.subgoals.put("BrakeSystemReady",   !runQuery("brakeSystemReady").isEmpty());
        cr.subgoals.put("DriverReady",        !runQuery("driverReady").isEmpty());
        return cr;
    }

    public synchronized CheckResult checkSafeRouteTree(String startNodeId) {
        CheckResult cr = new CheckResult();
        RouteNode startNode = routeNodes.stream().filter(n -> n.getId().equals(startNodeId)).findFirst().orElse(null);
        if (startNode == null) {
            cr.overall = false;
            return cr;
        }
        boolean isSafe = !runQuery("isSafeRouteTree", startNode).isEmpty();
        cr.overall = isSafe;
        cr.subgoals.put("SafeRouteTree", isSafe);
        return cr;
    }

    private List<QueryResultsRow> runQuery(String name, Object... args) {
        QueryResults res = ksession.getQueryResults(name, args);
        List<QueryResultsRow> out = new ArrayList<>();
        for (QueryResultsRow r : res) out.add(r);
        return out;
    }

    // ------------------------------------------------------------------
    // STATE SNAPSHOT (used by GET /api/state)
    // ------------------------------------------------------------------

    // Static lookup tables — mirrors the .drt row data in DroolsConfig.
    private static final Map<String, double[]> PROFILE_LOOKUP = new HashMap<>();
    private static final Map<String, Double>    ADHESION_LOOKUP = new HashMap<>();
    static {
        // {brakeRate, tBrake, maxV}
        PROFILE_LOOKUP.put("Cargo",     new double[]{ 0.6, 4.5, 100 });
        PROFILE_LOOKUP.put("Passenger", new double[]{ 0.9, 2.5, 160 });
        PROFILE_LOOKUP.put("HighSpeed", new double[]{ 1.3, 1.5, 300 });
        ADHESION_LOOKUP.put("Dry",  0.35);
        ADHESION_LOOKUP.put("Wet",  0.20);
        ADHESION_LOOKUP.put("Snow", 0.10);
        ADHESION_LOOKUP.put("Ice",  0.05);
    }

    public synchronized StateSnapshot snapshot() {
        StateSnapshot s = new StateSnapshot();
        s.running = running;
        s.train = TrainStatusView.of(train);
        s.infra = InfraView.of(infra);
        s.ma = MaView.of(ma);
        s.bc = BcView.of(bc);
        s.activeProfile = profileViewFor(train.getTrainType());
        s.activeAdhesion = adhesionViewFor(infra.getWeather());
        s.cars = new ArrayList<>();
        for (Car c : cars) s.cars.add(CarView.of(c));
        s.balises = new ArrayList<>();
        for (Balise b : balises) s.balises.add(BaliseView.of(b));
        s.warnings = new ArrayList<>();
        for (Object o : ksession.getObjects()) {
            if (o instanceof Warning) s.warnings.add(WarningView.of((Warning) o));
        }
        s.emergencyActive = emergencyActive;
        s.serviceBrakeActive = serviceBrakeActive;
        s.sanding = sanding;
        s.events = new ArrayList<>(eventLog);
        return s;
    }

    private static ProfileView profileViewFor(String type) {
        double[] p = PROFILE_LOOKUP.get(type);
        if (p == null) return null;
        return new ProfileView(type, p[0], p[1], p[2]);
    }

    private static AdhesionView adhesionViewFor(String weather) {
        Double mu = ADHESION_LOOKUP.get(weather);
        if (mu == null) return null;
        return new AdhesionView(weather, mu);
    }

    private void appendLog(String msg) {
        eventLog.add(new EventLog(System.currentTimeMillis(), msg));
        while (eventLog.size() > 200) eventLog.remove(0);
    }

    // Hide the snapshot types in this file to keep the model module clean
    public static class EventLog {
        public long timestamp;
        public String message;
        public EventLog(long ts, String m) { this.timestamp = ts; this.message = m; }
    }

    public static class CheckResult {
        public boolean overall;
        public java.util.Map<String, Boolean> subgoals = new java.util.LinkedHashMap<>();
    }
}
