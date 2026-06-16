package com.ftn.sbnz.train.service.simulation;

import com.ftn.sbnz.train.model.facts.TrainStatus;

public class TrainStatusView {
    public double speed;
    public double wheelSpeed;
    public double position;
    public double brakePercentage;
    public String trainType;
    public boolean tractionActive;
    public double brakePressure;
    public boolean compressorOperational;
    public String sifaStatus;
    public double throttle;
    public double brake;

    public static TrainStatusView of(TrainStatus t) {
        TrainStatusView v = new TrainStatusView();
        v.speed = round(t.getSpeed());
        v.wheelSpeed = round(t.getWheelSpeed());
        v.position = round(t.getPosition());
        v.brakePercentage = round(t.getBrakePercentage());
        v.trainType = t.getTrainType();
        v.tractionActive = t.isTractionActive();
        v.brakePressure = t.getBrakePressure();
        v.compressorOperational = t.isCompressorOperational();
        v.sifaStatus = t.getSifaStatus() != null ? t.getSifaStatus().name() : null;
        v.throttle = t.getThrottle();
        v.brake = t.getBrake();
        return v;
    }
    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
