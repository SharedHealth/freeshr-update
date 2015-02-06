package org.sharedhealth.freeshrUpdate.atomFeed;

import org.ict4h.atomfeed.client.AtomFeedProperties;
import org.ict4h.atomfeed.client.repository.AllFailedEvents;
import org.ict4h.atomfeed.client.repository.AllMarkers;
import org.ict4h.atomfeed.client.service.AtomFeedClient;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.sharedhealth.freeshrUpdate.client.MciWebClient;
import org.sharedhealth.freeshrUpdate.eventWorker.PatientUpdater;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;

public class MciFeedProcessor {
    private final URI feedUrl;
    private final AllMarkers markers;
    private final AllFailedEvents failedEvents;
    private final AtomFeedSpringTransactionManager transactionManager;

    @Autowired
    private final MciWebClient mciWebClient;
    @Autowired
    private final PatientUpdater patientUpdater;


    public MciFeedProcessor(URI feedUrl,
                            AllMarkers markers,
                            AllFailedEvents failedEvents,
                            AtomFeedSpringTransactionManager transactionManager,
                            MciWebClient mciWebClient,
                            PatientUpdater patientUpdater) {
        this.feedUrl = feedUrl;
        this.markers = markers;
        this.failedEvents = failedEvents;
        this.transactionManager = transactionManager;
        this.mciWebClient = mciWebClient;
        this.patientUpdater = patientUpdater;
    }

    public void pullLatest() {
        AtomFeedProperties atomProperties = new AtomFeedProperties();
        atomProperties.setMaxFailedEvents(20);
        AtomFeedClient atomFeedClient = atomFeedClient(this.feedUrl,
                patientUpdater,
                atomProperties);
        atomFeedClient.processEvents();
    }

    private AtomFeedClient atomFeedClient(URI feedUri, EventWorker worker, AtomFeedProperties atomProperties) {
        return new AtomFeedClient(
                new MciFeeds(mciWebClient),
                markers,
                failedEvents,
                atomProperties,
                transactionManager,
                feedUri,
                worker);
    }
}
