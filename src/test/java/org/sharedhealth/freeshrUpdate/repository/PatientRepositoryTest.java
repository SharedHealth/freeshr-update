package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother;
import org.springframework.cassandra.core.CqlOperations;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PatientRepositoryTest {
    @Mock
    private CqlOperations cqlOperations;

    @Mock
    private SHRQueryBuilder shrQueryBuilder;

    @Mock
    private Row result;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSetFuture resultSetFuture;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldIgnoreNonexistentPatient() throws Exception {
        PatientUpdate patientUpdate = PatientUpdateMother.confidentialPatient();
        when(result.getLong("count")).thenReturn(0L);
        when(resultSet.one()).thenReturn(result);
        when(resultSetFuture.get()).thenReturn(resultSet);
        Select selectQuery = getSelectQuery(patientUpdate.getHealthId());
        when(shrQueryBuilder.findPatientQuery(anyString())).thenReturn(selectQuery);
        when(cqlOperations.queryAsynchronously(selectQuery)).thenReturn(resultSetFuture);
        PatientRepository patientRepository = new PatientRepository(cqlOperations, shrQueryBuilder);
        patientRepository.applyUpdate(patientUpdate).toBlocking().first();

        verify(shrQueryBuilder, times(1)).findPatientQuery(patientUpdate.getHealthId());
        verify(shrQueryBuilder, times(0)).updatePatientQuery(patientUpdate);

        ArgumentCaptor<Select> captor = ArgumentCaptor.forClass(Select.class);
        verify(cqlOperations, times(1)).queryAsynchronously(captor.capture());
        Statement statement = captor.getValue();
        assertTrue(statement.toString().contains(String.format("WHERE health_id='%s' LIMIT 1", patientUpdate
                .getHealthId())));
    }

    @Test
    public void shouldUpdateExistingPatient() throws Exception {
        PatientUpdate patientUpdate = PatientUpdateMother.confidentialPatient();

        when(result.getLong("count")).thenReturn(1L);
        when(resultSet.one()).thenReturn(result);
        when(resultSetFuture.get()).thenReturn(resultSet);
        Select selectQuery = getSelectQuery(patientUpdate.getHealthId());
        when(shrQueryBuilder.findPatientQuery(anyString())).thenReturn(selectQuery);
        when(cqlOperations.queryAsynchronously(selectQuery)).thenReturn(resultSetFuture);

        Statement updateQuery = getUpdateQuery();
        when(shrQueryBuilder.updatePatientQuery(patientUpdate)).thenReturn(updateQuery);
        when(resultSetFuture.get()).thenReturn(resultSet);
        when(cqlOperations.executeAsynchronously(updateQuery)).thenReturn(resultSetFuture);

        new PatientRepository(cqlOperations, shrQueryBuilder)
                .applyUpdate(patientUpdate).toBlocking().first();

        verify(shrQueryBuilder, times(1)).findPatientQuery(patientUpdate.getHealthId());
        verify(shrQueryBuilder, times(1)).updatePatientQuery(patientUpdate);

        ArgumentCaptor<Statement> captor = ArgumentCaptor.forClass(Statement.class);
        verify(cqlOperations, times(1)).executeAsynchronously(captor.capture());
        Statement statement = captor.getValue();
    }

    private Statement getUpdateQuery() {
        return QueryBuilder.update("foo", "bar");
    }

    public Select getSelectQuery(String healthId) {
        return QueryBuilder.select().from("foo", "bar").where(eq("health_id", healthId)).limit(1);
    }
}