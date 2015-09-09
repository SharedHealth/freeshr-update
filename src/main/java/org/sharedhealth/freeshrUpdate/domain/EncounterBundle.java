package org.sharedhealth.freeshrUpdate.domain;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

public class EncounterBundle {
    private String encounterId;
    private String healthId;
    private String encounterContent;
    private Date receivedAt;

    public EncounterBundle(String encounterId, String healthId, String encounterContent, Date receivedAt) {
        this.encounterId = encounterId;
        this.healthId = healthId;
        this.encounterContent = encounterContent;
        this.receivedAt = receivedAt;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public String getHealthId() {
        return healthId;
    }

    public String getEncounterContent() {
        return encounterContent;
    }

    public Date getReceivedAt() {
        return receivedAt;
    }

    public void associateTo(String healthId) {
        String inactiveHealthId = this.healthId;
        this.encounterContent = StringUtils.replace(encounterContent, inactiveHealthId, healthId);
        this.healthId = healthId;
    }
}
