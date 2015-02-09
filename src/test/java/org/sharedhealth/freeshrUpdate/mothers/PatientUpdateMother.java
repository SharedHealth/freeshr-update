package org.sharedhealth.freeshrUpdate.mothers;

import org.sharedhealth.freeshrUpdate.domain.PatientData;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;

import java.util.UUID;

public class PatientUpdateMother {

    public static PatientUpdate confidentialPatient(){
        return patientUpdate(UUID.randomUUID().toString(), UUID.randomUUID(), changeSet("YES"));
    }
    private static PatientUpdate patientUpdate(String healthId, UUID eventId, PatientData patientData) {
        PatientUpdate patientUpdate = new PatientUpdate();
        patientUpdate.setHealthId(healthId);
        patientUpdate.setEventId(eventId);
        patientUpdate.setChangeSetMap(patientData);
        return patientUpdate;
    }

    private static PatientData changeSet(String confidential) {
        PatientData patientData = new PatientData();
        patientData.setConfidential(confidential);
        return patientData;
    }
}
