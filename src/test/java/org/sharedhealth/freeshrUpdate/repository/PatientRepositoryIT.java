package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sharedhealth.freeshrUpdate.config.SHREnvironmentMock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfig;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother;
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

    @Test
    public void shouldMergePatientIrrespectiveOfPreviousUpdate() throws Exception {
        PatientUpdate patientUpdate = PatientUpdateMother.merge("P1", "P2");

        insertPatient("P1");
        assertTrue(patientRepository.mergeIfFound(patientUpdate).toBlocking().first());
        assertTrue(patientRepository.mergeIfFound(patientUpdate).toBlocking().first());

    }

    private void insertPatient(String healthId) {
        Insert insert = QueryBuilder.insertInto("freeshr", "patient").value("health_id", healthId);
        cqlOperations.execute(insert);
    }
}