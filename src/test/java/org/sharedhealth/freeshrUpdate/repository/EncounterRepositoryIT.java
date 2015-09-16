package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sharedhealth.freeshrUpdate.config.SHREnvironmentMock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfig;
import org.sharedhealth.freeshrUpdate.domain.EncounterBundle;
import org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils;
import org.sharedhealth.freeshrUpdate.utils.TimeUuidUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.ENCOUNTER_BY_PATIENT_TABLE_NAME;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.ENCOUNTER_ID_COLUMN_NAME;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.HEALTH_ID_COLUMN_NAME;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = ShrUpdateConfig.class)
public class EncounterRepositoryIT {

    @Autowired
    EncounterRepository encounterRepository;

    @Autowired
    SHRQueryBuilder queryBuilder;

    @Autowired
    @Qualifier("SHRCassandraTemplate")
    CqlOperations cqlOperations;


    @Test
    public void shouldFetchEncounterIdsForAGivenPatient(){

        Insert insertE1P1 = QueryBuilder.insertInto("freeshr", "enc_by_patient").value("encounter_id", "E1").value("health_id","P1").value("created_at", TimeUuidUtil.uuidForDate(new DateTime(2015,07,8,0,0).toDate()));
        Insert insertE2P1 = QueryBuilder.insertInto("freeshr", "enc_by_patient").value("encounter_id", "E2").value("health_id","P1").value("created_at", TimeUuidUtil.uuidForDate(new DateTime(2015,07,9,0,0).toDate()));
        Insert insertE3P2 = QueryBuilder.insertInto("freeshr", "enc_by_patient").value("encounter_id", "E3").value("health_id","P2").value("created_at", TimeUuidUtil.uuidForDate(new DateTime(2015,07,8,0,0).toDate()));
        cqlOperations.execute(insertE1P1);
        cqlOperations.execute(insertE2P1);
        cqlOperations.execute(insertE3P2);

        List<String> encounterIdsForP1 = encounterRepository.getEncounterIdsForPatient("P1").toBlocking().first();
        assertEquals(encounterIdsForP1, asList("E1","E2"));
    }

    @Test
    public void shouldFetchEncountersForAGivenPatient() {
        Insert insertE1P1 = QueryBuilder.insertInto("freeshr", "enc_by_patient").value("encounter_id", "E1").value("health_id","P1").value("created_at", TimeUuidUtil.uuidForDate(new DateTime(2015,07,8,0,0).toDate()));
        Insert insertE2P1 = QueryBuilder.insertInto("freeshr", "enc_by_patient").value("encounter_id", "E2").value("health_id","P1").value("created_at", TimeUuidUtil.uuidForDate(new DateTime(2015,07,9,0,0).toDate()));
        Insert insertE3P2 = QueryBuilder.insertInto("freeshr", "enc_by_patient").value("encounter_id", "E3").value("health_id","P2").value("created_at", TimeUuidUtil.uuidForDate(new DateTime(2015,07,8,0,0).toDate()));
        cqlOperations.execute(insertE1P1);
        cqlOperations.execute(insertE2P1);
        cqlOperations.execute(insertE3P2);

        insertEncounter("E1", "P1", new DateTime(2015, 07, 8, 0, 0).toDate(), "e1 content");
        insertEncounter("E2", "P1", new DateTime(2015, 07, 9, 0, 0).toDate(), "e2 content");
        insertEncounter("E3", "P2", new DateTime(2015, 07, 8, 0, 0).toDate(), "e3 content");

        Iterable<EncounterBundle> encounters = encounterRepository.getEncounterBundles("P1").toBlocking().toIterable();

        ArrayList<EncounterBundle> encountersForP1 = Lists.newArrayList(encounters);

        assertThat(encountersForP1.size(), is(2));
        assertEncounter(encountersForP1.get(0), "E1", "P1", "e1 content");
        assertEncounter(encountersForP1.get(1),"E2","P1","e2 content");

    }

    @Test
    public void shouldAssociateAnEncounterWithNewHealthId(){
        EncounterBundle encounterBundle = new EncounterBundle("E1", "P1", "E1 content for P1", new DateTime(2015,07,8,0,0).toDate());
        insertEncounter(encounterBundle.getEncounterId(), encounterBundle.getHealthId(), encounterBundle.getReceivedAt(), encounterBundle.getEncounterContent());
        EncounterBundle encounterBeforeMerge = fetchEncounterBundle("E1");
        assertEncounter(encounterBeforeMerge, "E1", "P1", "E1 content for P1");

        encounterRepository.associateEncounterBundleTo(encounterBundle, "P2").toBlocking().first();
        EncounterBundle encounterBundleAfterMerge = fetchEncounterBundle("E1");
        List<Row> encByPatient = fetchEncounterByPatientFeed();

        assertEncounter(encounterBundleAfterMerge, "E1", "P2", "E1 content for P2");
        assertThat(encByPatient.size(), is(1));
        assertThat("E1", is(encByPatient.get(0).getString("encounter_id")));
        assertThat("P2", is(encByPatient.get(0).getString("health_id")));
    }

    private EncounterBundle fetchEncounterBundle(String encounterId) {
        String encounterContentColumnName = queryBuilder.getEncounterContentColumnName();
        ResultSet rs = cqlOperations.query(QueryBuilder.select(ENCOUNTER_ID_COLUMN_NAME, encounterContentColumnName, HEALTH_ID_COLUMN_NAME).from("freeshr", "encounter").where(eq(KeySpaceUtils.ENCOUNTER_ID_COLUMN_NAME, encounterId)).limit(1));
        Row row = rs.all().get(0);
        String healthId = row.getString(HEALTH_ID_COLUMN_NAME);
        String content = row.getString(encounterContentColumnName);
        return new EncounterBundle(encounterId, healthId, content, null);
    }

    private List<Row> fetchEncounterByPatientFeed() {
        ResultSet rs = cqlOperations.query(QueryBuilder.select().all().from("freeshr", ENCOUNTER_BY_PATIENT_TABLE_NAME));
        return rs.all();
    }

    private void insertEncounter(String encounterId, String healthId, Date recievedAt, String content) {
        Insert insert = QueryBuilder.insertInto("freeshr", "encounter").value("encounter_id", encounterId).value("health_id", healthId).value("received_at", TimeUuidUtil.uuidForDate(recievedAt)).value(queryBuilder.getEncounterContentColumnName(), content);
        cqlOperations.execute(insert);
    }

    private void assertEncounter(EncounterBundle encounterBundle, String encounterId, String healthId, String content) {
        assertEquals(encounterId, encounterBundle.getEncounterId());
        assertEquals(healthId, encounterBundle.getHealthId());
        assertEquals(content, encounterBundle.getEncounterContent());
    }
}
