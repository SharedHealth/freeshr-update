package org.sharedhealth.freeshrUpdate.shrUpdate;

import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.springframework.stereotype.Component;

@Component
public class PatientRepository {
    public boolean applyUpdate(PatientUpdate patientUpdate) {
        return false;
    }
}
