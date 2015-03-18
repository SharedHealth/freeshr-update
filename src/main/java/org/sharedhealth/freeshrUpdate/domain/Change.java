package org.sharedhealth.freeshrUpdate.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Change {
    @JsonProperty("old_value")
    private Object oldValue;

    @JsonProperty("new_value")
    private Object newValue;

    public Change() {

    }

    public Change(Object oldValue, Object newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    public Object getNewValue() {
        return newValue;
    }
}
