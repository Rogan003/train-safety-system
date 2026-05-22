package com.ftn.sbnz.train.model.events;

import java.io.Serializable;
import java.util.Date;

import org.kie.api.definition.type.Expires;
import org.kie.api.definition.type.Role;
import org.kie.api.definition.type.Timestamp;

import com.ftn.sbnz.train.model.enums.WarningType;

@Role(Role.Type.EVENT)
@Timestamp("eventTime")
@Expires("5m")
public class Warning implements Serializable {
    private static final long serialVersionUID = 1L;

    private WarningType type;
    private String message;
    private Date eventTime;

    public Warning() {
        this.eventTime = new Date();
    }

    public Warning(WarningType type, String message) {
        this.type = type;
        this.message = message;
        this.eventTime = new Date();
    }

    public WarningType getType() { return type; }
    public void setType(WarningType type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getEventTime() { return eventTime; }
    public void setEventTime(Date eventTime) { this.eventTime = eventTime; }
}
