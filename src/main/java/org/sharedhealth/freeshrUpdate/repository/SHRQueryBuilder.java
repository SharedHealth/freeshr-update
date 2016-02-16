package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import me.prettyprint.cassandra.utils.TimeUUIDUtils;
import org.apache.commons.lang3.StringUtils;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.sharedhealth.freeshrUpdate.domain.Address;
import org.sharedhealth.freeshrUpdate.domain.EncounterBundle;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.utils.TimeUuidUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

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


    public Select checkPatientExistsQuery(String healthId) {
        return QueryBuilder.select().countAll().from(configuration.getCassandraKeySpace(), PATIENT_TABLE_NAME)
                .where(eq(HEALTH_ID_COLUMN_NAME, healthId)).limit(1);
    }

    public Select getPatient(String healthId) {
        return QueryBuilder.select().all().from(configuration.getCassandraKeySpace(), PATIENT_TABLE_NAME)
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

    public Update updateEncounterOnMergeStatement(EncounterBundle encounterBundle, String healthIdToMergeWith){
        Update updateEncounter = QueryBuilder.update(configuration.getCassandraKeySpace(), ENCOUNTER_TABLE_NAME);
        updateEncounter.with(set(getEncounterContentColumnName(), encounterBundle.getEncounterContent()))
                        .and(set(HEALTH_ID_COLUMN_NAME, healthIdToMergeWith))
                        .where(eq(ENCOUNTER_ID_COLUMN_NAME, encounterBundle.getEncounterId()))
                        .and(eq(RECEIVED_AT_COLUMN_NAME, TimeUuidUtil.uuidForDate(encounterBundle.getReceivedAt())));

        return updateEncounter;
    }

    public Insert insertEncByPatientStatement(EncounterBundle encounterBundle, UUID createdAt, String healthIdToMergeWith, UUID mergedAt){
        Insert insert = QueryBuilder.insertInto(configuration.getCassandraKeySpace(), ENCOUNTER_BY_PATIENT_TABLE_NAME)
                                    .value(ENCOUNTER_ID_COLUMN_NAME, encounterBundle.getEncounterId())
                                    .value(HEALTH_ID_COLUMN_NAME, healthIdToMergeWith)
                                    .value(CREATED_AT_COLUMN_NAME, createdAt)
                                    .value(MERGED_AT_COLUMN_NAME, mergedAt);

        return insert;
    }

    public Statement findEncounterIdsQuery(String healthId) {
        return QueryBuilder.select(ENCOUNTER_ID_COLUMN_NAME)
                .from(configuration.getCassandraKeySpace(), ENCOUNTER_BY_PATIENT_TABLE_NAME)
                .where(eq(HEALTH_ID_COLUMN_NAME, healthId));
    }

    public Statement findEncounterDetailsByEncounterIdsQuery(List<String> encounterIds) {
        return QueryBuilder.select(ENCOUNTER_ID_COLUMN_NAME, RECEIVED_AT_COLUMN_NAME).from(configuration.getCassandraKeySpace(), ENCOUNTER_TABLE_NAME)
                .where(in(ENCOUNTER_ID_COLUMN_NAME, encounterIds.toArray()));
    }

    public Statement findEncounterBundlesByEncounterIdsQuery(List<String> encounterIds) {
        return QueryBuilder.select(ENCOUNTER_ID_COLUMN_NAME,getEncounterContentColumnName(),HEALTH_ID_COLUMN_NAME, RECEIVED_AT_COLUMN_NAME).from(configuration.getCassandraKeySpace(), ENCOUNTER_TABLE_NAME)
                .where(in(ENCOUNTER_ID_COLUMN_NAME, encounterIds.toArray()));
    }

    public String getEncounterContentColumnName(){
        return String.format("%s%s", ENCOUNTER_CONTENT_COLUMN_PREFIX, configuration.getFhirDocumentSchemaVersion());
    }

    public Statement updateEncounterQuery(PatientUpdate patientUpdate, EncounterDetail encountersDetail) {
        String confidentialChange = (String) patientUpdate.getChangeSet().getPatientDetailChanges().get(CONFIDENTIALITY_COLUMN_NAME);
        Update update = QueryBuilder
                .update(configuration.getCassandraKeySpace(), ENCOUNTER_TABLE_NAME);
        
        update.with(set(PATIENT_CONFIDENTIALITY_COLUMN_NAME, confidentialChange));
        
        return update.where(eq(ENCOUNTER_ID_COLUMN_NAME, encountersDetail.getEncounterId())).
                and(eq(RECEIVED_AT_COLUMN_NAME, TimeUuidUtil.uuidForDate(encountersDetail.getReceivedDate())));
    }

    public Statement insertCatchmentFeedForAddressChange(PatientUpdate patientUpdate, EncounterDetail encounterDetail){
        Address addressChange = patientUpdate.getChangeSet().getAddressChange();
        UUID createdAt = TimeUUIDUtils.getUniqueTimeUUIDinMillis();

        Insert insertEncByCatchmentStmt = getInsEncByCatchmentStmt(addressChange.getDivisionId(), addressChange.getConcatenatedDistrictId(), addressChange.getConcatenatedUpazilaId(), addressChange.getConcatenatedCityCorporationId(), addressChange.getConcatenatedWardId(), encounterDetail.getEncounterId(), createdAt, null);
        return insertEncByCatchmentStmt;
    }

    public Insert getInsEncByCatchmentStmt(String divisionId, String concatenatedDistrictId, String concatenatedUpazillaId, String concatenatedCityCorporationId, String concatenatedWardId, String encounterId, UUID createdAt, UUID mergedAt) {
        return QueryBuilder.insertInto(configuration.getCassandraKeySpace(), ENCOUNTER_BY_CATCHMENT_TABLE_NAME)
                                    .value(DIVISION_ID_COLUMN_NAME, divisionId)
                                    .value(DISTRICT_ID_COLUMN_NAME, concatenatedDistrictId)
                                    .value(UPAZILA_ID_COLUMN_NAME, concatenatedUpazillaId)
                                    .value(YEAR, Calendar.getInstance().get(Calendar.YEAR))
                                    .value(CREATED_AT_COLUMN_NAME, createdAt)
                                    .value(MERGED_AT_COLUMN_NAME, mergedAt)
                                    .value(CITY_CORPORATION_ID_COLUMN_NAME, StringUtils.defaultString(concatenatedCityCorporationId))
                                    .value(UNION_OR_URBAN_COLUMN_NAME, StringUtils.defaultString(concatenatedWardId))
                                    .value(ENCOUNTER_ID_COLUMN_NAME, encounterId);
    }
}
