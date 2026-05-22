package com.ftn.sbnz.train.model.facts;

import java.io.Serializable;


public class GradientSegment implements Serializable {
    private static final long serialVersionUID = 1L;

    private double startPosition;
    private double endPosition;
    private double gradient;

    public GradientSegment() {}

    public GradientSegment(double startPosition, double endPosition, double gradient) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.gradient = gradient;
    }

    public double getStartPosition() { return startPosition; }
    public void setStartPosition(double startPosition) { this.startPosition = startPosition; }

    public double getEndPosition() { return endPosition; }
    public void setEndPosition(double endPosition) { this.endPosition = endPosition; }

    public double getGradient() { return gradient; }
    public void setGradient(double gradient) { this.gradient = gradient; }

    public double getLength() { return endPosition - startPosition; }
}
