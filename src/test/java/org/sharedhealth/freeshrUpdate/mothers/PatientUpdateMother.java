package org.sharedhealth.freeshrUpdate.mothers;

import org.sharedhealth.freeshrUpdate.domain.AddressData;
import org.sharedhealth.freeshrUpdate.domain.PatientData;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;

import java.util.UUID;

public class PatientUpdateMother {

    public static PatientUpdate confidentialPatient(){
        return patientUpdate(UUID.randomUUID().toString(), UUID.randomUUID(), changeOnlyConfidential("YES"));
    }

    public static PatientUpdate confidentialPatient(String healthId){
        return patientUpdate(healthId, UUID.randomUUID(), changeOnlyConfidential("YES"));
    }
    private static PatientUpdate patientUpdate(String healthId, UUID eventId, PatientData patientData) {
        PatientUpdate patientUpdate = new PatientUpdate();
        patientUpdate.setHealthId(healthId);
        patientUpdate.setEventId(eventId);
        patientUpdate.setChangeSetMap(patientData);
        return patientUpdate;
    }

    public static PatientData changeOnlyConfidential(String confidential) {
        PatientData patientData = new PatientData();
        patientData.setConfidential(confidential);
        return patientData;
    }

    public static PatientUpdate addressLineUpdated(String addressLine) {
        AddressData addressData = new AddressData();
        addressData.setAddressLine(addressLine);
        return patientUpdate(UUID.randomUUID().toString(), UUID.randomUUID(), addressChange(addressData));
    }


    public static PatientUpdate patientAddressUpdate(AddressData addressData) {
        PatientData patientData = new PatientData();
        patientData.setAddress(addressData);
        return patientUpdate(UUID.randomUUID().toString(), UUID.randomUUID(), addressChange(addressData));
    }

    public static PatientData addressChange(AddressData addressData) {
        PatientData patientData = new PatientData();
        patientData.setAddress(addressData);
        return patientData;
    }


}
