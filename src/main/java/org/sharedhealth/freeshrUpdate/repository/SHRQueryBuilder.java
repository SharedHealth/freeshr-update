package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.*;

@Component
public class SHRQueryBuilder {
    @Autowired
    public ShrUpdateConfiguration configuration;

    public SHRQueryBuilder() {
    }

    public SHRQueryBuilder(ShrUpdateConfiguration configuration) {
        this.configuration = configuration;
    }


    public Select findPatientQuery(String healthId) {
        return QueryBuilder.select().countAll().from(configuration.getCassandraKeySpace(), PATIENT_TABLE_NAME)
                .where(eq(HEALTH_ID_COLUMN_NAME, healthId)).limit(1);
    }

    public Statement updatePatientQuery(String healthId, Map<String, Object> patientChanges) {
        Map<String, Object> changes = patientChanges;

        Update update = QueryBuilder
                .update(configuration.getCassandraKeySpace(), PATIENT_TABLE_NAME);

        for (String column : changes.keySet()) {
            update.with(set(column, changes.get(column)));
        }

        return update
                .where(eq(HEALTH_ID_COLUMN_NAME, healthId))
                .enableTracing();
    }

    public Statement findEncounterIdsQuery(String healthId) {
        return QueryBuilder.select(ENCOUNTER_ID_COLUMN_NAME)
                .from(configuration.getCassandraKeySpace(), ENCOUNTER_BY_PATIENT_TABLE_NAME)
                .where(eq(HEALTH_ID_COLUMN_NAME, healthId));
    }

    public Statement findEncountersByEncounterIdsQuery(List<String> encounterIds) {
        return QueryBuilder.select(ENCOUNTER_ID_COLUMN_NAME, RECEIVED_DATE_COLUMN_NAME).from(configuration.getCassandraKeySpace(), ENCOUNTER_TABLE_NAME)
                .where(in(ENCOUNTER_ID_COLUMN_NAME, encounterIds.toArray()));
    }

    public Statement updateEncounterQuery(PatientUpdate patientUpdate, EncounterDetail encountersDetail) {
        String confidentialChange = (String) patientUpdate.getChangeSet().getPatientDetailChanges().get(CONFIDENTIALITY_COLUMN_NAME);
        Update update = QueryBuilder
                .update(configuration.getCassandraKeySpace(), ENCOUNTER_TABLE_NAME);
        
        update.with(set(PATIENT_CONFIDENTIALITY_COLUMN_NAME, confidentialChange));
        
        return update.where(eq(ENCOUNTER_ID_COLUMN_NAME, encountersDetail.getEncounterId())).
                and(eq(RECEIVED_DATE_COLUMN_NAME, encountersDetail.getReceivedDate()));
    }
}
