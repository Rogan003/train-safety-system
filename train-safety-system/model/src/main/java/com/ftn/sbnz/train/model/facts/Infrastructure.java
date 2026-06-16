package com.ftn.sbnz.train.model.facts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Infrastructure implements Serializable {
    private static final long serialVersionUID = 1L;

    private double gradient;
    private double adhesion;
    private String weather = "Dry";
    private List<Balise> balises = new ArrayList<>();
    private String currentBaliseId;

    public Infrastructure() {}

    public double getGradient() { return gradient; }
    public void setGradient(double gradient) { this.gradient = gradient; }

    public double getAdhesion() { return adhesion; }
    public void setAdhesion(double adhesion) { this.adhesion = adhesion; }

    public String getWeather() { return weather; }
    public void setWeather(String weather) { this.weather = weather; }


    public List<Balise> getBalises() { return balises; }
    public void setBalises(List<Balise> balises) { this.balises = balises; }

    public String getCurrentBaliseId() { return currentBaliseId; }
    public void setCurrentBaliseId(String currentBaliseId) { this.currentBaliseId = currentBaliseId; }
}
