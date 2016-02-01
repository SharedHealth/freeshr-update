package org.sharedhealth.freeshrUpdate.atomFeed;

import com.datastax.driver.core.Row;
import com.google.common.collect.Lists;
import org.ict4h.atomfeed.client.repository.AllFailedEvents;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.client.MciWebClient;
import org.sharedhealth.freeshrUpdate.config.SHREnvironmentMock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfig;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.sharedhealth.freeshrUpdate.domain.EncounterBundle;
import org.sharedhealth.freeshrUpdate.domain.Patient;
import org.sharedhealth.freeshrUpdate.eventWorker.EncounterMovementTracker;
import org.sharedhealth.freeshrUpdate.eventWorker.PatientUpdateEventWorker;
import org.sharedhealth.freeshrUpdate.repository.EncounterRepository;
import org.sharedhealth.freeshrUpdate.repository.SHRQueryBuilder;
import org.sharedhealth.freeshrUpdate.utils.FileUtil;
import org.sharedhealth.freeshrUpdate.utils.QueryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.*;


@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("/test.properties")
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = ShrUpdateConfig.class)
public class MCIFeedProcessorIT {
    @Mock
    private MciWebClient mciWebClient;

    @InjectMocks
    @Autowired
    PatientUpdateEventWorker patientUpdateEventWorker;

    @Autowired
    ShrUpdateConfiguration shrUpdateConfiguration;

    @Autowired
    AtomFeedSpringTransactionManager transactionManager;

    private MciFeedProcessor mciFeedProcessor;

    @Autowired
    private ShrUpdateConfiguration properties;

    @Autowired
    @Qualifier("SHRCassandraTemplate")
    CqlOperations cqlOperations;

    @Autowired
    EncounterRepository encounterRepository;

    @Autowired
    SHRQueryBuilder queryBuilder;

    @Autowired
    EncounterMovementTracker tracker;

