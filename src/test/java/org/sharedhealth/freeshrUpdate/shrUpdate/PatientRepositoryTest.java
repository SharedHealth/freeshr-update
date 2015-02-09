package org.sharedhealth.freeshrUpdate.shrUpdate;

import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.domain.PatientData;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother;
import org.springframework.cassandra.core.CqlOperations;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PatientRepositoryTest {
    @Mock
    CqlOperations cqlOperations;

    @Mock
    PatientUpdateQuery patientUpdateQuery;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldUpdateConfidential() throws Exception {
        PatientUpdate patientUpdate = PatientUpdateMother.confidentialPatient();
        when(patientUpdateQuery.get(patientUpdate)).thenReturn(getUpdateQuery());
        new PatientRepository(cqlOperations, patientUpdateQuery).applyUpdate(patientUpdate);
        verify(patientUpdateQuery, times(1)).get(patientUpdate);
        verify(cqlOperations, times(1)).executeAsynchronously(any(Statement.class));
    }

    private Statement getUpdateQuery() {
        return QueryBuilder.update("foo", "bar");
    }
}