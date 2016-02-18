package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import me.prettyprint.cassandra.utils.TimeUUIDUtils;
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
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
                BatchStatement batchStatement = new BatchStatement();
                for (EncounterDetail encounterDetail : encountersDetails) {
                    if (patientUpdate.hasConfidentialChange()) {
                        Statement updateEncounterQuery = shrQueryBuilder.updateEncounterQuery(patientUpdate, encounterDetail);
                        batchStatement.add(updateEncounterQuery);
                    }
                    if (patientUpdate.hasAddressChange()) {
                        Statement insertCatchmentFeedForAddressChange = shrQueryBuilder.insertCatchmentFeedForAddressChange(patientUpdate, encounterDetail);
                        batchStatement.add(insertCatchmentFeedForAddressChange);

                    }
                }
                Observable<ResultSet> updateObservable = Observable.from(cqlOperations.executeAsynchronously(batchStatement));
                return updateObservable.flatMap(new Func1<ResultSet, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(ResultSet rows) {
                        LOG.debug("Updated all encounters for patient %s", patientUpdate.getHealthId());
                        return Observable.just(true);
                    }
                });
            }
        }, RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds());
    }

    public Observable<Boolean> applyMerge(final PatientUpdate patientUpdate, Patient patientToBeMergedWith) {
        Observable<EncounterBundle> encounterBundlesObservable = getEncounterBundles(patientUpdate.getHealthId());
        Observable<Boolean> encounterMergeObservable = encounterBundlesObservable.flatMap(onEncounterBundlesSucess(patientToBeMergedWith));
        return encounterMergeObservable;
    }

    private Func1<EncounterBundle, Observable<Boolean>> onEncounterBundlesSucess(final Patient patientToBeMergedWith) {
        return new Func1<EncounterBundle, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(EncounterBundle encounterBundle) {
                return associateEncounterBundleTo(encounterBundle, patientToBeMergedWith);
            }
        };
    }


    public Observable<EncounterBundle> getEncounterBundles(String healthId) {
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
                            UUID receivedAtUuid = row.getUUID(RECEIVED_AT_COLUMN_NAME);
                            Date receivedAt = TimeUuidUtil.getDateFromUUID(receivedAtUuid);

                            encountersBundles.add(new EncounterBundle(encounterId, healthId, content, receivedAt, receivedAtUuid));
                        }
                        return Observable.from(encountersBundles);
                    }
                });
            }
        });
    }

    public Observable<Boolean> associateEncounterBundleTo(EncounterBundle encounterBundle, Patient patientToBeMergeWith) {
        String healthIdToMergeWith = patientToBeMergeWith.getHealthId();
        encounterBundle.associateTo(healthIdToMergeWith);

        UUID createdAt = TimeUUIDUtils.getUniqueTimeUUIDinMillis();
        Update updateEncounterStmt = shrQueryBuilder.updateEncounterOnMergeStatement(encounterBundle, healthIdToMergeWith);

        Insert insertEncByPatientStatement = shrQueryBuilder.insertEncByPatientStatement(encounterBundle, createdAt, healthIdToMergeWith, createdAt);
        Batch batch = QueryBuilder.batch(updateEncounterStmt, insertEncByPatientStatement);
        Address address = patientToBeMergeWith.getAddress();

        if (address != null) {
            Insert insertEncByCatchmentStmt = shrQueryBuilder.getInsEncByCatchmentStmt(
                    address.getDivisionId(), address.getConcatenatedDistrictId(),
                    address.getConcatenatedUpazilaId(), address.getConcatenatedCityCorporationId(),
                    address.getConcatenatedWardId(), encounterBundle.getEncounterId(),
                    createdAt, createdAt);
            batch.add(insertEncByCatchmentStmt);

        }

        Observable<ResultSet> mergeObservable = Observable.from(cqlOperations.executeAsynchronously(batch), Schedulers.immediate());
        return mergeObservable.flatMap(respondOnNext(true), RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds());
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
                            UUID receivedAtUuid = row.getUUID(RECEIVED_AT_COLUMN_NAME);
                            encountersDetails.add(new EncounterDetail(encounterId, receivedAtUuid));
                        }
                        return Observable.just(encountersDetails);
                    }
                });
            }
        };
    }


    public Observable<List<EncounterBundle>> getAllEncounters(String healthId) {
        Observable<List<String>> encounterIdsForPatient = getEncounterIdsForPatient(healthId);
        return encounterIdsForPatient.flatMap(new Func1<List<String>, Observable<List<EncounterBundle>>>() {
            @Override
            public Observable<List<EncounterBundle>> call(List<String> encounterIds) {
                Statement encountersByEncounterIdsQuery = shrQueryBuilder.findEncounterBundlesByEncounterIdsQuery(encounterIds);
                Observable<ResultSet> observable = Observable.from(cqlOperations.queryAsynchronously(encountersByEncounterIdsQuery.toString()));
                return observable.concatMap(new Func1<ResultSet, Observable<List<EncounterBundle>>>() {
                    @Override
                    public Observable<List<EncounterBundle>> call(ResultSet rows) {
                        List<EncounterBundle> encountersBundles = new ArrayList<>();
                        for (Row row : rows.all()) {
                            String encounterId = row.getString(ENCOUNTER_ID_COLUMN_NAME);
                            String healthId = row.getString(HEALTH_ID_COLUMN_NAME);
                            String content = row.getString(shrQueryBuilder.getEncounterContentColumnName());
                            UUID receivedAtUuid = row.getUUID(RECEIVED_AT_COLUMN_NAME);
                            Date receivedAt = TimeUuidUtil.getDateFromUUID(receivedAtUuid);

                            encountersBundles.add(new EncounterBundle(encounterId, healthId, content, receivedAt, receivedAtUuid));
                        }
                        return Observable.just(encountersBundles);
                    }
                });
            }
        });
    }

}
