package com.ftn.sbnz.train.service.simulation;

import com.ftn.sbnz.train.model.facts.BrakingCurve;

public class BcView {
    public double vWarning;
    public double vServiceBrake;
    public double vEmergencyBrake;
    public double dStop;
    public double effectiveDeceleration;
    public double distanceToEoa;

    public static BcView of(BrakingCurve b) {
        BcView v = new BcView();
        v.vWarning = round(b.getvWarning());
        v.vServiceBrake = round(b.getvServiceBrake());
        v.vEmergencyBrake = round(b.getvEmergencyBrake());
        v.dStop = round(b.getdStop());
        v.effectiveDeceleration = round(b.getEffectiveDeceleration());
        v.distanceToEoa = round(b.getDistanceToEoa());
        return v;
    }
    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
