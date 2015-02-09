package org.sharedhealth.freeshrUpdate.shrUpdate;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother;
import org.springframework.cassandra.core.CqlOperations;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PatientRepositoryTest {
    @Mock
    CqlOperations cqlOperations;

    @Mock
    PatientUpdateQuery patientUpdateQuery;

    @Mock
    ResultSet resultSet;

    @Mock
    ResultSetFuture resultSetFuture;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldUpdatePatient() throws Exception {
        PatientUpdate patientUpdate = PatientUpdateMother.confidentialPatient();
        Statement updateQuery = getUpdateQuery();
        when(patientUpdateQuery.get(patientUpdate)).thenReturn(updateQuery);
        when(resultSetFuture.get()).thenReturn(resultSet);
        when(cqlOperations.executeAsynchronously(updateQuery)).thenReturn(resultSetFuture);
        new PatientRepository(cqlOperations, patientUpdateQuery)
                .applyUpdate(patientUpdate).toBlocking().first();
        verify(patientUpdateQuery, times(1)).get(patientUpdate);
        ArgumentCaptor<Statement> captor = ArgumentCaptor.forClass(Statement.class);
        verify(cqlOperations, times(1)).executeAsynchronously(captor.capture());
        Statement statement = captor.getValue();
        System.out.println(statement.toString());
    }

    private Statement getUpdateQuery() {
        return QueryBuilder.update("foo", "bar");
    }
}