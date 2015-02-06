package org.sharedhealth.freeshrUpdate.schedule;


import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;

@Component
public class Update {
    @Autowired
    ShrUpdateConfiguration config;

    public void start() {
        Observable.interval(config.getUpdateIntervalInSeconds(), TimeUnit.SECONDS,
                Schedulers.immediate())
                .startWith(-1L) // to start action immediately
                .subscribe(new ShrUpdateAction());


    }
}
