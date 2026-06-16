package com.ftn.sbnz.train.service.simulation;

import com.ftn.sbnz.train.model.facts.MovementAuthority;

public class MaView {
    public double eoa;
    public double targetSpeed;
    public long timestamp;

    public static MaView of(MovementAuthority m) {
        MaView v = new MaView();
        v.eoa = m.getEoa();
        v.targetSpeed = m.getTargetSpeed();
        v.timestamp = m.getTimestamp();
        return v;
    }
}
