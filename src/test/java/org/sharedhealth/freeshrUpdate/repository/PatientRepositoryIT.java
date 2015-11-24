package org.sharedhealth.freeshrUpdate.repository;

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

import static org.junit.Assert.assertTrue;


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
        queryUtils.assertPatient(patient, queryUtils.fetchPatient("123"));
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
        queryUtils.assertPatient(patient, queryUtils.fetchPatient("123"));

    }

    @After
    public void tearDown() {
        queryUtils.trucateAllTables();
    }
}