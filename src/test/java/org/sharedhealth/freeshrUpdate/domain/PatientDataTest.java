package org.sharedhealth.freeshrUpdate.domain;

import junit.framework.Assert;
import org.junit.Test;
import org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother;

import java.util.Map;

import static junit.framework.Assert.assertEquals;
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

    @Test
    public void shouldIdentifyChanges() throws Exception {
        AddressData addressData = new AddressData();
        addressData.setAddressLine("foo");
        addressData.setDivisionId("foo");
        addressData.setDistrictId("foo");
        addressData.setUpazilaId("foo");
        addressData.setCityCorporationId("foo");
        addressData.setUnionOrUrbanWardId("foo");
        PatientData patientData = PatientUpdateMother.addressChange(addressData);
        patientData.setConfidential("Yes");
        assertTrue(patientData.hasChanges());
        Map<String, Object> changes = patientData.getChanges();
        assertEquals(7, changes.size());
    }

    @Test
    public void shouldIdentifyConfidentialChangesAsBoolean() throws Exception {
        PatientData patientData = PatientUpdateMother.changeOnlyConfidential("Yes");
        assertTrue(patientData.hasChanges());
        Assert.assertTrue((Boolean) patientData.getChanges().get("confidential"));
    }
}