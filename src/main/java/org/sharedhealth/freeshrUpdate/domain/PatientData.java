package org.sharedhealth.freeshrUpdate.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import org.sharedhealth.freeshrUpdate.utils.Confidentiality;

import java.util.HashMap;
import java.util.Map;

import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.*;

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

    public Map<String, String> getChanges() {
        HashMap<String, String> changes = new HashMap<>();
        if (null != getConfidentialChange())
            changes.put(CONFIDENTIALITY_COLUMN_NAME, getConfidential());
        changes.put(GENDER_COLUMN_NAME, getGenderChange());

        changes.put(ADDRESS_LINE_COLUMN_NAME, getAddressChange().getAddressLine());
        changes.put(DIVISION_ID_COLUMN_NAME, getAddressChange().getDivisionId());
        changes.put(DISTRICT_ID_COLUMN_NAME, getAddressChange().getDistrictId());
        changes.put(UPAZILA_ID_COLUMN_NAME, getAddressChange().getUpazilaId());
        changes.put(CITY_CORPORATION_ID_COLUMN_NAME, getAddressChange().getCityCorporationId());
        changes.put(UNION_OR_URBAN_COLUMN_NAME, getAddressChange().getUnionOrUrbanWardId());

        return Maps.filterValues(changes, new Predicate<Object>() {
            @Override
            public boolean apply(Object input) {
                return null != input;
            }
        });
    }

    private String getConfidential() {
        return "YES".equalsIgnoreCase(getConfidentialChange()) ? Confidentiality.VeryRestricted.getLevel() :
                Confidentiality.Normal.getLevel();
    }

    public boolean hasConfidentialChange() {
        return getChanges().containsKey(CONFIDENTIALITY_COLUMN_NAME);
    }
}
