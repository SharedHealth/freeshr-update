package org.sharedhealth.freeshrUpdate.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShrUpdateAction extends rx.Subscriber<Long> {
    private static final Logger LOG = LoggerFactory.getLogger(ShrUpdateAction.class);

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
        LOG.info(String.format("starting iteration number: %s", aLong));
    }
}
