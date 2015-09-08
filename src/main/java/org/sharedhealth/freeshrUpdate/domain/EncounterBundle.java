package org.sharedhealth.freeshrUpdate.domain;

public class EncounterBundle {
    private String encounterId;
    private String healthId;
    private EncounterContent encounterContent;

    public EncounterBundle(String encounterId, String healthId, EncounterContent encounterContent) {
        this.encounterId = encounterId;
        this.healthId = healthId;
        this.encounterContent = encounterContent;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public String getHealthId() {
        return healthId;
    }

    public EncounterContent getEncounterContent() {
        return encounterContent;
    }
}
