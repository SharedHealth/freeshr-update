package org.sharedhealth.freeshrUpdate.atomFeed;

import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.repository.jdbc.AllFailedEventsJdbcImpl;
import org.ict4h.atomfeed.client.repository.jdbc.AllMarkersJdbcImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.client.MciWebClient;
import org.sharedhealth.freeshrUpdate.config.AtomClientDatabaseConfig;
import org.sharedhealth.freeshrUpdate.config.SHRCassandraConfig;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.sharedhealth.freeshrUpdate.eventWorker.PatientUpdateEventWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.HashMap;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.freeshrUpdate.helpers.ResourceHelper.asString;


@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("/test.properties")
@ContextConfiguration(classes = {ShrUpdateConfiguration.class, AtomClientDatabaseConfig.class})
@ComponentScan(basePackages = {"org.sharedhealth.freeshrUpdate"})
public class MciFeedProcessorTest {
    @Mock
    private MciWebClient mciWebClient;
    @Mock
    private PatientUpdateEventWorker patientUpdateEventWorker;

    @Autowired
    DataSourceTransactionManager txMgr;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldProcessFeedsFirstTime() throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        URI feedUri = URI.create("foo");
        URI nextUri = URI.create(
                "http://127.0.0.1:9997/api/v1/feed/patients?last_marker=32782f90-a843-11e4-ad63-6d5f88e0f020");
        when(mciWebClient.get(feedUri)).thenReturn(asString("feeds/patientUpdatesFeed.xml"));
        when(mciWebClient.get(nextUri)).thenReturn(asString("feeds/emptyFeed.xml"));

        AtomFeedSpringTransactionManager transactionManager = new AtomFeedSpringTransactionManager(txMgr);

        MciFeedProcessor mciFeedProcessor = new MciFeedProcessor(feedUri,
                new AllMarkersJdbcImpl(transactionManager),
                new AllFailedEventsJdbcImpl(transactionManager)
                , transactionManager,
                mciWebClient, patientUpdateEventWorker);
        mciFeedProcessor.pullLatest();

        verify(mciWebClient, times(1)).get(feedUri);
        verify(mciWebClient, times(1)).get(nextUri);
        verify(patientUpdateEventWorker, times(25)).process(any(Event.class));
    }

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(FileSystems.getDefault().getPath("freeSHRUpdate.mv.db"));
        Files.deleteIfExists(FileSystems.getDefault().getPath("freeSHRUpdate.trace.db"));
    }
}