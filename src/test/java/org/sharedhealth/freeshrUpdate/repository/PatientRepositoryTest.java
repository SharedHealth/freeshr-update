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

import java.util.HashMap;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static junit.framework.Assert.*;
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
        when(shrQueryBuilder.checkPatientExistsQuery(anyString())).thenReturn(selectQuery);
        when(cqlOperations.queryAsynchronously(selectQuery)).thenReturn(resultSetFuture);
        PatientRepository patientRepository = new PatientRepository(cqlOperations, shrQueryBuilder);
        patientRepository.applyUpdate(patientUpdate).toBlocking().first();

        verify(shrQueryBuilder, times(1)).checkPatientExistsQuery(patientUpdate.getHealthId());
        verify(shrQueryBuilder, times(0)).updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges());

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
        when(shrQueryBuilder.checkPatientExistsQuery(anyString())).thenReturn(selectQuery);
        when(cqlOperations.queryAsynchronously(selectQuery)).thenReturn(resultSetFuture);

        Statement updateQuery = getUpdateQuery();
        when(shrQueryBuilder.updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges())).thenReturn(updateQuery);
        when(resultSetFuture.get()).thenReturn(resultSet);
        when(cqlOperations.executeAsynchronously(updateQuery)).thenReturn(resultSetFuture);

        new PatientRepository(cqlOperations, shrQueryBuilder)
                .applyUpdate(patientUpdate).toBlocking().first();

        verify(shrQueryBuilder, times(1)).checkPatientExistsQuery(patientUpdate.getHealthId());
        verify(shrQueryBuilder, times(1)).updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges());

        ArgumentCaptor<Statement> captor = ArgumentCaptor.forClass(Statement.class);
        verify(cqlOperations, times(1)).executeAsynchronously(captor.capture());
    }

    @Test
    public void shouldMergeExistingPatient() throws Exception {
        PatientUpdate patientUpdate = PatientUpdateMother.merge("P1", "P2");

        when(result.getLong("count")).thenReturn(1L);
        when(resultSet.one()).thenReturn(result);
        when(resultSetFuture.get()).thenReturn(resultSet);
        Select selectQuery = getSelectQuery(patientUpdate.getHealthId());
        when(shrQueryBuilder.checkPatientExistsQuery(anyString())).thenReturn(selectQuery);
        when(cqlOperations.queryAsynchronously(selectQuery)).thenReturn(resultSetFuture);

        Statement updateQuery = getUpdateQuery();
        when(shrQueryBuilder.updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientMergeChanges())).thenReturn(updateQuery);
        when(resultSetFuture.get()).thenReturn(resultSet);
        when(cqlOperations.executeAsynchronously(updateQuery)).thenReturn(resultSetFuture);

        new PatientRepository(cqlOperations, shrQueryBuilder)
                .mergeUpdate(patientUpdate).toBlocking().first();

        verify(shrQueryBuilder, times(1)).checkPatientExistsQuery("P1");
        verify(shrQueryBuilder, times(1)).updatePatientQuery("P1", new HashMap<String,Object>(){{
           put("active", false);
           put("merged_with", "P2");
        }});

        ArgumentCaptor<Statement> captor = ArgumentCaptor.forClass(Statement.class);
        verify(cqlOperations, times(1)).executeAsynchronously(captor.capture());
        Statement statement = captor.getValue();
        assertEquals(updateQuery, statement);
    }

    @Test
    public void shouldNotMergePatientIfPatientNotPresent() throws Exception {
        PatientUpdate patientUpdate = PatientUpdateMother.merge("P1", "P2");

        when(result.getLong("count")).thenReturn(0L);
        when(resultSet.one()).thenReturn(result);
        when(resultSetFuture.get()).thenReturn(resultSet);
        Select selectQuery = getSelectQuery(patientUpdate.getHealthId());
        when(shrQueryBuilder.checkPatientExistsQuery(anyString())).thenReturn(selectQuery);
        when(cqlOperations.queryAsynchronously(selectQuery)).thenReturn(resultSetFuture);

        Boolean patientMergeResult = new PatientRepository(cqlOperations, shrQueryBuilder)
                .mergeUpdate(patientUpdate).toBlocking().first();

        verify(shrQueryBuilder, times(1)).checkPatientExistsQuery("P1");
        verify(shrQueryBuilder, times(0)).updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientMergeChanges());

        assertFalse(patientMergeResult);

    }

    private Statement getUpdateQuery() {
        return QueryBuilder.update("foo", "bar");
    }

    public Select getSelectQuery(String healthId) {
        return QueryBuilder.select().from("foo", "bar").where(eq("health_id", healthId)).limit(1);
    }
}