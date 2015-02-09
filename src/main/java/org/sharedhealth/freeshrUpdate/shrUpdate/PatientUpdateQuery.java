package org.sharedhealth.freeshrUpdate.shrUpdate;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Truncate;
import com.datastax.driver.core.querybuilder.Update;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.sharedhealth.freeshrUpdate.domain.PatientData;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;

@Component
public class PatientUpdateQuery {
    public static final String PATIENT_TABLE_NAME = "patient";
    @Autowired
    public ShrUpdateConfiguration configuration;



    public PatientUpdateQuery(ShrUpdateConfiguration configuration) {
        this.configuration = configuration;
    }

    public Statement get(PatientUpdate patientUpdate) {
        PatientData patientData = patientUpdate.getChangeSet();
        Statement statement = QueryBuilder
                .update(configuration.getCassandraKeySpace(), PATIENT_TABLE_NAME)
                .with(set("confidential", patientData.getConfidential()))
                .where(eq("foo", "bar")).setConsistencyLevel(ConsistencyLevel.QUORUM)
                ;

        return statement;
    }
}
