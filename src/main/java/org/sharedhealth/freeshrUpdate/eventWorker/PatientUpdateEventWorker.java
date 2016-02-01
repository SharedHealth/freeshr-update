package org.sharedhealth.freeshrUpdate.eventWorker;

import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.sharedhealth.freeshrUpdate.client.MciWebClient;
import org.sharedhealth.freeshrUpdate.domain.EncounterBundle;
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

import java.util.List;

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

    @Autowired
    EncounterMovementTracker encounterMovementTracker;

    public PatientUpdateEventWorker() {
    }

    @Override
    public void process(Event event) {
        try {
            final PatientUpdate patientUpdate = readFrom(extractContent(event.getContent()), PatientUpdate.class);
            patientUpdate.setEventId(event.getId());
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
        final String activePatientHealthId = (String) patientUpdate.getPatientMergeChanges().get(MERGED_WITH_COLUMN_NAME);
//        ensurePresent(patientUpdate.getHealthId()).first().flatMap(new Func1<Patient, Observable<Boolean>>() {
//            @Override
//            public Observable<Boolean> call(Patient patient) {
//                return ensurePresent(activePatientHealthId).first().flatMap(new Func1<Patient, Observable<Boolean>>() {
//                    @Override
//                    public Observable<Boolean> call(Patient activePatient) {
//                        if (activePatient == null) throw new RuntimeException(String.format("Processing merge event:Active patient [%s] does not exist", activePatientHealthId));
//                        return patientRepository.mergeUpdate(patientUpdate).flatMap(moveEncounters(patientUpdate.getHealthId(), activePatientHealthId),
//                                RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds());
//                    }
//                }, RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds());
//            }
//        }).toBlocking().subscribe();
        ensurePresent(activePatientHealthId).first().flatMap(new Func1<Patient, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Patient activePatient) {
               if (activePatient == null) throw new RuntimeException(String.format("Processing merge event:Active patient [%s] does not exist", activePatientHealthId));
               return patientRepository.mergeUpdate(patientUpdate).flatMap(moveEncounters(patientUpdate.getHealthId(), activePatientHealthId),
                        RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds());
            }
        }, RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds()).toBlocking().subscribe();
    }

    private void applyUpdate(final PatientUpdate patientUpdate) {
        //check if has address change request, if so check if has merges pending, if not fail, let retry take care of it when the merge is over
        if (patientUpdate.hasAddressChange()) {
            final Boolean mergePending = isMergePending(patientUpdate);
            if (mergePending) throw new RuntimeException(String.format("There are pending encounters for Patient [%s] to be merged.", patientUpdate.getHealthId()));
        }

        Observable<Boolean> updateEncounterResponse =
                patientRepository.applyUpdate(patientUpdate).flatMap(onSuccess(patientUpdate),
                        RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds());
        updateEncounterResponse.toBlocking().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean result) {
                System.out.println(String.format("Encounters for patient %s %s updated", patientUpdate.getHealthId(), result ? "" : "not"));
                LOG.debug(
                        String.format("Encounters for patient %s %s updated", patientUpdate.getHealthId(), result ? "" : "not"));
            }
        });
    }

    private Boolean isMergePending(PatientUpdate patientUpdate) {
        final int numberOfEncounterMovements = encounterMovementTracker.pendingNumberOfEncounterMovements(patientUpdate.getHealthId(), "to");
        return numberOfEncounterMovements > 0;
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

    private Func1<Boolean, Observable<Boolean>> moveEncounters(final String updatedPatientHealthId, final String mergePatientHealthId) {
        return new Func1<Boolean, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Boolean updateSuccess) {
                LOG.debug(String.format("Patient %s %s updated", updatedPatientHealthId, updateSuccess ? "" : "not"));
                if (updateSuccess) {
                    //return mergedWithPatientFetchObservable.flatMap(onMergedWithPatientFetchSuccess(patientUpdate), RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds());
                    return ensurePresent(mergePatientHealthId).flatMap(onMergeMoveEncountersOfPatient(updatedPatientHealthId), RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds());
                } else {
                    LOG.warn("Can not move encounters from Patient [%s] to Patient [%s]. Likely cause, Patient(s) could not be identify.");
                    return Observable.just(false);
                }
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

    private Func1<Patient, Observable<Boolean>> onMergeMoveEncountersOfPatient(final String updatedPatientHealthId) {
        return new Func1<Patient, Observable<Boolean>>() {
            public Observable<Boolean> call(final Patient patientMergedWith) {
                if (patientMergedWith != null) {
                    System.out.println("Patient health id:" + updatedPatientHealthId);
                    System.out.println("Encounter Repo:" + encounterRepository);
                    //final String updatedPatientHealthId = patientUpdate.getHealthId();
                    return encounterRepository.getAllEncounters(updatedPatientHealthId).flatMap(new Func1<List<EncounterBundle>, Observable<Boolean>>() {
                        @Override
                        public Observable<Boolean> call(List<EncounterBundle> encounterBundles) {
                            return trackEncounters(updatedPatientHealthId, patientMergedWith.getHealthId(), encounterBundles).concatMap(moveEncounterToPatient(patientMergedWith));
                        }
                    }, RxMaps.<Boolean>logAndForwardError(LOG), RxMaps.<Boolean>completeResponds());
                }
                return Observable.just(true);
            }

            private Observable<EncounterBundle> trackEncounters(final String updatedPatientHealthId, final String mergedPatientHealthId, final List<EncounterBundle> encounterBundles) {
                return Observable.from(encounterMovementTracker.trackPatientEncounterMovement(updatedPatientHealthId, mergedPatientHealthId, encounterBundles));
                //return Observable.from(encounterBundles);
            }

        };
    }

    private Func1<EncounterBundle, Observable<Boolean>> moveEncounterToPatient(final Patient patientMergedWith) {
        return new Func1<EncounterBundle, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(final EncounterBundle encounterBundle) {
//                final EncounterMovement encToMove = encounterMovementTracker.getEncounterForMovement(encounterBundle.getEncounterId());
//                if (encToMove == null) return Observable.just(true);
                return encounterRepository.associateEncounterBundleTo(encounterBundle, patientMergedWith).flatMap(new Func1<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(Boolean associationDone) {
                        if (associationDone) {
                            encounterMovementTracker.doneMovingEncounter(encounterBundle.getEncounterId(), patientMergedWith.getHealthId());
                        }
                        return Observable.just(associationDone);
                    }
                });
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
