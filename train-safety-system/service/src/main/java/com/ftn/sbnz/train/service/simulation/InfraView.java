package com.ftn.sbnz.train.service.simulation;

import com.ftn.sbnz.train.model.facts.Infrastructure;

public class InfraView {
    public double gradient;
    public double adhesion;
    public String weather;
    public String currentBaliseId;

    public static InfraView of(Infrastructure i) {
        InfraView v = new InfraView();
        v.gradient = i.getGradient();
        v.adhesion = Math.round(i.getAdhesion() * 1000.0) / 1000.0;
        v.weather = i.getWeather();
        v.currentBaliseId = i.getCurrentBaliseId();
        return v;
    }
}
