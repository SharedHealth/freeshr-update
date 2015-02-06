package org.sharedhealth.freeshrUpdate.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PatientData {
    private String given_name;
    private String sur_name;
    private String confidential;
    private AddressData present_address;

    public String getGivenName() {
        return given_name;
    }

    public String getSurName() {
        return sur_name;
    }

    public String getConfidential() {
        return confidential;
    }

    public AddressData getPresentAddress() {
        return present_address;
    }

    public void setGiven_name(String given_name) {
        this.given_name = given_name;
    }

    public void setSur_name(String sur_name) {
        this.sur_name = sur_name;
    }

    public void setConfidential(String confidential) {
        this.confidential = confidential;
    }

    public void setPresent_address(AddressData present_address) {
        this.present_address = present_address;
    }
}
