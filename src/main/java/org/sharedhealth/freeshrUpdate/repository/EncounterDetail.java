package org.sharedhealth.freeshrUpdate.repository;

import java.util.UUID;

public class EncounterDetail {

    private String encounterId;
    private UUID receivedAtUuid;

    public EncounterDetail(String encounterId, UUID receivedAtUuid) {
        this.encounterId = encounterId;
        this.receivedAtUuid = receivedAtUuid;
    }

    public String getEncounterId() {

        return encounterId;
    }

    public UUID getReceivedAtUuid() {
        return receivedAtUuid;
    }
}
