package org.sharedhealth.freeshrUpdate.schedule;

import org.sharedhealth.freeshrUpdate.atomFeed.MciFeedProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShrRetryUpdateAction extends rx.Subscriber<Long> {

    private final MciFeedProcessor mciFeedProcessor;

    public ShrRetryUpdateAction(MciFeedProcessor mciFeedProcessor) {
        this.mciFeedProcessor = mciFeedProcessor;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ShrRetryUpdateAction.class);

    @Override
    public void onCompleted() {
        LOG.info("bye bye!");
    }

    @Override
    public void onError(Throwable e) {
        LOG.error(e.getMessage());
    }

    @Override
    public void onNext(Long aLong) {
        LOG.debug(String.format("starting iteration number: %s", aLong));
        try {
            mciFeedProcessor.pullFailedEvents();
        } catch (Exception e) {
            LOG.error(e.getMessage());;
        }
    }
}
