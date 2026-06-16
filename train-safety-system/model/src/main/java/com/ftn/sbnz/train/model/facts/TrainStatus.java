package com.ftn.sbnz.train.model.facts;

import java.io.Serializable;

import com.ftn.sbnz.train.model.enums.SifaStatus;

public class TrainStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    private double speed;
    private double wheelSpeed;
    private double position;
    private double brakePercentage;
    private boolean tractionActive = true;
    private double brakePressure = 5.0;
    private boolean compressorOperational = true;

    private String trainType = "Passenger";

    private SifaStatus sifaStatus = SifaStatus.OPERATIONAL;

    private double throttle = 0.0;
    private double brake = 0.0;

    public TrainStatus() {}

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    public double getWheelSpeed() { return wheelSpeed; }
    public void setWheelSpeed(double wheelSpeed) { this.wheelSpeed = wheelSpeed; }

    public double getPosition() { return position; }
    public void setPosition(double position) { this.position = position; }

    public double getBrakePercentage() { return brakePercentage; }
    public void setBrakePercentage(double brakePercentage) { this.brakePercentage = brakePercentage; }

    public String getTrainType() { return trainType; }
    public void setTrainType(String trainType) { this.trainType = trainType; }

    public boolean isTractionActive() { return tractionActive; }
    public void setTractionActive(boolean tractionActive) { this.tractionActive = tractionActive; }

    public double getBrakePressure() { return brakePressure; }
    public void setBrakePressure(double brakePressure) { this.brakePressure = brakePressure; }

    public boolean isCompressorOperational() { return compressorOperational; }
    public void setCompressorOperational(boolean compressorOperational) { this.compressorOperational = compressorOperational; }

    public SifaStatus getSifaStatus() { return sifaStatus; }
    public void setSifaStatus(SifaStatus sifaStatus) { this.sifaStatus = sifaStatus; }


    public double getThrottle() { return throttle; }
    public void setThrottle(double throttle) { this.throttle = throttle; }

    public double getBrake() { return brake; }
    public void setBrake(double brake) { this.brake = brake; }

}
