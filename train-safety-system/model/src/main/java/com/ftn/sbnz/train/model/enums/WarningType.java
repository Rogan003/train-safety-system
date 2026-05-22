package com.ftn.sbnz.train.model.enums;

public enum WarningType {
    OVERSPEED,
    SIFA,
    WHEEL_SLIP,
    DOORS_OPEN_WHILE_MOVING, // is this necessary or is it immediate braking?
    LOW_BRAKE_PERCENTAGE,
    GSMR_LOSS
}
