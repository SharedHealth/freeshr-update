package org.sharedhealth.freeshrUpdate.utils;


import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.sharedhealth.freeshrUpdate.domain.EncounterBundle;
import org.sharedhealth.freeshrUpdate.domain.Patient;
import org.springframework.cassandra.core.CqlOperations;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static org.junit.Assert.assertEquals;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.*;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.CONFIDENTIALITY_COLUMN_NAME;

public class QueryUtils {

    private CqlOperations cqlOperations;

    public QueryUtils(CqlOperations cqlOperations) {
        this.cqlOperations = cqlOperations;
    }

    public void insertEncByPatient(String encounterId, String healthId, Date createdAt) {
        Insert insert = QueryBuilder.insertInto("freeshr", "enc_by_patient").value("encounter_id", encounterId).value("health_id", healthId).value("created_at", TimeUuidUtil.uuidForDate(createdAt));
        cqlOperations.execute(insert);
    }

    public Row fetchEncounter(String encounterId) {
        ResultSet rs = cqlOperations.query(QueryBuilder.select().all().from("freeshr", "encounter").where(eq(KeySpaceUtils.ENCOUNTER_ID_COLUMN_NAME, encounterId)).limit(1));
        return rs.all().get(0);
    }

    public List<Row> fetchEncounterByPatientFeed(String healthId) {
        Select select = QueryBuilder.select().all().from("freeshr", ENCOUNTER_BY_PATIENT_TABLE_NAME);
        select.where(eq(HEALTH_ID_COLUMN_NAME, healthId));
        ResultSet rs = cqlOperations.query(select);
        return rs.all();
    }

    public List<Row> fetchCatchmentFeed(String divisionId, String districtId){
        Select select = QueryBuilder.select().all()
                .from("freeshr", ENCOUNTER_BY_CATCHMENT_TABLE_NAME);
        select.where(eq("division_id", divisionId))
                .and(eq("district_id", districtId))
                .and(eq("year", Calendar.getInstance().get(Calendar.YEAR)));
        ResultSet rs = cqlOperations.query(select);

        return rs.all();
    }

    public void insertEncounter(String encounterId, String healthId, Date recievedAt, String content, String contentColumnName) {
        Insert insert = QueryBuilder.insertInto("freeshr", "encounter").value("encounter_id", encounterId).value("health_id", healthId).value("received_at", TimeUuidUtil.uuidForDate(recievedAt)).value(contentColumnName, content);
        cqlOperations.execute(insert);
    }

    public void insertEncounterByCatchment(String encounterId, String divisionId, String districtId, Date createdAt) {
        Insert insert = QueryBuilder.insertInto("freeshr", "enc_by_catchment")
                .value("encounter_id", encounterId).value("division_id", divisionId)
                .value("district_id", districtId)
                .value("year", Calendar.getInstance().get(Calendar.YEAR))
                .value("created_at", TimeUuidUtil.uuidForDate(createdAt));
        cqlOperations.execute(insert);
    }


    public void insertPatient(String healthId) {
        Insert insert = QueryBuilder.insertInto("freeshr", "patient").value("health_id", healthId);
        cqlOperations.execute(insert);
    }

    public Row fetchPatient(String healthId) {
        ResultSet rs = cqlOperations.query(QueryBuilder.select().all().from("freeshr", "patient").where(eq(KeySpaceUtils.HEALTH_ID_COLUMN_NAME, healthId)).limit(1));
        return rs.all().get(0);
    }

    public void assertPatient(Patient patient, Row row) {
        assertEquals(patient.getHealthId(), row.getString(HEALTH_ID_COLUMN_NAME));
        assertEquals(patient.getGender(), row.getString(GENDER_COLUMN_NAME));
        assertEquals(patient.getMergedWith(), row.getString(MERGED_WITH_COLUMN_NAME));
        assertEquals(patient.isActive(), row.getBool(ACTIVE_COLUMN_NAME));
        if (patient.getAddress() != null) {
            assertEquals(patient.getAddress().getAddressLine(), row.getString(ADDRESS_LINE_COLUMN_NAME));
            assertEquals(patient.getAddress().getDivisionId(), row.getString(DIVISION_ID_COLUMN_NAME));
            assertEquals(patient.getAddress().getDistrictId(), row.getString(DISTRICT_ID_COLUMN_NAME));
            assertEquals(patient.getAddress().getUpazilaId(), row.getString(UPAZILA_ID_COLUMN_NAME));
        }
        if(patient.getConfidentiality() != null)
            assertEquals(patient.getConfidentiality().getLevel(), row.getString(CONFIDENTIALITY_COLUMN_NAME));
    }

    public void trucateAllTables(){
        cqlOperations.execute("truncate encounter");
        cqlOperations.execute("truncate enc_by_catchment");
        cqlOperations.execute("truncate enc_by_patient");
        cqlOperations.execute("truncate enc_history");
        cqlOperations.execute("truncate patient");
    }

    public void assertEncounter(EncounterBundle encounterBundle, String encounterId, String healthId, String content) {
        assertEquals(encounterId, encounterBundle.getEncounterId());
        assertEquals(healthId, encounterBundle.getHealthId());
        assertEquals(content, encounterBundle.getEncounterContent());
    }

    public void assertEncounterRow(Row encounterRow, String encounterId, String healthId, String content, String confidentiality) {
        assertEquals(encounterId, encounterRow.getString("encounter_id"));
        assertEquals(healthId, encounterRow.getString("health_id"));
        assertEquals(content, encounterRow.getString("content_v1"));
        assertEquals(confidentiality, encounterRow.getString("patient_confidentiality"));
    }

}