package org.sharedhealth.freeshrUpdate.mothers;

import org.sharedhealth.freeshrUpdate.domain.*;

import java.util.UUID;

public class PatientUpdateMother {

    public static PatientUpdate confidentialPatient() {
        return patientUpdate(UUID.randomUUID().toString(), UUID.randomUUID(), changeOnlyConfidential("YES"));
    }

    public static PatientUpdate notConfidentialPatient() {
        return patientUpdate(UUID.randomUUID().toString(), UUID.randomUUID(), changeOnlyConfidential("NO"));
    }

    public static PatientUpdate confidentialPatient(String healthId) {
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
        patientData.setConfidentialChange(new Change("old", confidential));
        return patientData;
    }

    public static PatientUpdate addressLineUpdated(String addressLine) {
        AddressData addressData = new AddressData();
        addressData.setAddressLine(addressLine);
        return patientUpdate(UUID.randomUUID().toString(), UUID.randomUUID(), addressChange(addressData));
    }


    public static PatientUpdate patientAddressUpdate(AddressData addressData) {
        PatientData patientData = new PatientData();
        patientData.setAddressChange(new AddressChange(new AddressData(), addressData));
        return patientUpdate(UUID.randomUUID().toString(), UUID.randomUUID(), addressChange(addressData));
    }

    public static PatientData addressChange(AddressData addressData) {
        PatientData patientData = new PatientData();
        patientData.setAddressChange(new AddressChange(new AddressData(), addressData));
        return patientData;
    }


}
