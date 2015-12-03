package org.sharedhealth.freeshrUpdate.atomFeed;

import com.datastax.driver.core.Row;
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
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = ShrUpdateConfig.class)
public class MCIFeedProcessorIT {
    @Mock
    private MciWebClient mciWebClient;

    @InjectMocks
    @Autowired
    PatientUpdateEventWorker patientUpdateEventWorker;

    @Autowired
    @InjectMocks
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

    private QueryUtils queryUtils;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        queryUtils = new QueryUtils(cqlOperations);
    }

    @Test
    public void shouldDownloadToBeMergedWithPatientAndDoEncounterMerge() throws Exception {
        queryUtils.insertPatient("P1");
        queryUtils.insertEncounter("E1", "P1", new DateTime(2015, 11, 25, 0, 0, 0).toDate(), "E1 for P1", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncByPatient("E1", "P1", new DateTime(2015, 11, 25, 0, 0, 0).toDate());

        when(mciWebClient.getFeed(properties.getMciPatientUpdateFeedUrl())).thenReturn(FileUtil.asString("feeds/mergeFeed.xml"));
        when(mciWebClient.getFeed(new URI("http://127.0.0.1:8081/api/v1/feed/patients?last_marker=end"))).thenReturn(FileUtil.asString("feeds/emptyFeed.xml"));
        when(mciWebClient.getPatient("P2")).thenReturn(FileUtil.asString("patients/P2.json"));

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
        assertThat(encountersForP2.size(), is(1));
        queryUtils.assertEncounter(encountersForP2.get(0), "E1", "P2", "E1 for P2");
    }

    @Test
    @Ignore
    //Please donot delete this test.This runs individually, but fails on running the entire test file
    public void shouldMergedWithExistingPatient() throws Exception {
        queryUtils.insertPatient("P1");
        queryUtils.insertEncounter("E1", "P1", new DateTime(2015, 11, 25, 0, 0, 0).toDate(), "E1 for P1", queryBuilder.getEncounterContentColumnName());
        queryUtils.insertEncByPatient("E1", "P1", new DateTime(2015, 11, 25, 0, 0, 0).toDate());
        queryUtils.insertPatient("P2");
        when(mciWebClient.getFeed(properties.getMciPatientUpdateFeedUrl())).thenReturn(FileUtil.asString("feeds/mergeFeed.xml"));
        when(mciWebClient.getFeed(new URI("http://127.0.0.1:8081/api/v1/feed/patients?last_marker=end"))).thenReturn(FileUtil.asString("feeds/emptyFeed.xml"));

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

        assertThat(encountersForP2.size(), is(1));
        queryUtils.assertEncounter(encountersForP2.get(0), "E1", "P2", "E1 for P2");
    }

    private void assertPatient(Patient patient, Row row) {
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