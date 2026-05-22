package com.ftn.sbnz.train.model.facts;

import java.io.Serializable;

import com.ftn.sbnz.train.model.enums.BrakeTestStatus;
import com.ftn.sbnz.train.model.enums.DoorStatus;


public class Car implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private boolean locomotive;
    private double mass;
    private double brakePercentage;
    private DoorStatus doorStatus = DoorStatus.LOCKED;
    private BrakeTestStatus brakeTest = BrakeTestStatus.NOT_TESTED;
    private Car nextCar;

    public Car() {}

    public Car(String id, boolean locomotive, double mass, double brakePercentage) {
        this.id = id;
        this.locomotive = locomotive;
        this.mass = mass;
        this.brakePercentage = brakePercentage;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public boolean isLocomotive() { return locomotive; }
    public void setLocomotive(boolean locomotive) { this.locomotive = locomotive; }

    public double getMass() { return mass; }
    public void setMass(double mass) { this.mass = mass; }

    public double getBrakePercentage() { return brakePercentage; }
    public void setBrakePercentage(double brakePercentage) { this.brakePercentage = brakePercentage; }

    public DoorStatus getDoorStatus() { return doorStatus; }
    public void setDoorStatus(DoorStatus doorStatus) { this.doorStatus = doorStatus; }

    public BrakeTestStatus getBrakeTest() { return brakeTest; }
    public void setBrakeTest(BrakeTestStatus brakeTest) { this.brakeTest = brakeTest; }

    public Car getNextCar() { return nextCar; }
    public void setNextCar(Car nextCar) { this.nextCar = nextCar; }
}
