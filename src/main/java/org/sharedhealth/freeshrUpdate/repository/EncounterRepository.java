package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import org.sharedhealth.freeshrUpdate.domain.EncounterBundle;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.utils.TimeUuidUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.stereotype.Component;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.sharedhealth.freeshrUpdate.repository.RxMaps.completeResponds;
import static org.sharedhealth.freeshrUpdate.repository.RxMaps.respondOnNext;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.*;

@Component
public class EncounterRepository {
    @Autowired
    @Qualifier("SHRCassandraTemplate")
    private CqlOperations cqlOperations;
    private static final Logger LOG = LoggerFactory.getLogger(EncounterRepository.class);

    @Autowired
    private SHRQueryBuilder shrQueryBuilder;

    public EncounterRepository() {
    }

    public EncounterRepository(CqlOperations cqlOperations, SHRQueryBuilder shrQueryBuilder) {
        this.cqlOperations = cqlOperations;
        this.shrQueryBuilder = shrQueryBuilder;
    }


    public Observable<Boolean> applyUpdate(final PatientUpdate patientUpdate) {
        Observable<List<String>> encounterIdsObservable = getEncounterIdsForPatient(patientUpdate.getHealthId());
        Observable<List<EncounterDetail>> encountersForPatient = encounterIdsObservable.flatMap(getEncounterDetails());
        return encountersForPatient.flatMap(new Func1<List<EncounterDetail>, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(List<EncounterDetail> encountersDetails) {
                List<Observable<ResultSet>> observables = new ArrayList<>();
                for (EncounterDetail encountersDetail : encountersDetails) {
                    Statement updateEncounterQuery = shrQueryBuilder.updateEncounterQuery(patientUpdate, encountersDetail);
                    observables.add(Observable.from(cqlOperations.executeAsynchronously(updateEncounterQuery)));
                }
                return Observable.zip(observables, new FuncN<Boolean>() {
                    @Override
                    public Boolean call(Object... args) {
                        LOG.debug("Updated all encounters for patient %s", patientUpdate.getHealthId());
                        return true;
                    }
                });
            }
        }, onError(), onCompleted());
    }

    public Observable<EncounterBundle> getEncounterBundles(String healthId){
        Observable<List<String>> encounterIdsForPatient = getEncounterIdsForPatient(healthId);
        return encounterIdsForPatient.flatMap(new Func1<List<String>, Observable<EncounterBundle>>() {
            @Override
            public Observable<EncounterBundle> call(List<String> encounterIds) {
                Statement encountersByEncounterIdsQuery = shrQueryBuilder.findEncounterBundlesByEncounterIdsQuery(encounterIds);
                Observable<ResultSet> observable = Observable.from(cqlOperations.queryAsynchronously(encountersByEncounterIdsQuery.toString()));
                return observable.flatMap(new Func1<ResultSet, Observable<EncounterBundle>>() {
                    @Override
                    public Observable<EncounterBundle> call(ResultSet rows) {
                        List<EncounterBundle> encountersBundles = new ArrayList<>();
                        for (Row row : rows.all()) {
                            String encounterId = row.getString(ENCOUNTER_ID_COLUMN_NAME);
                            String healthId = row.getString(HEALTH_ID_COLUMN_NAME);
                            String content = row.getString(shrQueryBuilder.getEncounterContentColumnName());
                            Date receivedAt = TimeUuidUtil.getDateFromUUID(row.getUUID(RECEIVED_AT_COLUMN_NAME));

                            encountersBundles.add(new EncounterBundle(encounterId, healthId, content, receivedAt));
                        }
                        return Observable.from(encountersBundles);
                    }
                });
            }
        });
    }

    public Observable<Boolean> associateEncounterBundleTo(EncounterBundle encounterBundle, String healthIdToMergeWith){
        encounterBundle.associateTo(healthIdToMergeWith);

        Update updateEncounterStmt = shrQueryBuilder.updateEncounterOnMergeStatement(encounterBundle);

        Batch batch = QueryBuilder.batch(updateEncounterStmt);

        Observable<ResultSet> mergeObservable = Observable.from(cqlOperations.executeAsynchronously(batch), Schedulers.io());
        return mergeObservable.flatMap(respondOnNext(true), RxMaps.<Boolean>logAndForwardError(LOG), completeResponds(true));
    }

    public Observable<List<String>> getEncounterIdsForPatient(final String healthId) {
        Statement encounterIdsQuery = shrQueryBuilder.findEncounterIdsQuery(healthId);
        Observable<ResultSet> observable = Observable.from(cqlOperations.queryAsynchronously(encounterIdsQuery.toString()));
        return observable.flatMap(new Func1<ResultSet, Observable<List<String>>>() {
            @Override
            public Observable<List<String>> call(ResultSet rows) {
                LOG.debug("Fetching All Encounters for patient %s", healthId);
                List<String> encounterIds = new ArrayList<>();
                for (Row row : rows.all()) {
                    encounterIds.add(row.getString(ENCOUNTER_ID_COLUMN_NAME));
                }
                return Observable.just(encounterIds);
            }
        });
    }

    private Func1<List<String>, Observable<List<EncounterDetail>>> getEncounterDetails() {
        return new Func1<List<String>, Observable<List<EncounterDetail>>>() {
            @Override
            public Observable<List<EncounterDetail>> call(List<String> encounterIds) {
                Statement encountersByEncounterIdsQuery = shrQueryBuilder.findEncounterDetailsByEncounterIdsQuery(encounterIds);
                Observable<ResultSet> observable = Observable.from(cqlOperations.queryAsynchronously(encountersByEncounterIdsQuery.toString()));
                return observable.flatMap(new Func1<ResultSet, Observable<List<EncounterDetail>>>() {
                    @Override
                    public Observable<List<EncounterDetail>> call(ResultSet rows) {
                        List<EncounterDetail> encountersDetails = new ArrayList<>();
                        for (Row row : rows.all()) {
                            String encounterId = row.getString(ENCOUNTER_ID_COLUMN_NAME);
                            Date date = row.getDate(RECEIVED_AT_COLUMN_NAME);
                            encountersDetails.add(new EncounterDetail(encounterId, date));
                        }
                        return Observable.just(encountersDetails);
                    }
                });
            }
        };
    }

    private Func0<Observable<? extends Boolean>> onCompleted() {
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
