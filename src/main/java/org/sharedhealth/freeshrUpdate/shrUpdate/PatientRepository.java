package org.sharedhealth.freeshrUpdate.shrUpdate;

import com.datastax.driver.core.ResultSet;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
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

    @Autowired
    private PatientQueryBuilder patientQueryBuilder;

    public PatientRepository(CqlOperations cqlOperations, PatientQueryBuilder patientQueryBuilder) {
        this.cqlOperations = cqlOperations;
        this.patientQueryBuilder = patientQueryBuilder;
    }

    private Observable<Boolean> findPatient(String healthId) {
        Observable<ResultSet> observable = Observable.from(
                cqlOperations.queryAsynchronously(patientQueryBuilder.findPatientQuery(healthId))
        );
        return observable.flatMap(new Func1<ResultSet, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(ResultSet rows) {
                return Observable.just(rows.one().getInt(0) > 0);
            }
        }, onError(), onCompletion());
    }


    public Observable<Boolean> applyUpdate(final PatientUpdate patientUpdate) {
        return findPatient(patientUpdate.getHealthId()).flatMap(new Func1<Boolean, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Boolean patientExists) {
                return patientExists ? savePatientUpdate(patientUpdate) : Observable.just(false);
            }
        });
    }

    private Observable<Boolean> savePatientUpdate(PatientUpdate patientUpdate) {
        Observable<ResultSet> observable = Observable.from(
                cqlOperations.executeAsynchronously(patientQueryBuilder.updatePatientQuery(patientUpdate)));

        return observable.flatMap(new Func1<ResultSet, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(ResultSet rows) {
                return Observable.just(true);
            }
        }, onError(), onCompletion());
    }


    private Func0<Observable<? extends Boolean>> onCompletion() {
        return new Func0<Observable<? extends Boolean>>() {
            @Override
            public Observable<? extends Boolean> call() {
                return null;
            }
        };
    }

    private Func1<Throwable, Observable<? extends Boolean>> onError() {
        return new Func1<Throwable, Observable<? extends Boolean>>() {
            @Override
            public Observable<? extends Boolean> call(Throwable throwable) {
                return Observable.error(throwable);
            }
        };
    }

}
