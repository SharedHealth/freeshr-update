package org.sharedhealth.freeshrUpdate.job;

import org.sharedhealth.freeshrUpdate.atomFeed.MciFeedProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ShrRetryUpdateJob {

    private final MciFeedProcessor mciFeedProcessor;

    @Autowired
    public ShrRetryUpdateJob(MciFeedProcessor mciFeedProcessor) {
        this.mciFeedProcessor = mciFeedProcessor;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ShrRetryUpdateJob.class);

    @Scheduled(fixedDelayString = "${PATIENT_UPDATE_RETRY_INTERVAL}", initialDelay = 10000)
    public void start() {
        try {
            mciFeedProcessor.pullFailedEvents();
        } catch (Exception e) {
            LOG.error(e.getMessage());;
        }
    }
}
