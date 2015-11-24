package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.Row;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sharedhealth.freeshrUpdate.config.SHREnvironmentMock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfig;
import org.sharedhealth.freeshrUpdate.domain.Address;
import org.sharedhealth.freeshrUpdate.domain.EncounterBundle;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother;
import org.sharedhealth.freeshrUpdate.utils.QueryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rx.Observable;
import rx.functions.Action0;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother.addressChange;
import static org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother.confidentialPatient;

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

    private QueryUtils queryUtils;

    @Before
    public void setUp() throws Exception {
        queryUtils = new QueryUtils(cqlOperations);
    }


    @Test
    public void shouldFetchEncounterIdsForAGivenPatient() {
        queryUtils.insertEncByPatient("E1", "P1", new DateTime(2015, 07, 8, 0, 0).toDate());
        queryUtils.insertEncByPatient("E2", "P1", new DateTime(2015, 07, 9, 0, 0).toDate());
        queryUtils.insertEncByPatient("E2", "P2", new DateTime(2015, 07, 8, 0, 0).toDate());
        List<String> encounterIdsForP1 = encounterRepository.getEncounterIdsForPatient("P1").toBlocking().first();
        assertEquals(encounterIdsForP1, asList("E1", "E2"));
    }

    @Test
    public void shouldfetchEncountersForAGivenPatient() {
        queryUtils.insertEncByPatient("E1", "P1", new DateTime(2015, 07, 8, 0, 0).toDate());
        queryUtils.insertEncByPatient("E2", "P1", new DateTime(2015, 07, 9, 0, 0).toDate());
        queryUtils.insertEncByPatient("E3", "P2", new DateTime(2015, 07, 8, 0, 0).toDate());

        queryUtils.insertEncounter("E1", "P1", new DateTime(2015, 07, 8, 0, 0).toDate(), "e1 content", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncounter("E2", "P1", new DateTime(2015, 07, 9, 0, 0).toDate(), "e2 content", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncounter("E3", "P2", new DateTime(2015, 07, 8, 0, 0).toDate(), "e3 content", queryBuilder.getEncounterContentColumnName());

        Iterable<EncounterBundle> encounters = encounterRepository.getEncounterBundles("P1").toBlocking().toIterable();

        ArrayList<EncounterBundle> encountersForP1 = Lists.newArrayList(encounters);

        assertThat(encountersForP1.size(), is(2));
        queryUtils.assertEncounter(encountersForP1.get(0), "E1", "P1", "e1 content");
        queryUtils.assertEncounter(encountersForP1.get(1), "E2", "P1", "e2 content");

    }

    @Test
    public void shouldMergeEncountersForMergePatientUpdateFeed() throws Exception {
        queryUtils.insertEncByPatient("E1", "P1", new DateTime(2015, 07, 8, 0, 0).toDate());
        queryUtils.insertEncByPatient("E2", "P1", new DateTime(2015, 07, 9, 0, 0).toDate());
        queryUtils.insertEncByPatient("E3", "P2", new DateTime(2015, 07, 8, 0, 0).toDate());

        queryUtils.insertEncounter("E1", "P1", new DateTime(2015, 07, 8, 0, 0).toDate(), "e1 content for P1", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncounter("E2", "P1", new DateTime(2015, 07, 9, 0, 0).toDate(), "e2 content for P1", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncounter("E3", "P2", new DateTime(2015, 07, 8, 0, 0).toDate(), "e3 content for P2", queryBuilder.getEncounterContentColumnName());

        Observable<Boolean> mergeObservable = encounterRepository.applyMerge(PatientUpdateMother.merge("P2", "P1"));

        mergeObservable.doOnSubscribe(new Action0() {
            @Override
            public void call() {
                queryUtils.assertEncounterRow(queryUtils.fetchEncounter("E1"), "E1", "P1", "e1 content for P1", null);
                queryUtils.assertEncounterRow(queryUtils.fetchEncounter("E2"), "E2", "P1", "e2 content for P1", null);
                queryUtils.assertEncounterRow(queryUtils.fetchEncounter("E3"), "E3", "P1", "e3 content for P1", null);

                assertEquals(queryUtils.fetchEncounterByPatientFeed("P1"), 3);
                assertEquals(queryUtils.fetchEncounterByPatientFeed("P2"), 1);
            }
        });
    }

    @Test
    public void shouldAssociateAnEncounterWithNewHealthId() {
        EncounterBundle encounterBundle = new EncounterBundle("E1", "P1", "E1 content for P1", new DateTime(2015, 07, 8, 0, 0).toDate());
        queryUtils.insertEncounter(encounterBundle.getEncounterId(), encounterBundle.getHealthId(), encounterBundle.getReceivedAt(), encounterBundle.getEncounterContent(), queryBuilder.getEncounterContentColumnName());
        Row encounterBeforeMerge = queryUtils.fetchEncounter("E1");
        queryUtils.assertEncounterRow(encounterBeforeMerge, "E1", "P1", "E1 content for P1", null);

        encounterRepository.associateEncounterBundleTo(encounterBundle, "P2").toBlocking().first();
        Row encounterBundleAfterMerge = queryUtils.fetchEncounter("E1");
        List<Row> encByPatient = queryUtils.fetchEncounterByPatientFeed("P2");

        queryUtils.assertEncounterRow(encounterBundleAfterMerge, "E1", "P2", "E1 content for P2", null);
        assertThat(encByPatient.size(), is(1));
        assertThat(encByPatient.get(0).getString("encounter_id"), is("E1"));
        assertThat(encByPatient.get(0).getString("health_id"), is("P2"));
    }

    @Test
    public void shouldUpdateEncounterTableForPatientConfidentialityChanges() throws Exception {
        queryUtils.insertEncounter("E1", "P1", new DateTime(2015, 07, 8, 0, 0).toDate(), "e1 content", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncounter("E2", "P1", new DateTime(2015, 07, 9, 0, 0).toDate(), "e2 content", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncByPatient("E1", "P1", new DateTime(2015, 07, 8, 0, 0).toDate());
        queryUtils.insertEncByPatient("E2", "P1", new DateTime(2015, 07, 9, 0, 0).toDate());

        queryUtils.assertEncounterRow(queryUtils.fetchEncounter("E1"), "E1", "P1", "e1 content", null);
        queryUtils.assertEncounterRow(queryUtils.fetchEncounter("E2"), "E2", "P1", "e2 content", null);

        PatientUpdate patientUpdate = confidentialPatient("P1");
        Observable<Boolean> updateObservable = encounterRepository.applyUpdate(patientUpdate);

        updateObservable.doOnSubscribe(new Action0() {
            @Override
            public void call() {
                queryUtils.assertEncounterRow(queryUtils.fetchEncounter("E1"), "E1", "P1", "e1 content", "YES");
                queryUtils.assertEncounterRow(queryUtils.fetchEncounter("E2"), "E2", "P1", "e2 content", "YES");
            }
        });

    }

    @Test
    public void shouldAddEntriesToCatchmentFeedForAddressChange() throws Exception {
        Address address = new Address();
        address.setDivisionId("30");
        address.setDistrictId("26");
        address.setUpazilaId("18");
        address.setCityCorporationId("60");
        address.setUnionOrUrbanWardId("45");

        queryUtils.insertEncounter("E1", "P1", new DateTime(2015, 07, 8, 0, 0).toDate(), "e1 content", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncounter("E2", "P1", new DateTime(2015, 07, 9, 0, 0).toDate(), "e2 content", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncounter("E3", "P1", new DateTime(2015, 07, 10, 0, 0).toDate(), "e3 content", queryBuilder.getEncounterContentColumnName());

        queryUtils.insertEncByPatient("E1", "P1", new DateTime(2015, 07, 8, 0, 0).toDate());
        queryUtils.insertEncByPatient("E2", "P1", new DateTime(2015, 07, 9, 0, 0).toDate());
        queryUtils.insertEncByPatient("E3", "P1", new DateTime(2015, 07, 10, 0, 0).toDate());

        queryUtils.insertEncounterByCatchment("E1", "20", "15", new DateTime(2015, 8, 8, 0, 0).toDate());
        queryUtils.insertEncounterByCatchment("E2", "20", "15", new DateTime(2015, 8, 9, 0, 0).toDate());
        queryUtils.insertEncounterByCatchment("E3", "20", "15", new DateTime(2015, 8, 10, 0, 0).toDate());

        assertThat(queryUtils.fetchCatchmentFeed("20", "15").size(), is(3));
        assertThat(queryUtils.fetchCatchmentFeed("30", "3026").size(), is(0));

        PatientUpdate patientUpdate = new PatientUpdate();
        patientUpdate.setChangeSetMap(addressChange(address));
        patientUpdate.setHealthId("P1");

        Observable<Boolean> updateObservable = encounterRepository.applyUpdate(patientUpdate);

        updateObservable.doOnSubscribe(new Action0() {
            @Override
            public void call() {
                assertThat(queryUtils.fetchCatchmentFeed("20", "15").size(), is(3));
                List<Row> rows = queryUtils.fetchCatchmentFeed("30", "3026");
                assertThat(rows.size(), is(3));
                assertThat(rows.get(0).getString("upazila_id"), is("302618"));
                assertThat(rows.get(0).getString("city_corporation_id"), is("30261860"));
                assertThat(rows.get(0).getString("union_urban_ward_id"), is("3026186045"));
                assertThat(rows.get(0).getString("encounter_id"), is("E1"));
                assertThat(rows.get(1).getString("encounter_id"), is("E2"));
                assertThat(rows.get(2).getString("encounter_id"), is("E3"));
            }
        });


    }


    @After
    public void tearDown() {
        queryUtils.trucateAllTables();
    }
}
