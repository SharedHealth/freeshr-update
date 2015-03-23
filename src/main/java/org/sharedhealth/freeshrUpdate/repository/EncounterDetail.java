package org.sharedhealth.freeshrUpdate.repository;

import java.util.Date;

public class EncounterDetail {
    
    private String encounterId;
    private Date receivedDate;

    public EncounterDetail(String encounterId, Date receivedDate) {
        this.encounterId = encounterId;
        this.receivedDate = receivedDate;
    }

    public String getEncounterId() {
    
        return encounterId;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }
}
