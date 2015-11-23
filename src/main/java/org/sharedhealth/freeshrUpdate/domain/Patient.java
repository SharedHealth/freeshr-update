package org.sharedhealth.freeshrUpdate.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Patient {

    @JsonProperty("hid")
    private String healthId;

    @JsonProperty("present_address")
    private Address address;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("merged_with")
    private String mergedWith;

    private Confidentiality confidentiality;

    public String getHealthId() {
        return healthId;
    }

    public void setHealthId(String healthId) {
        this.healthId = healthId;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getGender() {
        return gender;
    }

    public String getMergedWith() {
        return mergedWith;
    }


    public void setGender(String gender) {
        this.gender = gender;
    }

    public Confidentiality getConfidentiality() {
        return confidentiality;
    }

    @JsonProperty("confidential")
    public void setConfidentiality(String confidentiality) {
        this.confidentiality = "YES".equalsIgnoreCase(confidentiality) ?
                Confidentiality.VeryRestricted : Confidentiality.Normal;
    }

    public void setMergedWith(String mergedWith) {
        this.mergedWith = mergedWith;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient)) return false;

        Patient patient = (Patient) o;

        if (active != null ? !active.equals(patient.active) : patient.active != null) return false;
        if (address != null ? !address.equals(patient.address) : patient.address != null) return false;
        if (confidentiality != patient.confidentiality) return false;
        if (gender != null ? !gender.equals(patient.gender) : patient.gender != null) return false;
        if (!healthId.equals(patient.healthId)) return false;
        if (mergedWith != null ? !mergedWith.equals(patient.mergedWith) : patient.mergedWith != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return healthId.hashCode();
    }

    @Override
    public String toString() {
        return "Patient{" +
                "healthId='" + healthId + '\'' +
                ", address=" + address +
                ", gender='" + gender + '\'' +
                ", active=" + active +
                ", mergedWith='" + mergedWith + '\'' +
                ", confidentiality=" + confidentiality +
                '}';
    }
}
