package org.sharedhealth.freeshrUpdate.shrUpdate;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Update;
import org.sharedhealth.freeshrUpdate.domain.PatientData;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.dao.DataAccessException;
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
    private PatientUpdateQuery patientUpdateQuery;

    public PatientRepository(CqlOperations cqlOperations, PatientUpdateQuery patientUpdateQuery) {
        this.cqlOperations = cqlOperations;
        this.patientUpdateQuery = patientUpdateQuery;
    }

    public Observable<Boolean> applyUpdate(PatientUpdate patientUpdate) {
        Observable<ResultSet> observable = Observable.from(
                cqlOperations.executeAsynchronously(patientUpdateQuery.get(patientUpdate)));

        return observable.flatMap(new Func1<ResultSet, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(ResultSet rows) {
                return Observable.just(true);
            }
        }, new Func1<Throwable, Observable<? extends Boolean>>() {
            @Override
            public Observable<? extends Boolean> call(Throwable throwable) {
                return Observable.error(throwable);
            }
        }, new Func0<Observable<? extends Boolean>>() {
            @Override
            public Observable<? extends Boolean> call() {
                return null;
            }
        });
    }
}
