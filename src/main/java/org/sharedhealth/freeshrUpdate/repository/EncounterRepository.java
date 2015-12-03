package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import org.sharedhealth.freeshrUpdate.domain.Address;
import org.sharedhealth.freeshrUpdate.domain.EncounterBundle;
import org.sharedhealth.freeshrUpdate.domain.Patient;
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
import java.util.UUID;

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
                for (EncounterDetail encounterDetail : encountersDetails) {
                    BatchStatement batchStatement = new BatchStatement();
                    if(patientUpdate.hasConfidentialChange()){
                        Statement updateEncounterQuery = shrQueryBuilder.updateEncounterQuery(patientUpdate, encounterDetail);
                        batchStatement.add(updateEncounterQuery);
                    }
                    if(patientUpdate.hasAddressChange()){
                        Statement insertCatchmentFeedForAddressChange = shrQueryBuilder.insertCatchmentFeedForAddressChange(patientUpdate, encounterDetail);
                        batchStatement.add(insertCatchmentFeedForAddressChange);

                    }
                    observables.add(Observable.from(cqlOperations.executeAsynchronously(batchStatement)));
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

    public Observable<Boolean> applyMerge(final PatientUpdate patientUpdate, Patient patientToBeMergedWith){
//        System.out.println("Applying Encounter merge");
        Observable<EncounterBundle> encounterBundlesObservable = getEncounterBundles(patientUpdate.getHealthId());
        Observable<Boolean> encounterMergeObservable = encounterBundlesObservable.flatMap(getEncountersSuccess(patientToBeMergedWith));
        return encounterMergeObservable;
    }

    private Func1<EncounterBundle, Observable<Boolean>> getEncountersSuccess(final Patient patientToBeMergedWith) {
        return new Func1<EncounterBundle, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(EncounterBundle encounterBundle) {
//                System.out.println("Processing encounter bundle");
                return associateEncounterBundleTo(encounterBundle, patientToBeMergedWith);
            }
        };
    }


    public Observable<EncounterBundle> getEncounterBundles(String healthId){
        Observable<List<String>> encounterIdsForPatient = getEncounterIdsForPatient(healthId);
        return encounterIdsForPatient.flatMap(new Func1<List<String>, Observable<EncounterBundle>>() {
            @Override
            public Observable<EncounterBundle> call(List<String> encounterIds) {
//                System.out.println("Constructing Encounter Bundles");
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

    public Observable<Boolean> associateEncounterBundleTo(EncounterBundle encounterBundle, Patient patientToBeMergeWith){
//        System.out.println("Substituting healthIds");
        String healthIdToMergeWith = patientToBeMergeWith.getHealthId();
        encounterBundle.associateTo(healthIdToMergeWith);

        UUID createdAt = TimeUuidUtil.uuidForDate(new Date());
        Update updateEncounterStmt = shrQueryBuilder.updateEncounterOnMergeStatement(encounterBundle, healthIdToMergeWith);

        Insert insertEncByPatientStatement = shrQueryBuilder.insertEncByPatientStatement(encounterBundle, createdAt, healthIdToMergeWith);
        Batch batch = QueryBuilder.batch(updateEncounterStmt, insertEncByPatientStatement);
        Address address = patientToBeMergeWith.getAddress();

        if(address != null){
        Insert insertEncByCatchmentStmt = shrQueryBuilder.getInsEncByCatchmentStmt(address.getDivisionId(), address.getConcatenatedDistrictId(), address.getConcatenatedUpazilaId(), address.getConcatenatedCityCorporationId(),
                address.getConcatenatedWardId(), encounterBundle.getEncounterId(), createdAt);
            batch.add(insertEncByCatchmentStmt);

        }

        Observable<ResultSet> mergeObservable = Observable.from(cqlOperations.executeAsynchronously(batch), Schedulers.immediate());
        return mergeObservable.flatMap(respondOnNext(true), RxMaps.<Boolean>logAndForwardError(LOG), completeResponds(true));
    }

    public Observable<List<String>> getEncounterIdsForPatient(final String healthId) {
        Statement encounterIdsQuery = shrQueryBuilder.findEncounterIdsQuery(healthId);
        Observable<ResultSet> observable = Observable.from(cqlOperations.queryAsynchronously(encounterIdsQuery.toString()));
        return observable.flatMap(new Func1<ResultSet, Observable<List<String>>>() {
            @Override
            public Observable<List<String>> call(ResultSet rows) {
//                System.out.println("Fetching Encounter Ids");
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
                            Date date = TimeUuidUtil.getDateFromUUID(row.getUUID(RECEIVED_AT_COLUMN_NAME));
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
