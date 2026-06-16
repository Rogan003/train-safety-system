package com.ftn.sbnz.train.service.simulation;

import java.util.List;

/**
 * Plain JSON-serialisable bundle returned by GET /api/state.
 * Built from the live fact graph; intentionally not reactive — pulled
 * once per render frame by the frontend.
 */
public class StateSnapshot {
    public boolean running;
    public boolean emergencyActive;
    public boolean serviceBrakeActive;
    public boolean sanding;

    public TrainStatusView train;
    public InfraView infra;
    public MaView ma;
    public BcView bc;
    public ProfileView activeProfile;
    public AdhesionView activeAdhesion;

    public List<CarView> cars;
    public List<BaliseView> balises;
    public List<WarningView> warnings;
    public List<SimulationEngine.EventLog> events;
}
