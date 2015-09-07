package org.sharedhealth.freeshrUpdate.domain;

import org.junit.Test;
import org.sharedhealth.freeshrUpdate.utils.Confidentiality;

import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother.addressChange;
import static org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother.changeOnlyConfidential;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.CONFIDENTIALITY_COLUMN_NAME;

public class PatientDataTest {

    @Test
    public void shouldIdentifyUnchanged() throws Exception {
        PatientData patientData = new PatientData();
        assertFalse(patientData.hasPatientDetailChanges());
    }

    @Test
    public void shouldIdentifyConfidentialChanges() throws Exception {
        PatientData confidentialChange = changeOnlyConfidential("Yes");
        assertTrue(confidentialChange.hasPatientDetailChanges());
        assertTrue(confidentialChange.hasConfidentialChange());
    }

    @Test
    public void shouldIdentifyAddressLineChanges() throws Exception {
        AddressData addressData = new AddressData();
        addressData.setAddressLine("foo");
        PatientData addressChange = addressChange(addressData);

        assertTrue(addressChange.hasPatientDetailChanges());
        assertFalse(addressChange.hasConfidentialChange());
    }

    @Test
    public void shouldIdentifyDivisionIdChanges() throws Exception {
        AddressData addressData = new AddressData();
        addressData.setDivisionId("foo");
        assertTrue(addressChange(addressData).hasPatientDetailChanges());
    }

    @Test
    public void shouldIdentifyDistrictIdChanges() throws Exception {
        AddressData addressData = new AddressData();
        addressData.setDistrictId("foo");
        assertTrue(addressChange(addressData).hasPatientDetailChanges());
    }

    @Test
    public void shouldIdentifyUpazilaIdChanges() throws Exception {
        AddressData addressData = new AddressData();
        addressData.setUpazilaId("foo");
        assertTrue(addressChange(addressData).hasPatientDetailChanges());
    }

    @Test
    public void shouldIdentifyCityCorporationIdChanges() throws Exception {
        AddressData addressData = new AddressData();
        addressData.setCityCorporationId("foo");
        assertTrue(addressChange(addressData).hasPatientDetailChanges());
    }

    @Test
    public void shouldIdentifyUnionOrUrbanWardIdChanges() throws Exception {
        AddressData addressData = new AddressData();
        addressData.setUnionOrUrbanWardId("foo");
        assertTrue(addressChange(addressData).hasPatientDetailChanges());
    }

    @Test
    public void shouldIdentifyPatientMergeChanges() throws Exception {
        PatientData patientData = new PatientData();
        patientData.setActive(new Change(true, false));
        patientData.setMergedWith(new Change("","some hid"));
        assertTrue(patientData.isPatientMerged());
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
        PatientData patientData = addressChange(addressData);
        patientData.setConfidentialChange(new Change("old", "Yes"));
        assertTrue(patientData.hasPatientDetailChanges());
        Map<String, Object> changes = patientData.getPatientDetailChanges();
        assertEquals(7, changes.size());
    }

    @Test
    public void shouldIdentifyConfidentialityChangesAsVeryRestricted() throws Exception {
        PatientData patientData = changeOnlyConfidential("Yes");
        assertTrue(patientData.hasPatientDetailChanges());
        assertTrue(patientData.getPatientDetailChanges().get(CONFIDENTIALITY_COLUMN_NAME).equals(Confidentiality.VeryRestricted.getLevel()));
    }

    @Test
    public void shouldIdentifyConfidentialityChangesAsNormal() throws Exception {
        PatientData patientData = changeOnlyConfidential("No");
        assertTrue(patientData.hasPatientDetailChanges());
        assertTrue(patientData.getPatientDetailChanges().get(CONFIDENTIALITY_COLUMN_NAME).equals(Confidentiality.Normal.getLevel()));
    }
}