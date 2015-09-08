package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother;
import org.springframework.cassandra.core.CqlOperations;

import java.util.List;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.*;

public class EncounterRepositoryTest {
    @Mock
    private CqlOperations cqlOperations;
    @Mock
    private SHRQueryBuilder shrQueryBuilder;
    @Mock
    private ResultSetFuture resultSetFuture;
    @Mock
    private ResultSet resultSet;
    @Mock
    private Row rowOne;
    @Mock
    private Row rowTwo;

    private EncounterRepository encounterRepository;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        encounterRepository = new EncounterRepository(cqlOperations, shrQueryBuilder);
    }

    @Test
    public void shouldUpdateAllEncountersForGivenPatient() throws Exception {
        List<Row> rows = asList(rowOne, rowTwo);
        String encounterId1 = "123";
        String encounterId2 = "678";
        List<String> encounterIds = asList(encounterId1, encounterId2);
        Statement encountersQuery = getEncountersQuery(encounterIds);
        PatientUpdate patientUpdate = PatientUpdateMother.confidentialPatient();
        String healthId = patientUpdate.getHealthId();
        Statement encounterIdsQuery = getEncounterIdsQuery(healthId);
        Statement encounterUpdateQuery = getEncounterUpdateQuery();

        when(rowOne.getString(ENCOUNTER_ID_COLUMN_NAME)).thenReturn(encounterId1);
        when(rowTwo.getString(ENCOUNTER_ID_COLUMN_NAME)).thenReturn(encounterId2);
        when(rowOne.getString(RECEIVED_DATE_COLUMN_NAME)).thenReturn("date1");
        when(rowTwo.getString(RECEIVED_DATE_COLUMN_NAME)).thenReturn("date2");
        when(resultSet.all()).thenReturn(rows);
        when(resultSetFuture.get()).thenReturn(resultSet);

        when(cqlOperations.queryAsynchronously(getEncounterIdsQuery(healthId).toString())).thenReturn(resultSetFuture);
        when(cqlOperations.queryAsynchronously(getEncountersQuery(encounterIds).toString())).thenReturn(resultSetFuture);
        when(cqlOperations.executeAsynchronously(encounterUpdateQuery)).thenReturn(resultSetFuture);

        when(shrQueryBuilder.findEncounterIdsQuery(healthId)).thenReturn(encounterIdsQuery);
        when(shrQueryBuilder.findEncounterDetailsByEncounterIdsQuery(encounterIds)).thenReturn(encountersQuery);
        when(shrQueryBuilder.updateEncounterQuery(Matchers.eq(patientUpdate), any(EncounterDetail.class))).thenReturn(encounterUpdateQuery);

        encounterRepository.applyUpdate(patientUpdate).toBlocking().first();

        verify(shrQueryBuilder, times(1)).findEncounterIdsQuery(healthId);
        verify(cqlOperations, times(1)).queryAsynchronously(encounterIdsQuery.toString());
        verify(shrQueryBuilder, times(1)).findEncounterDetailsByEncounterIdsQuery(encounterIds);
        verify(cqlOperations, times(1)).queryAsynchronously(encountersQuery.toString());

        ArgumentCaptor<EncounterDetail> captor = ArgumentCaptor.forClass(EncounterDetail.class);
        verify(shrQueryBuilder, times(2)).updateEncounterQuery(Matchers.eq(patientUpdate), captor.capture());
        EncounterDetail encounterDetail1 = captor.getAllValues().get(0);
        EncounterDetail encounterDetail2 = captor.getAllValues().get(1);
        assertEquals(encounterId1, encounterDetail1.getEncounterId());
        assertEquals(encounterId2, encounterDetail2.getEncounterId());
        verify(cqlOperations, times(2)).executeAsynchronously(encounterUpdateQuery);
    }

    private Statement getEncounterUpdateQuery() {
        return QueryBuilder.update("keyspaceName", "tableName").with(set(CONFIDENTIALITY_COLUMN_NAME, "V"))
                .where(eq(ENCOUNTER_ID_COLUMN_NAME, "123")).and(eq(RECEIVED_DATE_COLUMN_NAME, "date"));
    }

    private Statement getEncountersQuery(List<String> encounterIds) {
        return QueryBuilder.select().from("keyspaceName", "tableName").where(in(ENCOUNTER_ID_COLUMN_NAME, encounterIds.toArray()));
    }

    private Statement getEncounterIdsQuery(String healthId) {
        return QueryBuilder.select().from("keyspaceName", "tableName").where(eq(HEALTH_ID_COLUMN_NAME, healthId)).limit(1);
    }
}