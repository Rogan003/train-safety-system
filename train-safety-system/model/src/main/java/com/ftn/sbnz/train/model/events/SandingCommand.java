package com.ftn.sbnz.train.model.events;

import java.io.Serializable;
import java.util.Date;

import org.kie.api.definition.type.Expires;
import org.kie.api.definition.type.Role;
import org.kie.api.definition.type.Timestamp;


@Role(Role.Type.EVENT)
@Timestamp("eventTime")
@Expires("30s")
public class SandingCommand implements Serializable { // is this necessary to exist? or is it enough to do the changes?
    private static final long serialVersionUID = 1L;

    private Date eventTime;

    public SandingCommand() {
        this.eventTime = new Date();
    }

    public Date getEventTime() { return eventTime; }
    public void setEventTime(Date eventTime) { this.eventTime = eventTime; }
}
