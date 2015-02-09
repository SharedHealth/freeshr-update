package org.sharedhealth.freeshrUpdate.schedule;


import org.ict4h.atomfeed.client.repository.jdbc.AllFailedEventsJdbcImpl;
import org.ict4h.atomfeed.client.repository.jdbc.AllMarkersJdbcImpl;
import org.sharedhealth.freeshrUpdate.atomFeed.AtomFeedSpringTransactionManager;
import org.sharedhealth.freeshrUpdate.atomFeed.MciFeedProcessor;
import org.sharedhealth.freeshrUpdate.client.MciWebClient;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.sharedhealth.freeshrUpdate.eventWorker.PatientUpdateEventWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;

@Component
public class Scheduler {
    @Autowired
    ShrUpdateConfiguration config;
    @Autowired
    DataSourceTransactionManager platformTransactionManager;
    @Autowired
    PatientUpdateEventWorker patientUpdateEventWorker;
    @Autowired
    MciWebClient mciWebClient;

    public Scheduler() {
    }

    public Scheduler(ShrUpdateConfiguration config, DataSourceTransactionManager platformTransactionManager,
                     PatientUpdateEventWorker patientUpdateEventWorker, MciWebClient mciWebClient) {

        this.config = config;
        this.platformTransactionManager = platformTransactionManager;
        this.patientUpdateEventWorker = patientUpdateEventWorker;
        this.mciWebClient = mciWebClient;
    }

    public void start() {
        AtomFeedSpringTransactionManager transactionManager = new AtomFeedSpringTransactionManager
                (platformTransactionManager);

        MciFeedProcessor mciFeedProcessor = new MciFeedProcessor(config.getMciBaseUrl(),
                new AllMarkersJdbcImpl(transactionManager),
                new AllFailedEventsJdbcImpl(transactionManager),
                transactionManager,
                mciWebClient,
                patientUpdateEventWorker);

        Observable.interval(config.getUpdateIntervalInSeconds(), TimeUnit.SECONDS,
                Schedulers.immediate())
                .startWith(-1L) // to start action immediately
                .subscribe(new ShrUpdateAction(mciFeedProcessor));
    }
}
