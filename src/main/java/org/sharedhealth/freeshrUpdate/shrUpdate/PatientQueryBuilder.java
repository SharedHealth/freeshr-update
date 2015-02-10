package org.sharedhealth.freeshrUpdate.shrUpdate;

import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.sharedhealth.freeshrUpdate.domain.PatientData;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;

@Component
public class PatientQueryBuilder {
    public static final String PATIENT_TABLE_NAME = "patient";
    public static final String HEALTH_ID_COLUMN_NAME = "health_id";
    @Autowired
    public ShrUpdateConfiguration configuration;

    public PatientQueryBuilder() {
    }

    public PatientQueryBuilder(ShrUpdateConfiguration configuration) {
        this.configuration = configuration;
    }


    public Select findPatientQuery(String healthId) {
        return QueryBuilder.select().countAll().from(configuration.getCassandraKeySpace(), PATIENT_TABLE_NAME)
                .where(eq(HEALTH_ID_COLUMN_NAME, healthId)).limit(1);
    }

    public Statement updatePatientQuery(PatientUpdate patientUpdate) {
        PatientData patientData = patientUpdate.getChangeSet();
        Map<String, Object> changes = patientData.getChanges();

        Update update = QueryBuilder
                .update(configuration.getCassandraKeySpace(), PATIENT_TABLE_NAME);

        for (String column : changes.keySet()) {
            update.with(set(column, changes.get(column)));
        }

        return update
                .where(eq(HEALTH_ID_COLUMN_NAME, patientUpdate.getHealthId()))
                .enableTracing();
    }
}
