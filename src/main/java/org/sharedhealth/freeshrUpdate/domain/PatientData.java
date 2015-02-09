package org.sharedhealth.freeshrUpdate.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PatientData {
    @JsonProperty("given_name")
    private String givenName;
    @JsonProperty("sur_name")
    private String surName;
    @JsonProperty("confidential")
    private String confidential;
    @JsonProperty("present_address")
    private AddressData address = new AddressData();

    public String getGivenName() {
        return givenName;
    }

    public String getSurName() {
        return surName;
    }

    public String getConfidential() {
        return confidential;
    }

    public AddressData getAddress() {
        return address;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }

    public void setConfidential(String confidential) {
        this.confidential = confidential;
    }

    public void setAddress(AddressData present_address) {
        this.address = present_address;
    }

    public boolean hasChanges() {
        HashMap<String, String> changes = new HashMap<>();
        changes.put("confidential", confidential);

        changes.put("address_line", address.getAddressLine());
        changes.put("division_id", address.getDivisionId());
        changes.put("district_id", address.getDistrictId());
        changes.put("upazila_id", address.getUpazilaId());
        changes.put("city_corporation_id", address.getCityCorporationId());
        changes.put("union_urban_ward_id", address.getUnionOrUrbanWardId());

        for (String change : changes.values()) {
            if(StringUtils.isNotBlank(change)) return true;
        }
        return false;
    }
}
