package com.ftn.sbnz.train.service.simulation;

import com.ftn.sbnz.train.model.events.Warning;

public class WarningView {
    public String type;
    public String message;
    public long timestamp;

    public static WarningView of(Warning w) {
        WarningView v = new WarningView();
        v.type = w.getType() != null ? w.getType().name() : null;
        v.message = w.getMessage();
        v.timestamp = w.getEventTime() != null ? w.getEventTime().getTime() : 0L;
        return v;
    }
}
