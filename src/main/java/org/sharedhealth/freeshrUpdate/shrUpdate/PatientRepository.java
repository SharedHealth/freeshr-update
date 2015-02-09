package org.sharedhealth.freeshrUpdate.shrUpdate;

import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Update;
import org.sharedhealth.freeshrUpdate.domain.PatientData;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.stereotype.Component;

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

    public boolean applyUpdate(PatientUpdate patientUpdate) {
        Statement update = patientUpdateQuery.get(patientUpdate);
        PatientData changeSet = patientUpdate.getChangeSet();
        cqlOperations.executeAsynchronously(update);
        return false;
    }
}
