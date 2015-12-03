package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.Row;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sharedhealth.freeshrUpdate.config.SHREnvironmentMock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfig;
import org.sharedhealth.freeshrUpdate.domain.Address;
import org.sharedhealth.freeshrUpdate.domain.Patient;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother;
import org.sharedhealth.freeshrUpdate.utils.QueryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.*;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.CONFIDENTIALITY_COLUMN_NAME;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.UPAZILA_ID_COLUMN_NAME;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = ShrUpdateConfig.class)
public class PatientRepositoryIT {

    @Autowired
    PatientRepository patientRepository;

    @Autowired
    @Qualifier("SHRCassandraTemplate")
    CqlOperations cqlOperations;

    private QueryUtils queryUtils;

    @Before
    public void setUp() throws Exception {
        queryUtils = new QueryUtils(cqlOperations);
    }

    @Test
    public void shouldMergePatientIrrespectiveOfPreviousUpdate() throws Exception {
        PatientUpdate patientUpdate = PatientUpdateMother.merge("P1", "P2");

        queryUtils.insertPatient("P1");
        assertTrue(patientRepository.mergeIfFound(patientUpdate).toBlocking().first());
        assertTrue(patientRepository.mergeIfFound(patientUpdate).toBlocking().first());

    }

    @Test
    public void shouldSaveInactivePatient() throws Exception {
        Patient patient = new Patient();
        patient.setHealthId("123");
        patient.setActive(false);
        patient.setMergedWith("Some other active patient");

        patientRepository.save(patient).toBlocking().first();
        assertPatient(patient, queryUtils.fetchPatient("123"));
    }

    @Test
    public void shouldSaveActivePatient() throws Exception {
        Patient patient = new Patient();
        patient.setHealthId("123");
        patient.setActive(true);
        patient.setGender("M");
        Address address = new Address();
        address.setAddressLine("abc");
        address.setDivisionId("10");
        address.setDistrictId("20");
        address.setUpazilaId("30");
        patient.setAddress(address);
        patient.setConfidentiality("YES");

        patientRepository.save(patient).toBlocking().first();
        assertPatient(patient, queryUtils.fetchPatient("123"));

    }

    @Test
    public void shouldVerifyIfPatientIsPresentOnLocal() throws Exception {
        queryUtils.insertPatient("P1");
        assertTrue(patientRepository.checkPatientExists("P1").toBlocking().first());
        assertFalse(patientRepository.checkPatientExists("Some random patient").toBlocking().first());

    }

    public void assertPatient(Patient patient, Row row) {
        assertEquals(patient.getHealthId(), row.getString(HEALTH_ID_COLUMN_NAME));
        assertEquals(patient.getGender(), row.getString(GENDER_COLUMN_NAME));
        assertEquals(patient.getMergedWith(), row.getString(MERGED_WITH_COLUMN_NAME));
        assertEquals(patient.isActive(), row.getBool(ACTIVE_COLUMN_NAME));
        if (patient.getAddress() != null) {
            assertEquals(patient.getAddress().getAddressLine(), row.getString(ADDRESS_LINE_COLUMN_NAME));
            assertEquals(patient.getAddress().getDivisionId(), row.getString(DIVISION_ID_COLUMN_NAME));
            assertEquals(patient.getAddress().getDistrictId(), row.getString(DISTRICT_ID_COLUMN_NAME));
            assertEquals(patient.getAddress().getUpazilaId(), row.getString(UPAZILA_ID_COLUMN_NAME));
        }
        if(patient.getConfidentiality() != null)
            assertEquals(patient.getConfidentiality().getLevel(), row.getString(CONFIDENTIALITY_COLUMN_NAME));
    }

    @After
    public void tearDown() {
        queryUtils.trucateAllTables();
    }
}