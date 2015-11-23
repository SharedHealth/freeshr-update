package org.sharedhealth.freeshrUpdate.domain;

import junit.framework.Assert;
import org.junit.Test;
import org.sharedhealth.freeshrUpdate.utils.Confidentiality;

import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother.addressChange;
import static org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother.changeOnlyConfidential;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.CONFIDENTIALITY_COLUMN_NAME;

public class PatientChangeSetTest {

    @Test
    public void shouldIdentifyUnchanged() throws Exception {
        PatientChangeSet patientChangeSet = new PatientChangeSet();
        assertFalse(patientChangeSet.hasPatientDetailChanges());
    }

    @Test
    public void shouldIdentifyConfidentialChanges() throws Exception {
        PatientChangeSet confidentialChange = changeOnlyConfidential("Yes");
        assertTrue(confidentialChange.hasPatientDetailChanges());
        assertTrue(confidentialChange.hasConfidentialChange());
    }

    @Test
    public void shouldIdentifyAddressLineChanges() throws Exception {
        Address address = new Address();
        address.setAddressLine("foo");
        PatientChangeSet addressChange = addressChange(address);

        assertTrue(addressChange.hasPatientDetailChanges());
        Assert.assertTrue(addressChange.hasAddressChange());
        assertFalse(addressChange.hasConfidentialChange());
    }

    @Test
    public void shouldIdentifyDivisionIdChanges() throws Exception {
        Address address = new Address();
        address.setDivisionId("foo");
        PatientChangeSet patientChangeSet = addressChange(address);
        assertTrue(patientChangeSet.hasPatientDetailChanges());
        assertTrue(patientChangeSet.hasAddressChange());
    }

    @Test
    public void shouldIdentifyDistrictIdChanges() throws Exception {
        Address address = new Address();
        address.setDistrictId("foo");
        PatientChangeSet patientChangeSet = addressChange(address);
        assertTrue(patientChangeSet.hasPatientDetailChanges());
        assertTrue(patientChangeSet.hasAddressChange());
    }

    @Test
    public void shouldIdentifyUpazilaIdChanges() throws Exception {
        Address address = new Address();
        address.setUpazilaId("foo");
        PatientChangeSet patientChangeSet = addressChange(address);
        assertTrue(patientChangeSet.hasPatientDetailChanges());
        assertTrue(patientChangeSet.hasAddressChange());
    }

    @Test
    public void shouldIdentifyCityCorporationIdChanges() throws Exception {
        Address address = new Address();
        address.setCityCorporationId("foo");
        PatientChangeSet patientChangeSet = addressChange(address);
        assertTrue(patientChangeSet.hasPatientDetailChanges());
        assertTrue(patientChangeSet.hasAddressChange());
    }

    @Test
    public void shouldIdentifyUnionOrUrbanWardIdChanges() throws Exception {
        Address address = new Address();
        address.setUnionOrUrbanWardId("foo");
        PatientChangeSet patientChangeSet = addressChange(address);
        assertTrue(patientChangeSet.hasPatientDetailChanges());
        assertTrue(patientChangeSet.hasAddressChange());
    }

    @Test
    public void shouldIdentifyPatientMergeChanges() throws Exception {
        PatientChangeSet patientChangeSet = new PatientChangeSet();
        patientChangeSet.setActive(new Change(true, false));
        patientChangeSet.setMergedWith(new Change("","some hid"));
        assertTrue(patientChangeSet.isPatientMerged());
    }

    @Test
    public void shouldIdentifyChanges() throws Exception {
        Address address = new Address();
        address.setAddressLine("foo");
        address.setDivisionId("foo");
        address.setDistrictId("foo");
        address.setUpazilaId("foo");
        address.setCityCorporationId("foo");
        address.setUnionOrUrbanWardId("foo");
        PatientChangeSet patientChangeSet = addressChange(address);
        patientChangeSet.setConfidentialChange(new Change("old", "Yes"));
        assertTrue(patientChangeSet.hasPatientDetailChanges());
        Map<String, Object> changes = patientChangeSet.getPatientDetailChanges();
        assertEquals(7, changes.size());
    }

    @Test
    public void shouldIdentifyConfidentialityChangesAsVeryRestricted() throws Exception {
        PatientChangeSet patientChangeSet = changeOnlyConfidential("Yes");
        assertTrue(patientChangeSet.hasPatientDetailChanges());
        assertTrue(patientChangeSet.getPatientDetailChanges().get(CONFIDENTIALITY_COLUMN_NAME).equals(Confidentiality.VeryRestricted.getLevel()));
    }

    @Test
    public void shouldIdentifyConfidentialityChangesAsNormal() throws Exception {
        PatientChangeSet patientChangeSet = changeOnlyConfidential("No");
        assertTrue(patientChangeSet.hasPatientDetailChanges());
        assertTrue(patientChangeSet.getPatientDetailChanges().get(CONFIDENTIALITY_COLUMN_NAME).equals(Confidentiality.Normal.getLevel()));
    }

    @Test
    public void shouldCheckIfAddressDataHasNoChanges() throws Exception {
        PatientChangeSet patientChangeSet = new PatientChangeSet();
        assertFalse(patientChangeSet.hasAddressChange());

    }
}