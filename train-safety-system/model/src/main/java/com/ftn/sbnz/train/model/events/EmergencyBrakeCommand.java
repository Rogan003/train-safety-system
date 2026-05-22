package com.ftn.sbnz.train.model.events;

import java.io.Serializable;
import java.util.Date;

import org.kie.api.definition.type.Expires;
import org.kie.api.definition.type.Role;
import org.kie.api.definition.type.Timestamp;

@Role(Role.Type.EVENT)
@Timestamp("eventTime")
@Expires("1m")
public class EmergencyBrakeCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    private String reason;
    private Date eventTime;

    public EmergencyBrakeCommand() {
        this.eventTime = new Date();
    }

    public EmergencyBrakeCommand(String reason) {
        this.reason = reason;
        this.eventTime = new Date();
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Date getEventTime() { return eventTime; }
    public void setEventTime(Date eventTime) { this.eventTime = eventTime; }
}
