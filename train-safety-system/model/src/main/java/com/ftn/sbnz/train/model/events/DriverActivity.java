package com.ftn.sbnz.train.model.events;

import java.io.Serializable;
import java.util.Date;

import org.kie.api.definition.type.Expires;
import org.kie.api.definition.type.Role;
import org.kie.api.definition.type.Timestamp;

import com.ftn.sbnz.train.model.enums.InteractionType;

@Role(Role.Type.EVENT)
@Timestamp("eventTime")
@Expires("5m")
public class DriverActivity implements Serializable {
    private static final long serialVersionUID = 1L;

    private InteractionType type;
    private Date eventTime;

    public DriverActivity() {
        this.eventTime = new Date();
    }

    public DriverActivity(InteractionType type) {
        this.type = type;
        this.eventTime = new Date();
    }

    public InteractionType getType() { return type; }
    public void setType(InteractionType type) { this.type = type; }

    public Date getEventTime() { return eventTime; }
    public void setEventTime(Date eventTime) { this.eventTime = eventTime; }
}
