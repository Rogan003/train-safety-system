package com.ftn.sbnz.train.service.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ftn.sbnz.train.service.simulation.SimulationEngine;
import com.ftn.sbnz.train.service.simulation.StateSnapshot;

/**
 * Single REST controller exposing everything the DMI/web frontend needs.
 * All mutating endpoints are POST so they're explicit; GET /api/state is
 * the only read endpoint and is polled at 5 Hz by the frontend.
 */
@RestController
@RequestMapping("/api")
public class TrainController {

    private final SimulationEngine engine;

    @Autowired
    public TrainController(SimulationEngine engine) {
        this.engine = engine;
    }

    // -------------------------------------------------------------- STATE
    @GetMapping("/state")
    public StateSnapshot state() {
        return engine.snapshot();
    }

    // ------------------------------------------------------- LIFECYCLE
    @PostMapping("/sim/start") public Map<String,Object> start() { engine.start(); return ok(); }
    @PostMapping("/sim/stop")  public Map<String,Object> stop()  { engine.stop();  return ok(); }
    @PostMapping("/sim/reset") public Map<String,Object> reset() { engine.reset(); return ok(); }

    // ------------------------------------------------------- DRIVER ACTIONS
    @PostMapping("/driver/throttle")
    public Map<String,Object> throttle(@RequestParam double value) {
        engine.setThrottle(value);
        return ok();
    }

    @PostMapping("/driver/brake")
    public Map<String,Object> brake(@RequestParam double value) {
        engine.setBrake(value);
        return ok();
    }

    @PostMapping("/driver/arm-traction")
    public Map<String,Object> armTraction() {
        boolean success = engine.armTraction();
        return Map.of("ok", success);
    }

    @PostMapping("/driver/alertness")
    public Map<String,Object> alertness() {
        engine.pressAlertness();
        return ok();
    }

    // -------------------------------------------------- BACKWARD-CHAIN CHECKS
    @GetMapping("/checks/safe-to-depart")
    public SimulationEngine.CheckResult safeToDepart() { return engine.checkSafeToDepart(); }

    @GetMapping("/checks/safe-route-tree")
    public SimulationEngine.CheckResult checkSafeRouteTree(@RequestParam String startNodeId) {
        return engine.checkSafeRouteTree(startNodeId);
    }

    private static Map<String,Object> ok() {
        Map<String,Object> r = new HashMap<>();
        r.put("ok", true);
        return r;
    }
}
