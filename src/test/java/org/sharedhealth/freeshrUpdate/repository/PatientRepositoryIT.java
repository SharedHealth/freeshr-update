package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sharedhealth.freeshrUpdate.config.SHREnvironmentMock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfig;
import org.sharedhealth.freeshrUpdate.domain.Address;
import org.sharedhealth.freeshrUpdate.domain.Patient;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother;
import org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = ShrUpdateConfig.class)
public class PatientRepositoryIT {

    @Autowired
    PatientRepository patientRepository;

    @Autowired
    @Qualifier("SHRCassandraTemplate")
    CqlOperations cqlOperations;

    @Test
    public void shouldMergePatientIrrespectiveOfPreviousUpdate() throws Exception {
        PatientUpdate patientUpdate = PatientUpdateMother.merge("P1", "P2");

        insertPatient("P1");
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
        assertPatient(patient, fetchPatient("123"));
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
        assertPatient(patient, fetchPatient("123"));

    }

    private void assertPatient(Patient patient, Row row) {
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


    private void insertPatient(String healthId) {
        Insert insert = QueryBuilder.insertInto("freeshr", "patient").value("health_id", healthId);
        cqlOperations.execute(insert);
    }

    private Row fetchPatient(String healthId) {
        ResultSet rs = cqlOperations.query(QueryBuilder.select().all().from("freeshr", "patient").where(eq(KeySpaceUtils.HEALTH_ID_COLUMN_NAME, healthId)).limit(1));
        return rs.all().get(0);
    }

    @After
    public void tearDown() {
        cqlOperations.execute("truncate encounter");
        cqlOperations.execute("truncate enc_by_catchment");
        cqlOperations.execute("truncate enc_by_patient");
        cqlOperations.execute("truncate patient");
    }
}