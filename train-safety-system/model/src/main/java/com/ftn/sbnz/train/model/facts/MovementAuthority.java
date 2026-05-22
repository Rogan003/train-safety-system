package com.ftn.sbnz.train.model.facts;

import java.io.Serializable;


public class MovementAuthority implements Serializable {
    private static final long serialVersionUID = 1L;

    private double eoa;
    private double targetSpeed;
    private long timestamp;

    public MovementAuthority() {}

    public MovementAuthority(double eoa, double targetSpeed) {
        this.eoa = eoa;
        this.targetSpeed = targetSpeed;
        this.timestamp = System.currentTimeMillis();
    }

    public double getEoa() { return eoa; }
    public void setEoa(double eoa) { this.eoa = eoa; }

    public double getTargetSpeed() { return targetSpeed; }
    public void setTargetSpeed(double targetSpeed) { this.targetSpeed = targetSpeed; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
