package org.sharedhealth.freeshrUpdate.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AddressChange {
    @JsonProperty("old_value")
    private AddressData oldValue = new AddressData();

    @JsonProperty("new_value")
    private AddressData newValue = new AddressData();

    public AddressChange() {

    }

    public AddressChange(AddressData oldValue, AddressData newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public void setOldValue(AddressData oldValue) {
        this.oldValue = oldValue;
    }

    public AddressData getOldValue() {
        return oldValue;
    }

    public void setNewValue(AddressData newValue) {
        this.newValue = newValue;
    }

    public AddressData getNewValue() {
        return newValue;
    }

}
