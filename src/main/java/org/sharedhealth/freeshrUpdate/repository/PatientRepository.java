package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.sharedhealth.freeshrUpdate.domain.Address;
import org.sharedhealth.freeshrUpdate.domain.Patient;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.stereotype.Component;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.Map;

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
        final String healthId = patientUpdate.getHealthId();
        return checkPatientExists(healthId).flatMap(new Func1<Boolean, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Boolean patientExists) {
                LOG.debug(String.format("Patient %s %s found", healthId, patientExists ? "" : "not"));
                return patientExists ? savePatientUpdate(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges()) : Observable.just(false);
            }
        }, RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds());
    }

    public Observable<Boolean> mergeUpdate(final PatientUpdate patientUpdate) {
        final String healthId = patientUpdate.getHealthId();
        return checkPatientExists(healthId).flatMap(new Func1<Boolean, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Boolean patientExists) {
                LOG.debug(String.format("Patient %s %s found", healthId, patientExists ? "" : "not"));
                return patientExists ? savePatientUpdate(patientUpdate.getHealthId(), patientUpdate.getPatientMergeChanges()) : Observable.just(false);
            }
        }, RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds());
    }

    public Observable<Boolean> checkPatientExists(final String healthId) {
        Observable<ResultSet> patientExistsObservable = Observable.from(
                cqlOperations.queryAsynchronously(shrQueryBuilder.checkPatientExistsQuery(healthId))
        );
        return patientExistsObservable.flatMap(new Func1<ResultSet, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(ResultSet rows) {
                return Observable.just(rows.one().getLong("count") > 0);
            }
        }, RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds()).firstOrDefault(false);
    }

    public Observable<Patient> fetchPatient(String healthID) {
        Observable<ResultSet> patientObservable = Observable.from(
                cqlOperations.queryAsynchronously(shrQueryBuilder.getPatient(healthID))
        );
        return patientObservable.flatMap(new Func1<ResultSet, Observable<Patient>>() {
            @Override
            public Observable<Patient> call(ResultSet patientRows) {
                Row patientRow = patientRows.one();
                if (patientRow != null) {
                    return Observable.just(readPatient(patientRow));
                }
                return Observable.just(null);
            }
        });

    }

    private Patient readPatient(Row row) {
        Patient patient = new Patient();
        patient.setHealthId(row.getString("health_id"));
        String divisionId = row.getString("division_id");
        String districtId = row.getString("district_id");
        String upazilaId = row.getString("upazila_id");
        if(divisionId != null && districtId != null && upazilaId!= null){
            Address address = new Address();
            address.setDivisionId(divisionId);
            address.setDistrictId(districtId);
            address.setUpazilaId(upazilaId);
            address.setCityCorporationId(row.getString("city_corporation_id"));
            address.setUnionOrUrbanWardId(row.getString("union_urban_ward_id"));
            address.setAddressLine(row.getString("address_line"));
            patient.setAddress(address);
        }
        return patient;

    }


    private Observable<Boolean> savePatientUpdate(String healthId, Map<String, Object> patientChanges) {
        Observable<ResultSet> observable = Observable.from(
                cqlOperations.executeAsynchronously(shrQueryBuilder.updatePatientQuery(healthId, patientChanges))).first();

        return observable.flatMap(new Func1<ResultSet, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(ResultSet rows) {
                return Observable.just(true);
            }
        }, RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds()).firstOrDefault(false);
    }

    public Observable<Boolean> save(Patient patient) {
        if (patient.getHealthId() != null) {
            Observable<ResultSet> saveObservable = Observable.from(cqlOperations.executeAsynchronously(buildPatientInsertQuery(patient)), Schedulers.io());
            return saveObservable.flatMap(RxMaps.respondOnNext(true), RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds());
        }
        return Observable.just(false);
    }

    private Insert buildPatientInsertQuery(Patient patient) {
        Address address = patient.getAddress();
        Insert insert = QueryBuilder.insertInto("patient")
                .value("health_id", patient.getHealthId())
                .value("merged_with", patient.getMergedWith())
                .value("active", patient.isActive());
        if (patient.getMergedWith() == null) {
            insert.value("gender", patient.getGender())
                    .value("address_line", address.getAddressLine())
                    .value("division_id", address.getDivisionId())
                    .value("district_id", address.getDistrictId())
                    .value("upazila_id", address.getUpazilaId())
                    .value("city_corporation_id", address.getCityCorporationId())
                    .value("union_urban_ward_id", address.getUnionOrUrbanWardId())
                    .value("confidentiality", patient.getConfidentiality().getLevel());
        }
        return insert;
    }

}