    private QueryUtils queryUtils;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        queryUtils = new QueryUtils(cqlOperations);
        mciFeedProcessor =  new MciFeedProcessor(transactionManager, mciWebClient,patientUpdateEventWorker,shrUpdateConfiguration);
    }

    @Test
    @Ignore
    public void shouldDownloadRetainedPatientAndDoEncounterMerge() throws Exception {
        queryUtils.insertPatient("P1");
        queryUtils.insertEncounter("E1", "P1", new DateTime(2015, 11, 25, 0, 0, 0).toDate(), "E1 for P1", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncByPatient("E1", "P1", new DateTime(2015, 11, 25, 0, 0, 0).toDate());
        queryUtils.insertEncounter("E2", "P1", new DateTime(2015, 11, 26, 0, 0, 0).toDate(), "E2 for P1", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncByPatient("E2", "P1", new DateTime(2015, 11, 26, 0, 0, 0).toDate());
        List<String> list = encounterRepository.getEncounterIdsForPatient("P1").toBlocking().first();
        assertEquals(2, list.size());

        when(mciWebClient.getFeed(properties.getMciPatientUpdateFeedUrl())).thenReturn(FileUtil.asString("feeds/mergeFeed.xml"));
        when(mciWebClient.getFeed(new URI("http://127.0.0.1:8081/api/v1/feed/patients?last_marker=end"))).thenReturn(FileUtil.asString("feeds/emptyFeed.xml"));
        String toBeMergedWithPatient = FileUtil.asString("patients/P2.json");
        when(mciWebClient.getPatient("P2")).thenReturn(toBeMergedWithPatient);

        final Row p1Patient = queryUtils.fetchPatient("P1");
        System.out.println("healthId:" +p1Patient.getString(HEALTH_ID_COLUMN_NAME));
        System.out.println("merged with:" + p1Patient.getString(MERGED_WITH_COLUMN_NAME));
        System.out.println("active?:" + p1Patient.getBool(ACTIVE_COLUMN_NAME));


        TestSubscriber<String> subscriber = new TestSubscriber<>();
        Observable<String> testPullObservable = mciFeedProcessor.pullLatestForTest();
        testPullObservable.subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();
        subscriber.assertCompleted();

        Patient patientP1 = new Patient();
        patientP1.setHealthId("P1");
        patientP1.setActive(false);
        patientP1.setMergedWith("P2");

        assertPatient(patientP1, queryUtils.fetchPatient("P1"));

        Patient patientP2 = new Patient();
        patientP2.setHealthId("P2");
        patientP2.setActive(true);

        assertPatient(patientP2, queryUtils.fetchPatient("P2"));

        TestSubscriber<EncounterBundle> encounterBundleSubscriber = new TestSubscriber<>();
        Observable<EncounterBundle> encounterObservable = encounterRepository.getEncounterBundles("P2");
        encounterObservable.subscribe(encounterBundleSubscriber);

        encounterBundleSubscriber.awaitTerminalEvent();
        encounterBundleSubscriber.assertNoErrors();
        encounterBundleSubscriber.assertCompleted();

        List<EncounterBundle> encountersForP2 = encounterBundleSubscriber.getOnNextEvents();
        assertEquals(2, encountersForP2.size());
        queryUtils.assertEncounter(encountersForP2.get(0), "E1", "P2", "E1 for P2");
    }

    @Test
    //Please donot delete this test.This runs individually, but fails on running the entire test file
    @Ignore
    public void shouldMergeWithExistingPatient() throws Exception {
        queryUtils.insertPatient("P1");
        queryUtils.insertEncounter("E1", "P1", new DateTime(2015, 11, 25, 0, 0, 0).toDate(), "E1 for P1", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncByPatient("E1", "P1", new DateTime(2015, 11, 25, 0, 0, 0).toDate());
        queryUtils.insertPatient("P2");
        when(mciWebClient.getFeed(properties.getMciPatientUpdateFeedUrl())).thenReturn(FileUtil.asString("feeds/mergeFeed.xml"));
        when(mciWebClient.getFeed(new URI("http://127.0.0.1:8081/api/v1/feed/patients?last_marker=end"))).thenReturn(FileUtil.asString("feeds/emptyFeed.xml"));

        mciFeedProcessor.pullLatest();

        Patient patientP1 = new Patient();
        patientP1.setHealthId("P1");
        patientP1.setActive(false);
        patientP1.setMergedWith("P2");
        assertPatient(patientP1, queryUtils.fetchPatient("P1"));

        Patient patientP2 = new Patient();
        patientP2.setHealthId("P2");
        patientP2.setActive(true);

        assertPatient(patientP2, queryUtils.fetchPatient("P2"));

        TestSubscriber<EncounterBundle> encounterBundleSubscriber = new TestSubscriber<>();
        Observable<EncounterBundle> encounterObservable = encounterRepository.getEncounterBundles("P2");
        encounterObservable.subscribe(encounterBundleSubscriber);

        encounterBundleSubscriber.awaitTerminalEvent();
        encounterBundleSubscriber.assertNoErrors();
        encounterBundleSubscriber.assertCompleted();

        List<EncounterBundle> encountersForP2 = encounterBundleSubscriber.getOnNextEvents();

        assertThat(encountersForP2.size(), is(1));
        queryUtils.assertEncounter(encountersForP2.get(0), "E1", "P2", "E1 for P2");
    }

    @Test
    public void shouldWriteFailedEventsWithEncTracking() throws Exception {
        //create a patinet  (Henry) with some encounters
        queryUtils.insertPatient("Henry");
        queryUtils.insertEncounter("E1Henry", "Henry", new DateTime(2015, 11, 25, 0, 0, 0).toDate(), "E1 for P1", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncByPatient("E1Henry", "Henry", new DateTime(2015, 11, 25, 0, 0, 0).toDate());
        queryUtils.insertEncounter("E2Henry", "Henry", new DateTime(2015, 11, 26, 0, 0, 0).toDate(), "E2 for P1", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncByPatient("E2Henry", "Henry", new DateTime(2015, 11, 26, 0, 0, 0).toDate());

        when(mciWebClient.getFeed(properties.getMciPatientUpdateFeedUrl())).thenReturn(FileUtil.asString("feeds/mciPatientFeedWithMergeAndUpdateEvents.xml"));
        when(mciWebClient.getFeed(new URI("http://127.0.0.1:8081/api/v1/feed/patients?last_marker=end"))).thenReturn(FileUtil.asString("feeds/emptyFeed.xml"));
//        String hendrix_id = FileUtil.asString("patients/Patient_Hendrix.json");
//        when(mciWebClient.getPatient("Hendrix")).thenReturn(hendrix_id);

        final Row rowHenry = queryUtils.fetchPatient("Henry");
        debugPatientRow(rowHenry, "Henry");

        mciFeedProcessor.pullLatest();

        assertEquals("Should have a failed event as the active patient was not found", 1, mciFeedProcessor.getNumberOfFailedEvents());

        String hendrix_id = FileUtil.asString("patients/Patient_Hendrix.json");
        when(mciWebClient.getPatient("Hendrix")).thenReturn(hendrix_id);
        mciFeedProcessor.pullFailedEvents();

        assertEquals("Should have processed failed event", 0, mciFeedProcessor.getNumberOfFailedEvents());
    }

    private void debugPatientRow(Row rowHenry, String patientName) {
        System.out.println("******** Details of " + patientName + " ********");
        System.out.println("healthId:" + rowHenry.getString(HEALTH_ID_COLUMN_NAME));
        System.out.println("merged with:" + rowHenry.getString(MERGED_WITH_COLUMN_NAME));
        System.out.println("active:" + rowHenry.getBool(ACTIVE_COLUMN_NAME));
        System.out.println("******** Details of " + patientName + " ********");
    }

    private void assertPatient(Patient patient, Row row) {
        System.out.println("healthId:" +row.getString(HEALTH_ID_COLUMN_NAME));
        System.out.println("merged with:" + row.getString(MERGED_WITH_COLUMN_NAME));
        System.out.println("active?:" + row.getBool(ACTIVE_COLUMN_NAME));
        assertEquals(patient.getMergedWith(), row.getString(MERGED_WITH_COLUMN_NAME));
        assertEquals(patient.isActive(), row.getBool(ACTIVE_COLUMN_NAME));
        assertEquals(patient.getHealthId(), row.getString(HEALTH_ID_COLUMN_NAME));

    }

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(FileSystems.getDefault().getPath("freeSHRUpdate.mv.db"));
        Files.deleteIfExists(FileSystems.getDefault().getPath("freeSHRUpdate.trace.db"));
        queryUtils.trucateAllTables();
    }
}