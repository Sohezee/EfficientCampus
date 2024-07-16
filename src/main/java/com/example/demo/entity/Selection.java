package com.example.demo.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class Selection {
    private String offeringName;
    private String teacherDisplay;

    public Selection(String offeringName, String teacherDisplay) {
        if (offeringName == null || teacherDisplay == null) {
            throw new IllegalArgumentException("offeringName / teacherDisplay cannot be null");
        }
        this.offeringName = offeringName;
        this.teacherDisplay = teacherDisplay;
    }

    public Selection() {

    }

    public String getOfferingName() {
        return offeringName;
    }

    public String getTeacherDisplay() {
        return teacherDisplay;
    }

    public void setOfferingName(String offeringName) {
        this.offeringName = offeringName;
    }

    public void setTeacherDisplay(String teacherDisplay) {
        this.teacherDisplay = teacherDisplay;
    }
}

