package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.Row;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sharedhealth.freeshrUpdate.config.SHREnvironmentMock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfig;
import org.sharedhealth.freeshrUpdate.domain.Address;
import org.sharedhealth.freeshrUpdate.domain.EncounterBundle;
import org.sharedhealth.freeshrUpdate.domain.Patient;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother;
import org.sharedhealth.freeshrUpdate.utils.QueryUtils;
import org.sharedhealth.freeshrUpdate.utils.TimeUuidUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
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

        // P1 from D1d1 has E1,E2
        // P2 from D2de has E3
        // P2 is merged with P1
        // P2's encounters are added to P1 after merge
        // P2's encounters are added to D1d1 catchement after merge
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        final int year = calendar.get(Calendar.YEAR);
        final Date currentTime = calendar.getTime();
        calendar.add(Calendar.HOUR, -1);
        final Date oneHoursAgo = calendar.getTime();
        calendar.add(Calendar.HOUR, -1);
        final Date twoHoursAgo = calendar.getTime();
        org.joda.time.DateTimeUtils.setCurrentMillisFixed(currentTime.getTime());

        queryUtils.insertEncByPatient("E1", "P1", twoHoursAgo);
        queryUtils.insertEncByPatient("E2", "P1", oneHoursAgo);
        queryUtils.insertEncByPatient("E3", "P2", twoHoursAgo);

        queryUtils.insertEncounter("E1", "P1", twoHoursAgo, "e1 content for P1", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncounter("E2", "P1", oneHoursAgo, "e2 content for P1", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncounter("E3", "P2", twoHoursAgo, "e3 content for P2", queryBuilder.getEncounterContentColumnName());

        queryUtils.insertEncounterByCatchment("E1", "D1", "D1d1", "D1d1u1", twoHoursAgo);
        queryUtils.insertEncounterByCatchment("E2", "D1", "D1d1", "D1d1u1", oneHoursAgo);
        queryUtils.insertEncounterByCatchment("E3", "D2", "D2d2", "D2d2u2", twoHoursAgo);

        assertEquals(2, queryUtils.fetchCatchmentFeed("D1", "D1d1", year).size());
        assertEquals(1, queryUtils.fetchCatchmentFeed("D2", "D2d2", year).size());

        Patient patientMergedWith = new Patient();
        patientMergedWith.setHealthId("P1");
        Address d1D1 = new Address();
        d1D1.setDistrictId("d1");
        d1D1.setDivisionId("D1");
        d1D1.setUpazilaId("u1");
        patientMergedWith.setAddress(d1D1);

        TestSubscriber<Boolean> mergeResultSubscriber = new TestSubscriber<>();
        Observable<Boolean> mergeObservable = encounterRepository.applyMerge(PatientUpdateMother.merge("P2", "P1"), patientMergedWith);
        mergeObservable.subscribe(mergeResultSubscriber);

        mergeResultSubscriber.awaitTerminalEvent();
        mergeResultSubscriber.assertNoErrors();
        mergeResultSubscriber.assertCompleted();

        queryUtils.assertEncounterRow(queryUtils.fetchEncounter("E1"), "E1", "P1", "e1 content for P1", null);
        queryUtils.assertEncounterRow(queryUtils.fetchEncounter("E2"), "E2", "P1", "e2 content for P1", null);
        queryUtils.assertEncounterRow(queryUtils.fetchEncounter("E3"), "E3", "P1", "e3 content for P1", null);

        assertEquals(3, queryUtils.fetchEncounterByPatientFeed("P1").size());
        assertEquals(1, queryUtils.fetchEncounterByPatientFeed("P2").size());

        List<Row> rows = queryUtils.fetchCatchmentFeed("D1", "D1d1", year);
        assertEquals(3, rows.size());
        queryUtils.assertEncounterByCatchmentRow(rows.get(0), new HashMap<String, String>() {{
            put("encounter_id", "E1");
            put("division_id", "D1");
            put("district_id", "D1d1");
            put("upazila_id", "D1d1u1");
            put("year", String.valueOf(year));
            put("created_at", TimeUuidUtil.uuidForDate(twoHoursAgo).toString());
        }}, true);

        queryUtils.assertEncounterByCatchmentRow(rows.get(1), new HashMap<String, String>() {{
            put("encounter_id", "E2");
            put("division_id", "D1");
            put("district_id", "D1d1");
            put("upazila_id", "D1d1u1");
            put("year", String.valueOf(year));
            put("created_at", TimeUuidUtil.uuidForDate(oneHoursAgo).toString());
        }}, true);

        queryUtils.assertEncounterByCatchmentRow(rows.get(2), new HashMap<String, String>() {{
            put("encounter_id", "E3");
            put("division_id", "D1");
            put("district_id", "D1d1");
            put("upazila_id", "D1d1u1");
            put("year", String.valueOf(year));
            put("created_at", TimeUuidUtil.uuidForDate(currentTime).toString());
            put("merged_at", TimeUuidUtil.uuidForDate(currentTime).toString());
        }}, false);

    }

    @Test
    public void shouldAssociateAnEncounterWithNewHealthId() {
        final Date currentTime = new Date();
        org.joda.time.DateTimeUtils.setCurrentMillisFixed(currentTime.getTime());

        EncounterBundle encounterBundle = new EncounterBundle("E1", "P1", "E1 content for P1", currentTime);
        queryUtils.insertEncounter(encounterBundle.getEncounterId(), encounterBundle.getHealthId(), encounterBundle.getReceivedAt(), encounterBundle.getEncounterContent(), queryBuilder.getEncounterContentColumnName());
        Row encounterBeforeMerge = queryUtils.fetchEncounter("E1");
        queryUtils.assertEncounterRow(encounterBeforeMerge, "E1", "P1", "E1 content for P1", null);

        Patient p2 = new Patient();
        p2.setHealthId("P2");
        encounterRepository.associateEncounterBundleTo(encounterBundle, p2).toBlocking().first();
        Row encounterBundleAfterMerge = queryUtils.fetchEncounter("E1");
        List<Row> encByPatient = queryUtils.fetchEncounterByPatientFeed("P2");

        queryUtils.assertEncounterRow(encounterBundleAfterMerge, "E1", "P2", "E1 content for P2", null);
        assertThat(encByPatient.size(), is(1));
        assertThat(encByPatient.get(0).getString("encounter_id"), is("E1"));
        assertThat(encByPatient.get(0).getString("health_id"), is("P2"));
        final String mergedAt = encByPatient.get(0).getUUID("merged_at").toString();
        assertEquals(extractDateFromUuidString(mergedAt), new SimpleDateFormat("dd-MM-yyyy").format(currentTime));
    }


    private void assertUuids(Row encounterByCatchmentRow, HashMap<String, String> expectedEncounterByCatchment, String column) {
        final String actualCreatedAt = encounterByCatchmentRow.getUUID(column).toString();
        final String expectedCreatedAt = expectedEncounterByCatchment.get("created_at");
        assertEquals(extractDateFromUuidString(expectedCreatedAt), extractDateFromUuidString(actualCreatedAt));
    }

    private String extractDateFromUuidString(String expectedCreatedAt) {
        final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        return format.format(TimeUuidUtil.getDateFromUUID(UUID.fromString(expectedCreatedAt)));
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
        TestSubscriber<Boolean> updateResultSubscriber = new TestSubscriber<>();
        updateObservable.subscribe(updateResultSubscriber);

        updateResultSubscriber.awaitTerminalEvent();
        updateResultSubscriber.assertNoErrors();
        updateResultSubscriber.assertCompleted();

        queryUtils.assertEncounterRow(queryUtils.fetchEncounter("E1"), "E1", "P1", "e1 content", "V");
        queryUtils.assertEncounterRow(queryUtils.fetchEncounter("E2"), "E2", "P1", "e2 content", "V");

    }

    @Test
    public void shouldAddEntriesToCatchmentFeedForAddressChange() throws Exception {
        Address address = new Address();
        address.setDivisionId("40");
        address.setDistrictId("36");
        address.setUpazilaId("18");
        address.setCityCorporationId("60");
        address.setUnionOrUrbanWardId("45");

        int year = new DateTime().getYear();
        Date jul18 = new DateTime(year, 7, 18, 0, 0).toDate();
        Date jul19 = new DateTime(year, 7, 19, 0, 0).toDate();
        Date jul20 = new DateTime(year, 7, 20, 0, 0).toDate();

        queryUtils.insertEncounter("E1", "P1", jul18, "e1 content", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncounter("E2", "P1", jul19, "e2 content", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncounter("E3", "P1", jul20, "e3 content", queryBuilder.getEncounterContentColumnName());

        queryUtils.insertEncByPatient("E1", "P1", jul18);
        queryUtils.insertEncByPatient("E2", "P1", jul19);
        queryUtils.insertEncByPatient("E3", "P1", jul20);

        queryUtils.insertEncounterByCatchment("E1", "20", "2015", "201502", jul18);
        queryUtils.insertEncounterByCatchment("E2", "20", "2015", "201502", jul19);
        queryUtils.insertEncounterByCatchment("E3", "20", "2015", "201502", jul20);

        assertThat(queryUtils.fetchCatchmentFeed("20", "2015", year).size(), is(3));
        assertThat(queryUtils.fetchCatchmentFeed("40", "4036", year).size(), is(0));

        PatientUpdate patientUpdate = new PatientUpdate();
        patientUpdate.setChangeSetMap(addressChange(address));
        patientUpdate.setHealthId("P1");

        Observable<Boolean> updateObservable = encounterRepository.applyUpdate(patientUpdate);
        TestSubscriber<Boolean> updateResponseSubscriber = new TestSubscriber<>();
        updateObservable.subscribe(updateResponseSubscriber);

        updateResponseSubscriber.awaitTerminalEvent();
        updateResponseSubscriber.assertTerminalEvent();
        updateResponseSubscriber.assertNoErrors();
        updateResponseSubscriber.assertCompleted();
        assertTrue(updateResponseSubscriber.getOnNextEvents().get(0));

        assertThat(queryUtils.fetchCatchmentFeed("20", "2015", year).size(), is(3));
        List<Row> rows = queryUtils.fetchCatchmentFeed("40", "4036", year);
        assertEquals(3, rows.size());
        assertThat(rows.get(0).getString("upazila_id"), is("403618"));
        assertThat(rows.get(0).getString("city_corporation_id"), is("40361860"));
        assertThat(rows.get(0).getString("union_urban_ward_id"), is("4036186045"));
        assertThat(rows.get(0).getString("encounter_id"), is("E1"));
        assertThat(rows.get(1).getString("encounter_id"), is("E2"));
        assertThat(rows.get(2).getString("encounter_id"), is("E3"));

    }


    @After
    public void tearDown() {
        queryUtils.trucateAllTables();
        DateTimeUtils.setCurrentMillisSystem();
    }
}
