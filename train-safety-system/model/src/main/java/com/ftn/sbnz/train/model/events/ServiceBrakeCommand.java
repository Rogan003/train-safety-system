package com.ftn.sbnz.train.model.events;

import java.io.Serializable;
import java.util.Date;

import org.kie.api.definition.type.Expires;
import org.kie.api.definition.type.Role;
import org.kie.api.definition.type.Timestamp;

@Role(Role.Type.EVENT)
@Timestamp("eventTime")
@Expires("10s")
public class ServiceBrakeCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    private double intensity;
    private String reason;
    private Date eventTime;

    public ServiceBrakeCommand() {
        this.eventTime = new Date();
    }

    public ServiceBrakeCommand(double intensity, String reason) {
        this.intensity = intensity;
        this.reason = reason;
        this.eventTime = new Date();
    }

    public double getIntensity() { return intensity; }
    public void setIntensity(double intensity) { this.intensity = intensity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Date getEventTime() { return eventTime; }
    public void setEventTime(Date eventTime) { this.eventTime = eventTime; }
}
