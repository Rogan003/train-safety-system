package com.ftn.sbnz.train.service.simulation;

import com.ftn.sbnz.train.model.facts.Balise;

public class BaliseView {
    public String id;
    public double position;
    public double newEoa;
    public double newTargetSpeed;
    public boolean consumed;

    public static BaliseView of(Balise b) {
        BaliseView v = new BaliseView();
        v.id = b.getId();
        v.position = b.getPosition();
        v.newEoa = b.getNewEoa();
        v.newTargetSpeed = b.getNewTargetSpeed();
        v.consumed = b.isConsumed();
        return v;
    }
}
