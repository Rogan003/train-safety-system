package com.ftn.sbnz.train.model.facts;

import java.io.Serializable;

public class BrakingCurve implements Serializable { // GOOD
    private static final long serialVersionUID = 1L;

    private double vWarning;
    private double vServiceBrake;
    private double vEmergencyBrake;
    private double dStop;
    private double effectiveDeceleration;
    private double distanceToEoa;

    public BrakingCurve() {}

    public double getvWarning() { return vWarning; }
    public void setvWarning(double vWarning) { this.vWarning = vWarning; }

    public double getvServiceBrake() { return vServiceBrake; }
    public void setvServiceBrake(double vServiceBrake) { this.vServiceBrake = vServiceBrake; }

    public double getvEmergencyBrake() { return vEmergencyBrake; }
    public void setvEmergencyBrake(double vEmergencyBrake) { this.vEmergencyBrake = vEmergencyBrake; }

    public double getdStop() { return dStop; }
    public void setdStop(double dStop) { this.dStop = dStop; }

    public double getEffectiveDeceleration() { return effectiveDeceleration; }
    public void setEffectiveDeceleration(double effectiveDeceleration) { this.effectiveDeceleration = effectiveDeceleration; }

    public double getDistanceToEoa() { return distanceToEoa; }
    public void setDistanceToEoa(double distanceToEoa) { this.distanceToEoa = distanceToEoa; }
}
