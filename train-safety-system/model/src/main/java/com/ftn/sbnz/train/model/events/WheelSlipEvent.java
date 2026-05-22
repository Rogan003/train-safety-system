package com.ftn.sbnz.train.model.events;

import java.io.Serializable;
import java.util.Date;

import org.kie.api.definition.type.Expires;
import org.kie.api.definition.type.Role;
import org.kie.api.definition.type.Timestamp;

@Role(Role.Type.EVENT)
@Timestamp("eventTime")
@Expires("30s")
public class WheelSlipEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private double slipAmount;
    private Date eventTime;

    public WheelSlipEvent() {
        this.eventTime = new Date();
    }

    public WheelSlipEvent(double slipAmount) {
        this.slipAmount = slipAmount;
        this.eventTime = new Date();
    }

    public double getSlipAmount() { return slipAmount; }
    public void setSlipAmount(double slipAmount) { this.slipAmount = slipAmount; }

    public Date getEventTime() { return eventTime; }
    public void setEventTime(Date eventTime) { this.eventTime = eventTime; }
}
