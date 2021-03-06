package org.sharedhealth.freeshrUpdate.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PatientUpdate {
    @JsonProperty("year")
    private int year;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("health_id")
    private String healthId;

    @JsonProperty("change_set")
    private PatientChangeSet changeSetMap = new PatientChangeSet();

    @JsonProperty("eventTime")
    private Date eventTime;

    public int getYear() {
        return year;
    }

    public String getEventId() {
        return eventId;
    }

    public String getHealthId() {
        return healthId;
    }

    public PatientChangeSet getChangeSet() {
        return changeSetMap;
    }

    public Date getEventTime() {
        return eventTime;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setHealthId(String healthId) {
        this.healthId = healthId;
    }

    public void setChangeSetMap(PatientChangeSet changeSetMap) {
        this.changeSetMap = changeSetMap;
    }

    public void setEventTime(Date eventTime) {
        this.eventTime = eventTime;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PatientUpdate that = (PatientUpdate) o;

        if (!eventId.equals(that.eventId)) return false;
        if (!healthId.equals(that.healthId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = eventId.hashCode();
        result = 31 * result + healthId.hashCode();
        return result;
    }

    public boolean hasPatientDetailChanges() {
        return getChangeSet().hasPatientDetailChanges();
    }

    public boolean hasMergeChanges() {
        return getChangeSet().isPatientMerged();
    }

    public boolean hasConfidentialChange() {
        return getChangeSet().hasConfidentialChange();
    }

    public boolean hasAddressChange(){
        return getChangeSet().hasAddressChange();
    }

    public Map<String, Object> getPatientDetailChanges() {
        return getChangeSet().getPatientDetailChanges();
    }

    public Map<String, Object> getPatientMergeChanges() {
        return getChangeSet().getPatientMergeChanges();
    }
    }