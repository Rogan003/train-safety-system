package com.ftn.sbnz.train.model.facts;

import java.io.Serializable;

public class CriticalSafetyBreach implements Serializable {
    private static final long serialVersionUID = 1L;

    private String reason;
    private long timestamp;

    public CriticalSafetyBreach() {}

    public CriticalSafetyBreach(String reason) {
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
