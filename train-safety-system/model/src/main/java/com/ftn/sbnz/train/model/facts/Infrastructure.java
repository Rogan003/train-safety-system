package com.ftn.sbnz.train.model.facts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Infrastructure implements Serializable {
    private static final long serialVersionUID = 1L;

    private double gradient;
    private double adhesion;
    private boolean gsmrActive = true;
    private boolean rbcHandshake = true;
    private List<Balise> balises = new ArrayList<>();
    private String currentBaliseId;

    public Infrastructure() {}

    public double getGradient() { return gradient; }
    public void setGradient(double gradient) { this.gradient = gradient; }

    public double getAdhesion() { return adhesion; }
    public void setAdhesion(double adhesion) { this.adhesion = adhesion; }

    public boolean isGsmrActive() { return gsmrActive; }
    public void setGsmrActive(boolean gsmrActive) { this.gsmrActive = gsmrActive; }

    public boolean isRbcHandshake() { return rbcHandshake; }
    public void setRbcHandshake(boolean rbcHandshake) { this.rbcHandshake = rbcHandshake; }

    public List<Balise> getBalises() { return balises; }
    public void setBalises(List<Balise> balises) { this.balises = balises; }

    public String getCurrentBaliseId() { return currentBaliseId; }
    public void setCurrentBaliseId(String currentBaliseId) { this.currentBaliseId = currentBaliseId; }
}
