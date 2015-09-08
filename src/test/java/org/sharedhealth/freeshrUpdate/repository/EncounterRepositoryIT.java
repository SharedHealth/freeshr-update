package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sharedhealth.freeshrUpdate.config.SHREnvironmentMock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfig;
import org.sharedhealth.freeshrUpdate.domain.EncounterBundle;
import org.sharedhealth.freeshrUpdate.utils.TimeUuidUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = ShrUpdateConfig.class)
public class EncounterRepositoryIT {

    @Autowired
    EncounterRepository encounterRepository;

    @Autowired
    @Qualifier("SHRCassandraTemplate")
    CqlOperations cqlOperations;


    @Test
    public void shouldFetchEncounterIdsForAGivenPatient() throws Exception {

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
    public void shouldFetchEncountersForAGivenPatient() throws Exception {
        Insert insertE1P1 = QueryBuilder.insertInto("freeshr", "enc_by_patient").value("encounter_id", "E1").value("health_id","P1").value("created_at", TimeUuidUtil.uuidForDate(new DateTime(2015,07,8,0,0).toDate()));
        Insert insertE2P1 = QueryBuilder.insertInto("freeshr", "enc_by_patient").value("encounter_id", "E2").value("health_id","P1").value("created_at", TimeUuidUtil.uuidForDate(new DateTime(2015,07,9,0,0).toDate()));
        Insert insertE3P2 = QueryBuilder.insertInto("freeshr", "enc_by_patient").value("encounter_id", "E3").value("health_id","P2").value("created_at", TimeUuidUtil.uuidForDate(new DateTime(2015,07,8,0,0).toDate()));
        cqlOperations.execute(insertE1P1);
        cqlOperations.execute(insertE2P1);
        cqlOperations.execute(insertE3P2);

        Insert insertE1 = QueryBuilder.insertInto("freeshr", "encounter").value("encounter_id", "E1").value("health_id","P1").value("received_at", TimeUuidUtil.uuidForDate(new DateTime(2015,07,8,0,0).toDate())).value("content_v1","e1 content");
        Insert insertE2 = QueryBuilder.insertInto("freeshr", "encounter").value("encounter_id", "E2").value("health_id","P1").value("received_at", TimeUuidUtil.uuidForDate(new DateTime(2015,07,9,0,0).toDate())).value("content_v1","e2 content");
        Insert insertE3 = QueryBuilder.insertInto("freeshr", "encounter").value("encounter_id", "E3").value("health_id","P2").value("received_at", TimeUuidUtil.uuidForDate(new DateTime(2015,07,8,0,0).toDate())).value("content_v1","e3 content");
        cqlOperations.execute(insertE1);
        cqlOperations.execute(insertE2);
        cqlOperations.execute(insertE3);

        Iterable<EncounterBundle> encounters = encounterRepository.getEncounterBundles("P1").toBlocking().toIterable();

        ArrayList<EncounterBundle> encountersForP1 = Lists.newArrayList(encounters);

        assertThat(encountersForP1.size(), is(2));
        assertEncounter(encountersForP1.get(0), "E1", "P1", "e1 content");
        assertEncounter(encountersForP1.get(1),"E2","P1","e2 content");

    }

    private void assertEncounter(EncounterBundle encounterBundle, String encounterId, String healthId, String content) {
        assertEquals(encounterId, encounterBundle.getEncounterId());
        assertEquals(healthId, encounterBundle.getHealthId());
        assertEquals(content, encounterBundle.getEncounterContent().toString());
    }
}
