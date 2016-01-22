package org.sharedhealth.freeshrUpdate.eventWorker;

import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.sharedhealth.freeshrUpdate.client.MciWebClient;
import org.sharedhealth.freeshrUpdate.domain.Patient;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.repository.EncounterRepository;
import org.sharedhealth.freeshrUpdate.repository.PatientRepository;
import org.sharedhealth.freeshrUpdate.repository.RxMaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.MERGED_WITH_COLUMN_NAME;
import static org.sharedhealth.freeshrUpdate.utils.StringUtils.readFrom;

@Component
public class PatientUpdateEventWorker implements EventWorker {
    private static final Logger LOG = LoggerFactory.getLogger(PatientUpdateEventWorker.class);
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private EncounterRepository encounterRepository;
    @Autowired
    private MciWebClient mciWebClient;

    public PatientUpdateEventWorker() {
    }

    @Override
    public void process(Event event) {
        try {
            final PatientUpdate patientUpdate = readFrom(extractContent(event.getContent()), PatientUpdate.class);
            if (patientUpdate.hasMergeChanges())
                merge(patientUpdate);
            else if (patientUpdate.hasPatientDetailChanges())
                applyUpdate(patientUpdate);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void merge(final PatientUpdate patientUpdate) {
        Observable<Boolean> savePatientResponse = patientRepository.mergeIfFound(patientUpdate);
        Observable<Boolean> encounterMergeObservable = savePatientResponse.flatMap(onPatientMergeSuccess(patientUpdate), RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds());
        encounterMergeObservable.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean updated) {
                LOG.debug(String.format("Patient's encounters are %s merged", updated ? "" : "not"));
            }
        }, actionOnError());
        encounterMergeObservable.toBlocking().firstOrDefault(true);
    }

    private void applyUpdate(final PatientUpdate patientUpdate) {
        Observable<Boolean> savePatientResponse = patientRepository.applyUpdate(patientUpdate);
        Observable<Boolean> updateEncounterResponse = savePatientResponse.
                flatMap(onSuccess(patientUpdate), RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds());

        updateEncounterResponse.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean updated) {
                LOG.debug(String.format("Encounters for patient %s %s updated", patientUpdate.getHealthId(), updated ? "" :
                        "not"));
            }
        }, actionOnError());
        updateEncounterResponse.toBlocking().firstOrDefault(true);
    }

    private Action1<Throwable> actionOnError() {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        };
    }

    private Func1<Boolean, Observable<Boolean>> onSuccess(final PatientUpdate patientUpdate) {
        return new Func1<Boolean, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Boolean patientUpdated) {
                LOG.debug(String.format("Patient %s %s updated", patientUpdate.getHealthId(), patientUpdated ? "" :
                        "not"));
                if (patientUpdated) {
                    return encounterRepository.applyUpdate(patientUpdate);
                }
                return Observable.just(false);
            }
        };
    }

    private Func1<Boolean, Observable<Boolean>> onPatientMergeSuccess(final PatientUpdate patientUpdate) {
//        System.out.println("Patient table merge applied");
        return new Func1<Boolean, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Boolean patientUpdated) {
                LOG.debug(String.format("Patient %s %s updated", patientUpdate.getHealthId(), patientUpdated ? "" :
                        "not"));
                if (patientUpdated) {
                    Observable<Patient> mergedWithPatientFetchObservable = ensurePresent((String) patientUpdate.getPatientMergeChanges().get(MERGED_WITH_COLUMN_NAME));
                    return mergedWithPatientFetchObservable.flatMap(onMergedWithPatientFetchSuccess(patientUpdate), RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds());
                } else
                    return Observable.just(false);
            }
        };
    }

    private Func1<Patient, Observable<Boolean>> onMergedWithPatientFetchSuccess(final PatientUpdate patientUpdate) {
        return new Func1<Patient, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Patient patientMergedWith) {
                if (patientMergedWith != null) {
                    return encounterRepository.applyMerge(patientUpdate, patientMergedWith);
                }
                return Observable.just(false);
            }
        };
    }


    @Override
    public void cleanUp(Event event) {

    }

    public Observable<Patient> ensurePresent(final String healthId) {
        Observable<Patient> patientPresent = patientRepository.fetchPatient(healthId);
        return patientPresent.flatMap(new Func1<Patient, Observable<Patient>>() {
            @Override
            public Observable<Patient> call(Patient patient) {
                if (patient != null) return Observable.just(patient);
                try {
                    return Observable.just(findRemote(healthId));
                } catch (Exception e) {
                    return Observable.error(e);
                }
            }
        });
    }

    private Patient findRemote(final String healthId) throws Exception {
//            System.out.println("Downloading patient:" + healthId);
        String patientResponse = mciWebClient.getPatient(healthId);
        if (patientResponse != null) {
            Patient patient = readFrom(patientResponse, Patient.class);
            Observable<Boolean> saveStatus = patientRepository.save(patient);
            return (saveStatus.toBlocking().first()) ? patient : null;
        }
        return null;
    }

    private static String extractContent(String content) {
        return content.trim().replaceFirst(
                "^<!\\[CDATA\\[", "").replaceFirst("\\]\\]>$", "");
    }


}
