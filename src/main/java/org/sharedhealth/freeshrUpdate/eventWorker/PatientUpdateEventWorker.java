package org.sharedhealth.freeshrUpdate.eventWorker;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.shrUpdate.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PatientUpdateEventWorker implements EventWorker {

    @Autowired
    PatientRepository patientRepository;

    public PatientUpdateEventWorker(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public void process(Event event) {
        try {
            PatientUpdate patientUpdate = readFrom(extractContent(event.getContent()), PatientUpdate.class);
            patientRepository.applyUpdate(patientUpdate);

        } catch (IOException e) {
            e.printStackTrace();
        }
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
