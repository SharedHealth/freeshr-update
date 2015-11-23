package org.sharedhealth.freeshrUpdate.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AddressChange {
    @JsonProperty("old_value")
    private Address oldValue = new Address();

    @JsonProperty("new_value")
    private Address newValue = new Address();

    public AddressChange() {

    }

    public AddressChange(Address oldValue, Address newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public void setOldValue(Address oldValue) {
        this.oldValue = oldValue;
    }

    public Address getOldValue() {
        return oldValue;
    }

    public void setNewValue(Address newValue) {
        this.newValue = newValue;
    }

    public Address getNewValue() {
        return newValue;
    }

}
