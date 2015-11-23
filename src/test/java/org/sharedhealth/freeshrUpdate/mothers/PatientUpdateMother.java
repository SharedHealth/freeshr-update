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

    public static PatientUpdate addressLineUpdated(String addressLine) {
        Address address = new Address();
        address.setAddressLine(addressLine);
        return patientUpdate(UUID.randomUUID().toString(), UUID.randomUUID(), addressChange(address));
    }

    public static PatientUpdate merge(String healthId, String withHealthId){
        return patientUpdate(healthId, UUID.randomUUID(), mergeChange(withHealthId));
    }

    public static PatientUpdate patientAddressUpdate(Address address) {
        return patientUpdate(UUID.randomUUID().toString(), UUID.randomUUID(), addressChange(address));
    }

    public static PatientChangeSet changeOnlyConfidential(String confidential) {
        PatientChangeSet patientChangeSet = new PatientChangeSet();
        patientChangeSet.setConfidentialChange(new Change("old", confidential));
        return patientChangeSet;
    }

    public static PatientChangeSet addressChange(Address address) {
        PatientChangeSet patientChangeSet = new PatientChangeSet();
        patientChangeSet.setAddressChange(new AddressChange(new Address(), address));
        return patientChangeSet;
    }

    public static PatientChangeSet mergeChange(String mergedWith){
        PatientChangeSet patientChangeSet = new PatientChangeSet();
        patientChangeSet.setMergedWith(new Change("",mergedWith));
        patientChangeSet.setActive(new Change(true, false));
        return patientChangeSet;
    }

    private static PatientUpdate patientUpdate(String healthId, UUID eventId, PatientChangeSet patientChangeSet) {
        PatientUpdate patientUpdate = new PatientUpdate();
        patientUpdate.setHealthId(healthId);
        patientUpdate.setEventId(eventId);
        patientUpdate.setChangeSetMap(patientChangeSet);
        return patientUpdate;
    }

}
