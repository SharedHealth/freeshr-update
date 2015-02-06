package org.sharedhealth.freeshrUpdate.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect
public class PatientUpdate {
    private int year;
    private UUID eventId;
    private String healthId;
    private PatientData changeSetMap;
    private Date eventTime;

    public int getYear() {
        return year;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getHealthId() {
        return healthId;
    }

    public PatientData getChangeSet() {
        return changeSetMap;
    }

    public Date getEventTime() {
        return eventTime;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public void setHealthId(String healthId) {
        this.healthId = healthId;
    }

    public void setChangeSetMap(PatientData changeSetMap) {
        this.changeSetMap = changeSetMap;
    }

    public void setEventTime(Date eventTime) {
        this.eventTime = eventTime;
    }
}
