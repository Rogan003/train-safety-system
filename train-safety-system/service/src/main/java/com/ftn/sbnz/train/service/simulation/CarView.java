package com.ftn.sbnz.train.service.simulation;

import com.ftn.sbnz.train.model.facts.Car;

public class CarView {
    public String id;
    public boolean locomotive;
    public double mass;
    public double brakePercentage;
    public String doorStatus;
    public String brakeTest;
    public String nextCarId;

    public static CarView of(Car c) {
        CarView v = new CarView();
        v.id = c.getId();
        v.locomotive = c.isLocomotive();
        v.mass = c.getMass();
        v.brakePercentage = c.getBrakePercentage();
        v.doorStatus = c.getDoorStatus() != null ? c.getDoorStatus().name() : null;
        v.brakeTest = c.getBrakeTest() != null ? c.getBrakeTest().name() : null;
        v.nextCarId = c.getNextCar() != null ? c.getNextCar().getId() : null;
        return v;
    }
}
