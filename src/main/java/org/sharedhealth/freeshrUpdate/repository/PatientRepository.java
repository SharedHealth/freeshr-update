package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.ResultSet;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.stereotype.Component;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

@Component
public class PatientRepository {
    @Autowired
    @Qualifier("SHRCassandraTemplate")
    private CqlOperations cqlOperations;
    private static final Logger LOG = LoggerFactory.getLogger(PatientRepository.class);

    @Autowired
    private SHRQueryBuilder shrQueryBuilder;

    public PatientRepository() {
    }

    public PatientRepository(CqlOperations cqlOperations, SHRQueryBuilder shrQueryBuilder) {
        this.cqlOperations = cqlOperations;
        this.shrQueryBuilder = shrQueryBuilder;
    }

    public Observable<Boolean> applyUpdate(final PatientUpdate patientUpdate) {
        if (!patientUpdate.hasChanges()) return Observable.just(false);
        final String healthId = patientUpdate.getHealthId();
        return findPatient(healthId).flatMap(new Func1<Boolean, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Boolean patientExists) {
                LOG.debug(String.format("Patient %s %s found", healthId, patientExists ? "" : "not"));
                return patientExists ? savePatientUpdate(patientUpdate) : Observable.just(false);
            }
        }, onError(), onCompletion());
    }


    private Observable<Boolean> findPatient(final String healthId) {
        Observable<ResultSet> observable = Observable.from(
                cqlOperations.queryAsynchronously(shrQueryBuilder.findPatientQuery(healthId))
        );
        return observable.flatMap(new Func1<ResultSet, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(ResultSet rows) {
                return Observable.just(rows.one().getLong("count") > 0);
            }
        }, onError(), onCompletion()).firstOrDefault(false);
    }


    private Observable<Boolean> savePatientUpdate(PatientUpdate patientUpdate) {
        Observable<ResultSet> observable = Observable.from(
                cqlOperations.executeAsynchronously(shrQueryBuilder.updatePatientQuery(patientUpdate))).first();

        return observable.flatMap(new Func1<ResultSet, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(ResultSet rows) {
                return Observable.just(true);
            }
        }, onError(), onCompletion()).firstOrDefault(false);
    }


    private Func0<Observable<? extends Boolean>> onCompletion() {
        return new Func0<Observable<? extends Boolean>>() {
            @Override
            public Observable<? extends Boolean> call() {
                return Observable.just(false);
            }
        };
    }

    private Func1<Throwable, Observable<? extends Boolean>> onError() {
        return new Func1<Throwable, Observable<? extends Boolean>>() {
            @Override
            public Observable<? extends Boolean> call(Throwable throwable) {
                LOG.error(throwable.getMessage());
                return Observable.error(throwable);
            }
        };
    }

}
