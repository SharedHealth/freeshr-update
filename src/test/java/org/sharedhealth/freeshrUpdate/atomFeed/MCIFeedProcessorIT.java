package org.sharedhealth.freeshrUpdate.atomFeed;

import com.datastax.driver.core.Row;
import org.joda.time.DateTime;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.client.MciWebClient;
import org.sharedhealth.freeshrUpdate.config.SHREnvironmentMock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfig;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.sharedhealth.freeshrUpdate.domain.EncounterBundle;
import org.sharedhealth.freeshrUpdate.domain.Patient;
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
import rx.observers.TestSubscriber;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.*;


@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("/test.properties")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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

    private static QueryUtils queryUtils;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        queryUtils = new QueryUtils(cqlOperations);
        mciFeedProcessor = new MciFeedProcessor(transactionManager, mciWebClient, patientUpdateEventWorker, shrUpdateConfiguration);

    }

    @Test
    public void shouldDownloadRetainedPatientAndDoEncounterMerge() throws Exception {
        String p1 = "Patient1";
        String p2 = "Patient2";
        String e2 = "Encounter2";
        String e1 = "Encounter1";
        queryUtils.insertPatient(p1);
        queryUtils.insertEncounter(e1, p1, new DateTime(2015, 11, 25, 0, 0, 0).toDate(), e1 + " for " + p1, queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncByPatient(e1, p1, new DateTime(2015, 11, 25, 0, 0, 0).toDate());
        queryUtils.insertEncounter(e2, p1, new DateTime(2015, 11, 26, 0, 0, 0).toDate(), e2 + " for " + p1, queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncByPatient(e2, p1, new DateTime(2015, 11, 26, 0, 0, 0).toDate());
        List<String> list = encounterRepository.getEncounterIdsForPatient(p1).toBlocking().first();
        assertEquals(2, list.size());

        when(mciWebClient.getFeed(properties.getMciPatientUpdateFeedUrl())).thenReturn(FileUtil.asString("feeds/mergeFeed2.xml"));
        when(mciWebClient.getFeed(new URI("http://127.0.0.1:8081/api/v1/feed/patients?last_marker=end"))).thenReturn(FileUtil.asString("feeds/emptyFeed.xml"));
        String toBeMergedWithPatient = FileUtil.asString("patients/Patient2.json");
        when(mciWebClient.getPatient(p2)).thenReturn(toBeMergedWithPatient);

        mciFeedProcessor.pullLatest();

        Patient patientP1 = new Patient();
        patientP1.setHealthId(p1);
        patientP1.setActive(false);
        patientP1.setMergedWith(p2);

        assertPatient(patientP1, queryUtils.fetchPatient(p1));

        Patient patientP2 = new Patient();
        patientP2.setHealthId(p2);
        patientP2.setActive(true);

        assertPatient(patientP2, queryUtils.fetchPatient(p2));

        TestSubscriber<EncounterBundle> encounterBundleSubscriber = new TestSubscriber<>();
        Observable<EncounterBundle> encounterObservable = encounterRepository.getEncounterBundles(p2);
        encounterObservable.subscribe(encounterBundleSubscriber);

        encounterBundleSubscriber.awaitTerminalEvent();
        encounterBundleSubscriber.assertNoErrors();
        encounterBundleSubscriber.assertCompleted();

        List<EncounterBundle> encountersForP2 = encounterBundleSubscriber.getOnNextEvents();
        assertEquals(2, encountersForP2.size());
        queryUtils.assertEncounter(encountersForP2.get(0), e1, p2, e1 + " for " + p2);
    }

    @Test
    public void shouldMergeWithExistingPatient() throws Exception {
        String p1 = "P1";
        queryUtils.insertPatient(p1);
        String e1 = "Encounter3";
        queryUtils.insertEncounter(e1, p1, new DateTime(2015, 11, 25, 0, 0, 0).toDate(), e1 + " for " + p1, queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncByPatient(e1, p1, new DateTime(2015, 11, 25, 0, 0, 0).toDate());
        String p2 = "P2";
        queryUtils.insertPatient(p2);
        when(mciWebClient.getFeed(properties.getMciPatientUpdateFeedUrl())).thenReturn(FileUtil.asString("feeds/mergeFeed.xml"));
        when(mciWebClient.getFeed(new URI("http://127.0.0.1:8081/api/v1/feed/patients?last_marker=end"))).thenReturn(FileUtil.asString("feeds/emptyFeed.xml"));

        mciFeedProcessor.pullLatest();

        Patient patientP1 = new Patient();
        patientP1.setHealthId(p1);
        patientP1.setActive(false);
        patientP1.setMergedWith(p2);
        assertPatient(patientP1, queryUtils.fetchPatient(p1));

        Patient patientP2 = new Patient();
        patientP2.setHealthId(p2);
        patientP2.setActive(true);

        assertPatient(patientP2, queryUtils.fetchPatient(p2));

        TestSubscriber<EncounterBundle> encounterBundleSubscriber = new TestSubscriber<>();
        Observable<EncounterBundle> encounterObservable = encounterRepository.getEncounterBundles(p2);
        encounterObservable.subscribe(encounterBundleSubscriber);

        encounterBundleSubscriber.awaitTerminalEvent();
        encounterBundleSubscriber.assertNoErrors();
        encounterBundleSubscriber.assertCompleted();

        List<EncounterBundle> encountersForP2 = encounterBundleSubscriber.getOnNextEvents();

        assertThat(encountersForP2.size(), is(1));
        queryUtils.assertEncounter(encountersForP2.get(0), e1, p2, e1 + " for " + p2);
    }

    @Test
    public void shouldWriteFailedEventsWithEncTracking() throws Exception {
        //create a patinet  (Henry) with some encounters
        String p1 = "Henry";
        queryUtils.insertPatient(p1);
        String e1 = "E1Henry";
        queryUtils.insertEncounter(e1, p1, new DateTime(2015, 11, 25, 0, 0, 0).toDate(), "E1 for P1", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncByPatient(e1, p1, new DateTime(2015, 11, 25, 0, 0, 0).toDate());
        String e2 = "E2Henry";
        queryUtils.insertEncounter(e2, p1, new DateTime(2015, 11, 26, 0, 0, 0).toDate(), "E2 for P1", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncByPatient(e2, p1, new DateTime(2015, 11, 26, 0, 0, 0).toDate());

        when(mciWebClient.getFeed(properties.getMciPatientUpdateFeedUrl())).thenReturn(FileUtil.asString("feeds/mciPatientFeedWithMergeAndUpdateEvents.xml"));
        when(mciWebClient.getFeed(new URI("http://127.0.0.1:8081/api/v1/feed/patients?last_marker=end"))).thenReturn(FileUtil.asString("feeds/emptyFeed.xml"));


        mciFeedProcessor.pullLatest();

        assertEquals("Should have a failed event as the active patient was not found", 1, mciFeedProcessor.getNumberOfFailedEvents());

        String hendrix_id = FileUtil.asString("patients/Patient_Hendrix.json");
        when(mciWebClient.getPatient("Hendrix")).thenReturn(hendrix_id);
        mciFeedProcessor.pullFailedEvents();

        assertEquals("Should have processed failed event", 0, mciFeedProcessor.getNumberOfFailedEvents());
    }


    private void assertPatient(Patient patient, Row row) {
        assertEquals(patient.getMergedWith(), row.getString(MERGED_WITH_COLUMN_NAME));
        assertEquals(patient.isActive(), row.getBool(ACTIVE_COLUMN_NAME));
        assertEquals(patient.getHealthId(), row.getString(HEALTH_ID_COLUMN_NAME));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        Files.deleteIfExists(FileSystems.getDefault().getPath("freeSHRUpdate.mv.db"));
        Files.deleteIfExists(FileSystems.getDefault().getPath("freeSHRUpdate.trace.db"));
        queryUtils.trucateAllTables();
    }
}