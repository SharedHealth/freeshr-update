package org.sharedhealth.freeshrUpdate.eventWorker;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.repository.EncounterRepository;
import org.sharedhealth.freeshrUpdate.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import java.io.IOException;

@Component
public class PatientUpdateEventWorker implements EventWorker {
    private static final Logger LOG = LoggerFactory.getLogger(PatientUpdateEventWorker.class);
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private EncounterRepository encounterRepository;

    public PatientUpdateEventWorker() {
    }

    public PatientUpdateEventWorker(PatientRepository patientRepository, EncounterRepository encounterRepository) {
        this.patientRepository = patientRepository;
        this.encounterRepository = encounterRepository;
    }

    @Override
    public void process(Event event) {
        try {
            final PatientUpdate patientUpdate = readFrom(extractContent(event.getContent()), PatientUpdate.class);
            if(patientUpdate.hasMergeChanges())
                merge(patientUpdate);
            else if(patientUpdate.hasPatientDetailChanges())
                applyUpdate(patientUpdate);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    private void merge(final PatientUpdate patientUpdate) {
        Observable<Boolean> savePatientResponse = patientRepository.mergeIfFound(patientUpdate);
        Observable<Boolean> encounterMergeObservable = savePatientResponse.flatMap(onPatientMergeSuccess(patientUpdate), onError(), onCompleted());
        encounterMergeObservable.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean updated) {
                LOG.debug(String.format("Patient's encounters are %s merged", updated ? "": "not" ));
            }
        }, actionOnError());

    }

    private void applyUpdate(final PatientUpdate patientUpdate) {
        Observable<Boolean> savePatientResponse = patientRepository.applyUpdate(patientUpdate);
        Observable<Boolean> updateEncounterResponse = savePatientResponse.
                flatMap(onSuccess(patientUpdate), onError(), onCompleted());

        updateEncounterResponse.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean updated) {
                LOG.debug(String.format("Encounters for patient %s %s updated", patientUpdate.getHealthId(), updated ? "" :
                        "not"));
            }
        }, actionOnError());
    }

    private Action1<Throwable> actionOnError() {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                LOG.error(throwable.getMessage());
            }
        };
    }

    private Func0<Observable<Boolean>> onCompleted() {
        return new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                return null;
            }
        };
    }

    private Func1<Throwable, Observable<Boolean>> onError() {
        return new Func1<Throwable, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Throwable throwable) {
                return null;
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
        return new Func1<Boolean, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Boolean patientUpdated) {
                LOG.debug(String.format("Patient %s %s updated", patientUpdate.getHealthId(), patientUpdated ? "" :
                        "not"));
                if(patientUpdated)
                    return encounterRepository.applyMerge(patientUpdate);
                else
                    return Observable.just(false);
            }
        };
    }


    @Override
    public void cleanUp(Event event) {

    }

    public static <T> T readFrom(String content, Class<T> returnType) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        return mapper.readValue(content, returnType);

    }

    private static String extractContent(String content) {
        return content.replaceFirst(
                "^<!\\[CDATA\\[", "").replaceFirst("\\]\\]>$", "");
    }


}
