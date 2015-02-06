package org.sharedhealth.freeshrUpdate.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressData {
    private String address_line;
    private String division_id;
    private String district_id;
    private String upazila_id;
    private String city_corporation_id;
    private String union_or_urban_ward_id;
    private String rural_ward_id;
    private String holding_number;
    private String street;
    private String area_mouja;
    private String village;
    private String post_office;
    private String post_code;
    private String country_code;

    public String getAddressLine() {
        return address_line;
    }

    public String getDivisionId() {
        return division_id;
    }

    public String getDistrictId() {
        return district_id;
    }

    public String getUpazilaId() {
        return upazila_id;
    }

    public String getUnionOrUrbanWardId() {
        return union_or_urban_ward_id;
    }

    public String getHoldingNumber() {
        return holding_number;
    }

    public String getStreet() {
        return street;
    }

    public String getAreaMouja() {
        return area_mouja;
    }

    public String getVillage() {
        return village;
    }

    public String getPostOffice() {
        return post_office;
    }

    public String getCountryCode() {
        return country_code;
    }

    public String getCityCorporationId() {
        return city_corporation_id;
    }

    public String getRuralWardId() {
        return rural_ward_id;
    }

    public String getPostCode() {
        return post_code;
    }


    public void setAddress_line(String address_line) {
        this.address_line = address_line;
    }

    public void setDivision_id(String division_id) {
        this.division_id = division_id;
    }

    public void setDistrict_id(String district_id) {
        this.district_id = district_id;
    }

    public void setUpazila_id(String upazila_id) {
        this.upazila_id = upazila_id;
    }

    public void setCity_corporation_id(String city_corporation_id) {
        this.city_corporation_id = city_corporation_id;
    }

    public void setUnion_or_urban_ward_id(String union_or_urban_ward_id) {
        this.union_or_urban_ward_id = union_or_urban_ward_id;
    }

    public void setRural_ward_id(String rural_ward_id) {
        this.rural_ward_id = rural_ward_id;
    }

    public void setHolding_number(String holding_number) {
        this.holding_number = holding_number;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public void setArea_mouja(String area_mouja) {
        this.area_mouja = area_mouja;
    }

    public void setVillage(String village) {
        this.village = village;
    }

    public void setPost_office(String post_office) {
        this.post_office = post_office;
    }

    public void setPost_code(String post_code) {
        this.post_code = post_code;
    }

    public void setCountry_code(String country_code) {
        this.country_code = country_code;
    }
}
