package org.sharedhealth.freeshrUpdate.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PatientData {
    @JsonProperty("given_name")
    private Change givenNameChange = new Change();

    @JsonProperty("sur_name")
    private Change surNameChange = new Change();

    @JsonProperty("confidential")
    private Change confidentialChange = new Change();

    @JsonProperty("gender")
    private Change genderChange = new Change();

    @JsonProperty("present_address")
    private AddressChange addressChange = new AddressChange();

    public AddressData getAddressChange() {
        return addressChange.getNewValue();
    }

    public void setGivenNameChange(Change givenNameChange) {
        this.givenNameChange = givenNameChange;
    }

    public void setSurNameChange(Change surNameChange) {
        this.surNameChange = surNameChange;
    }

    public void setConfidentialChange(Change confidentialChange) {
        this.confidentialChange = confidentialChange;
    }

    public void setAddressChange(AddressChange addressChange) {
        this.addressChange = addressChange;
    }

    public void setGenderChange(Change genderChange) {
        this.genderChange = genderChange;
    }

    public String getGivenNameChange() {
        return (String) givenNameChange.getNewValue();
    }

    public String getSurNameChange() {
        return (String) surNameChange.getNewValue();
    }

    public String getConfidentialChange() {
        return (String) confidentialChange.getNewValue();
    }

    public String getGenderChange() {
        return (String) genderChange.getNewValue();
    }

    public boolean hasChanges() {
        return getChanges().size() > 0;
    }

    public Map<String, Object> getChanges() {
        HashMap<String, Object> changes = new HashMap<>();
        if (null != getConfidentialChange())
            changes.put("confidential", "YES".equalsIgnoreCase(getConfidentialChange()));
        changes.put("gender", getGenderChange());

        changes.put("address_line", getAddressChange().getAddressLine());
        changes.put("division_id", getAddressChange().getDivisionId());
        changes.put("district_id", getAddressChange().getDistrictId());
        changes.put("upazila_id", getAddressChange().getUpazilaId());
        changes.put("city_corporation_id", getAddressChange().getCityCorporationId());
        changes.put("union_urban_ward_id", getAddressChange().getUnionOrUrbanWardId());

        return Maps.filterValues(changes, new Predicate<Object>() {
            @Override
            public boolean apply(Object input) {
                return null != input;
            }
        });
    }

}
