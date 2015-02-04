package org.sharedhealth.freeshrUpdate.schedule;


import org.sharedhealth.freeshrUpdate.config.SHRUpdateConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Component
public class Update {
    private static final Logger LOG = LoggerFactory.getLogger(Update.class);
    @Autowired
    SHRUpdateConfig config;

    public void start() {
        Observable.interval(config.getUpdateIntervalInSeconds(), TimeUnit.SECONDS,
                Schedulers.immediate()).subscribe(new ShrUpdateAction());


    }
}
