package com.ftn.sbnz.train.model.facts;

import java.io.Serializable;

import com.ftn.sbnz.train.model.enums.CouplingStatus;


public class Coupling implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private CouplingStatus status = CouplingStatus.SECURE;
    private double tension;
    private double minTension;
    private boolean isFirst;
    private Coupling nextCoupling;

    public Coupling() {}

    public Coupling(String id, CouplingStatus status, double tension, double minTension) {
        this.id = id;
        this.status = status;
        this.tension = tension;
        this.minTension = minTension;
    }

    public boolean isFirst() { return isFirst; }
    public void setFirst(boolean first) { isFirst = first; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public CouplingStatus getStatus() { return status; }
    public void setStatus(CouplingStatus status) { this.status = status; }

    public double getTension() { return tension; }
    public void setTension(double tension) { this.tension = tension; }

    public double getMinTension() { return minTension; }
    public void setMinTension(double minTension) { this.minTension = minTension; }

    public Coupling getNextCoupling() { return nextCoupling; }
    public void setNextCoupling(Coupling nextCoupling) { this.nextCoupling = nextCoupling; }
}
