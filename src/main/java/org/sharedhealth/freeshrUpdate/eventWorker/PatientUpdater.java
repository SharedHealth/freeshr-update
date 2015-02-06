package org.sharedhealth.freeshrUpdate.eventWorker;

import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.springframework.stereotype.Component;

@Component
public class PatientUpdater implements EventWorker {
    @Override
    public void process(Event event) {

    }

    @Override
    public void cleanUp(Event event) {

    }
}
