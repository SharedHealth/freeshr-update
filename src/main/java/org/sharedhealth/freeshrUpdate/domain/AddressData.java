package org.sharedhealth.freeshrUpdate.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressData {
    @JsonProperty("address_line")
    private String addressLine;
    @JsonProperty("division_id")
    private String divisionId;
    @JsonProperty("district_id")
    private String districtId;
    @JsonProperty("upazila_id")
    private String upazilaId;
    @JsonProperty("city_corporation_id")
    private String cityCorporationId;
    @JsonProperty("union_or_urban_ward_id")
    private String unionOrUrbanWardId;

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getDivisionId() {
        return divisionId;
    }

    public void setDivisionId(String divisionId) {
        this.divisionId = divisionId;
    }

    public String getDistrictId() {
        return districtId;
    }

    public void setDistrictId(String districtId) {
        this.districtId = districtId;
    }

    public String getUpazilaId() {
        return upazilaId;
    }

    public void setUpazilaId(String upazilaId) {
        this.upazilaId = upazilaId;
    }

    public String getCityCorporationId() {
        return cityCorporationId;
    }

    public void setCityCorporationId(String cityCorporationId) {
        this.cityCorporationId = cityCorporationId;
    }

    public String getUnionOrUrbanWardId() {
        return unionOrUrbanWardId;
    }

    public void setUnionOrUrbanWardId(String unionOrUrbanWardId) {
        this.unionOrUrbanWardId = unionOrUrbanWardId;
    }

    public String getConcatenatedDistrictId() {
        return divisionId + districtId;
    }

    public String getConcatenatedUpazilaId() {
        return divisionId + districtId + upazilaId;
    }

    public String getConcatenatedCityCorporationId() {
        return cityCorporationId != null ? divisionId + districtId + upazilaId + cityCorporationId : null;
    }

    public String getConcatenatedWardId() {
        return unionOrUrbanWardId != null ? divisionId + districtId + upazilaId + cityCorporationId + unionOrUrbanWardId : null;
    }

    public boolean isEmpty(){
        List collection = new ArrayList() {{add(divisionId);add(districtId);add(upazilaId);add(cityCorporationId);add(unionOrUrbanWardId);add(addressLine);}};
        collection.removeAll(Collections.singleton(null));
        return collection.size() == 0;
    }

}
