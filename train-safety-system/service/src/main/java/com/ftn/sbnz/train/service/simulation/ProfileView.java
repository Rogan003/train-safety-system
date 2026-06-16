package com.ftn.sbnz.train.service.simulation;

public class ProfileView {
    public String type;
    public double brakeRate;
    public double tBrake;
    public double maxV;
    public ProfileView() {}
    public ProfileView(String type, double brakeRate, double tBrake, double maxV) {
        this.type = type; this.brakeRate = brakeRate; this.tBrake = tBrake; this.maxV = maxV;
    }
}
