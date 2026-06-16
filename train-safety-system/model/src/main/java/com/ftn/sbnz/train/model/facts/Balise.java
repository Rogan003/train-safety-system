package com.ftn.sbnz.train.model.facts;

import java.io.Serializable;


public class Balise implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private double position;
    private double newEoa;
    private double newTargetSpeed;
    private boolean consumed = false;
    private RouteNode linkedNode;

    public Balise() {}

    public Balise(String id, double position, double newEoa, double newTargetSpeed) {
        this.id = id;
        this.position = position;
        this.newEoa = newEoa;
        this.newTargetSpeed = newTargetSpeed;
        this.linkedNode = null;
    }
    
    public Balise(String id, double position, double newEoa, double newTargetSpeed, RouteNode linkedNode) {
        this.id = id;
        this.position = position;
        this.newEoa = newEoa;
        this.newTargetSpeed = newTargetSpeed;
        this.linkedNode = linkedNode;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getPosition() { return position; }
    public void setPosition(double position) { this.position = position; }

    public double getNewEoa() { return newEoa; }
    public void setNewEoa(double newEoa) { this.newEoa = newEoa; }

    public double getNewTargetSpeed() { return newTargetSpeed; }
    public void setNewTargetSpeed(double newTargetSpeed) { this.newTargetSpeed = newTargetSpeed; }

    public boolean isConsumed() { return consumed; }
    public void setConsumed(boolean consumed) { this.consumed = consumed; }

    public RouteNode getLinkedNode() { return linkedNode; }
    public void setLinkedNode(RouteNode linkedNode) { this.linkedNode = linkedNode; }
}
