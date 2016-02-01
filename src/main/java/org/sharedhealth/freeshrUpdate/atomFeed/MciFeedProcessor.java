package org.sharedhealth.freeshrUpdate.atomFeed;

import org.ict4h.atomfeed.client.AtomFeedProperties;
import org.ict4h.atomfeed.client.repository.AllFailedEvents;
import org.ict4h.atomfeed.client.repository.AllMarkers;
import org.ict4h.atomfeed.client.repository.jdbc.AllFailedEventsJdbcImpl;
import org.ict4h.atomfeed.client.repository.jdbc.AllMarkersJdbcImpl;
import org.ict4h.atomfeed.client.service.AtomFeedClient;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.sharedhealth.freeshrUpdate.client.MciWebClient;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.sharedhealth.freeshrUpdate.eventWorker.PatientUpdateEventWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.Observable;

import java.net.URI;

@Component
public class MciFeedProcessor {
    private final AllMarkers markers;
    private final AllFailedEvents failedEvents;
    private final AtomFeedSpringTransactionManager transactionManager;
    private MciWebClient mciWebClient;
    private PatientUpdateEventWorker patientUpdateEventWorker;
    private ShrUpdateConfiguration shrUpdateConfiguration;


    @Autowired
    public MciFeedProcessor(AtomFeedSpringTransactionManager transactionManager,
                            MciWebClient mciWebClient,
                            PatientUpdateEventWorker patientUpdateEventWorker,
                            ShrUpdateConfiguration shrUpdateConfiguration) {
        this.shrUpdateConfiguration = shrUpdateConfiguration;
        this.markers = new AllMarkersJdbcImpl(transactionManager);
        this.failedEvents = new AllFailedEventsJdbcImpl(transactionManager);
        this.transactionManager = transactionManager;
        this.mciWebClient = mciWebClient;
        this.patientUpdateEventWorker = patientUpdateEventWorker;
    }

    public void pullLatest() {
        AtomFeedProperties atomProperties = new AtomFeedProperties();
        atomProperties.setMaxFailedEvents(20);
        AtomFeedClient atomFeedClient = atomFeedClient(this.shrUpdateConfiguration.getMciPatientUpdateFeedUrl(),
                patientUpdateEventWorker,
                atomProperties);
        atomFeedClient.processEvents();
    }

    public Observable<String> pullLatestForTest(){
        pullLatest();
        return Observable.just("Testing");
    }

    public void pullFailedEvents() {
        AtomFeedProperties atomProperties = new AtomFeedProperties();
        atomProperties.setMaxFailedEvents(20);
        AtomFeedClient atomFeedClient = atomFeedClient(this.shrUpdateConfiguration.getMciPatientUpdateFeedUrl(),
                patientUpdateEventWorker,
                atomProperties);
        atomFeedClient.processFailedEvents();
    }

    private AtomFeedClient atomFeedClient(URI feedUri, EventWorker worker, AtomFeedProperties atomProperties) {
        return new AtomFeedClient(
                new MciPatientUpdateFeeds(mciWebClient),
                markers,
                failedEvents,
                atomProperties,
                transactionManager,
                feedUri,
                worker);
    }

    public int getNumberOfFailedEvents() {
        return failedEvents.getNumberOfFailedEvents(this.shrUpdateConfiguration.getMciPatientUpdateFeedUrl().toString());
    }
}
