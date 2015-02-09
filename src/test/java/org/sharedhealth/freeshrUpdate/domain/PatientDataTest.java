package org.sharedhealth.freeshrUpdate.domain;

import org.junit.Test;
import org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PatientDataTest {

    @Test
    public void shouldIdentifyUnchanged() throws Exception {
        PatientData patientData = new PatientData();
        assertFalse(patientData.hasChanges());
    }

    @Test
    public void shouldIdentifyConfidentialChanges() throws Exception {
        assertTrue(PatientUpdateMother.changeOnlyConfidential("Yes").hasChanges());
    }

    @Test
    public void shouldIdentifyAddressLineChanges() throws Exception {
        AddressData addressData = new AddressData();
        addressData.setAddressLine("foo");
        assertTrue(PatientUpdateMother.addressChange(addressData).hasChanges());
    }

    @Test
    public void shouldIdentifyDivisionIdChanges() throws Exception {
        AddressData addressData = new AddressData();
        addressData.setDivisionId("foo");
        assertTrue(PatientUpdateMother.addressChange(addressData).hasChanges());
    }

    @Test
    public void shouldIdentifyDistrictIdChanges() throws Exception {
        AddressData addressData = new AddressData();
        addressData.setDistrictId("foo");
        assertTrue(PatientUpdateMother.addressChange(addressData).hasChanges());
    }

    @Test
    public void shouldIdentifyUpazilaIdChanges() throws Exception {
        AddressData addressData = new AddressData();
        addressData.setUpazilaId("foo");
        assertTrue(PatientUpdateMother.addressChange(addressData).hasChanges());
    }

    @Test
    public void shouldIdentifyCityCorporationIdChanges() throws Exception {
        AddressData addressData = new AddressData();
        addressData.setCityCorporationId("foo");
        assertTrue(PatientUpdateMother.addressChange(addressData).hasChanges());
    }

    @Test
    public void shouldIdentifyUnionOrUrbanWardIdChanges() throws Exception {
        AddressData addressData = new AddressData();
        addressData.setUnionOrUrbanWardId("foo");
        assertTrue(PatientUpdateMother.addressChange(addressData).hasChanges());
    }
}