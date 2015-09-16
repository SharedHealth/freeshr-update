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

    @JsonProperty("confidential")
    private Change confidentialChange = new Change();

    @JsonProperty("gender")
    private Change genderChange = new Change();

    @JsonProperty("active")
    private Change active = new Change();

    @JsonProperty("merged_with")
    private Change mergedWith = new Change();

    @JsonProperty("present_address")
    private AddressChange addressChange = new AddressChange();

    public AddressData getAddressChange() {
        return addressChange.getNewValue();
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

    public String getConfidentialChange() {
        return (String) confidentialChange.getNewValue();
    }

    public String getGenderChange() {
        return (String) genderChange.getNewValue();
    }

    public Boolean getActive() {
        return (Boolean)active.getNewValue();
    }

    public void setActive(Change active) {
        this.active = active;
    }

    public String getMergedWith() {
        return (String)mergedWith.getNewValue();
    }

    public void setMergedWith(Change mergedWith) {
        this.mergedWith = mergedWith;
    }

    public boolean hasPatientDetailChanges() {
        return getPatientDetailChanges().size() > 0;
    }
    
    public boolean isPatientMerged(){
        return getPatientMergeChanges().size() > 0 && !getActive() && getMergedWith()!=null;
    }

    public Map<String, Object> getPatientMergeChanges() {
        HashMap<String, Object> changes = new HashMap<>();
        changes.put(ACTIVE_COLUMN_NAME, getActive());
        changes.put(MERGED_WITH_COLUMN_NAME, getMergedWith());

        return Maps.filterValues(changes, new Predicate<Object>() {
            @Override
            public boolean apply(Object input) {
                return null != input;
            }
        });
    }

    public Map<String, Object> getPatientDetailChanges() {
        HashMap<String, Object> changes = new HashMap<>();
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
        return getPatientDetailChanges().containsKey(CONFIDENTIALITY_COLUMN_NAME);
    }

    public boolean hasAddressChange(){
        return !getAddressChange().isEmpty();
    }
}
