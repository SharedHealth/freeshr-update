package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.Statement;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.sharedhealth.freeshrUpdate.domain.Address;
import org.sharedhealth.freeshrUpdate.domain.EncounterBundle;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother;
import org.sharedhealth.freeshrUpdate.utils.TimeUuidUtil;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static junit.framework.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.*;

public class SHRQueryBuilderTest {
    @Mock
    ShrUpdateConfiguration configuration;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldCreateUpdatePatientQuery() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        PatientUpdate patientUpdate = PatientUpdateMother.confidentialPatient();
        Statement update = new SHRQueryBuilder(configuration).updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges());
        assertTrue(update.toString().contains("UPDATE"));
    }

    @Test
    public void shouldCreateUpdateQueryForKeySpaceAndTable() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        PatientUpdate patientUpdate = PatientUpdateMother.confidentialPatient();
        Statement update = new SHRQueryBuilder(configuration).updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges());
        assertTrue(update.toString().contains("keyspace." + PATIENT_TABLE_NAME));
    }

    @Test
    public void shouldCreateUpdateQueryBasedOnHealthId() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        PatientUpdate patientUpdate = PatientUpdateMother.confidentialPatient("bar");
        Statement update = new SHRQueryBuilder(configuration).updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges());
        String whereClause = String.format("WHERE %s='bar'", HEALTH_ID_COLUMN_NAME);
        assertTrue(update.toString().contains(whereClause));
    }

    @Test
    public void shouldCreateUpdateQueryForConfidentialityAsVeryRestricted() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        PatientUpdate patientUpdate = PatientUpdateMother.confidentialPatient();
        Statement update = new SHRQueryBuilder(configuration).updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges());
        String confidentialityPart = String.format("SET %s='V'", CONFIDENTIALITY_COLUMN_NAME);
        assertTrue(update.toString().contains(confidentialityPart));
    }

    @Test
    public void shouldCreateUpdateQueryForConfidentialityAsNormal() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        PatientUpdate patientUpdate = PatientUpdateMother.notConfidentialPatient();
        Statement update = new SHRQueryBuilder(configuration).updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges());
        String confidentialityPart = String.format("SET %s='N'", CONFIDENTIALITY_COLUMN_NAME);
        assertTrue(update.toString().contains(confidentialityPart));
    }

    @Test
    public void shouldCreateUpdateQueryIgnoreConfidentialityIfNotSet() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        PatientUpdate patientUpdate = PatientUpdateMother.addressLineUpdated("new address line");
        Statement update = new SHRQueryBuilder(configuration).updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges());
        assertFalse(update.toString().contains(CONFIDENTIALITY_COLUMN_NAME));
    }

    @Test
    public void shouldCreateUpdateQueryForAddressLine() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        String addressLine = "new address line";
        PatientUpdate patientUpdate = PatientUpdateMother.addressLineUpdated(addressLine);
        Statement update = new SHRQueryBuilder(configuration).updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges());
        assertTrue(update.toString().contains(String.format("SET %s='%s'", ADDRESS_LINE_COLUMN_NAME, addressLine)));
    }

    @Test
    public void shouldCreateUpdateQueryForDivisionId() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        Address address = new Address();
        address.setDivisionId("foo");
        PatientUpdate patientUpdate = PatientUpdateMother.patientAddressUpdate(address);
        Statement update = new SHRQueryBuilder(configuration).updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges());
        assertTrue(update.toString().contains(String.format("SET %s='foo'", DIVISION_ID_COLUMN_NAME)));
    }

    @Test
    public void shouldCreateUpdateQueryForDistrictId() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        Address address = new Address();
        address.setDistrictId("foo");
        PatientUpdate patientUpdate = PatientUpdateMother.patientAddressUpdate(address);
        Statement update = new SHRQueryBuilder(configuration).updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges());
        assertTrue(update.toString().contains(String.format("SET %s='foo'", DISTRICT_ID_COLUMN_NAME)));
    }

    @Test
    public void shouldCreateUpdateQueryForUpazilaId() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        Address address = new Address();
        address.setUpazilaId("foo");
        PatientUpdate patientUpdate = PatientUpdateMother.patientAddressUpdate(address);
        Statement update = new SHRQueryBuilder(configuration).updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges());
        assertTrue(update.toString().contains(String.format("SET %s='foo'", UPAZILA_ID_COLUMN_NAME)));
    }

    @Test
    public void shouldCreateUpdateQueryForCityCorporationId() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        Address address = new Address();
        address.setCityCorporationId("foo");
        PatientUpdate patientUpdate = PatientUpdateMother.patientAddressUpdate(address);
        Statement update = new SHRQueryBuilder(configuration).updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges());
        assertTrue(update.toString().contains(String.format("SET %s='foo'", CITY_CORPORATION_ID_COLUMN_NAME)));
    }

    @Test
    public void shouldCreateUpdateQueryForWardId() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        Address address = new Address();
        address.setUnionOrUrbanWardId("foo");
        PatientUpdate patientUpdate = PatientUpdateMother.patientAddressUpdate(address);
        Statement update = new SHRQueryBuilder(configuration).updatePatientQuery(patientUpdate.getHealthId(), patientUpdate.getPatientDetailChanges());
        assertTrue(update.toString().contains(String.format("SET %s='foo'", UNION_OR_URBAN_COLUMN_NAME)));
    }

    @Test
    public void shouldCreateSelectAllEncounterIdsQueryForGivenHealthId() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        String healthId = "123";
        String encountersQuery = new SHRQueryBuilder(configuration).findEncounterIdsQuery(healthId).toString();

        String query = String.format("SELECT %s FROM keyspace.%s WHERE %s='123';", ENCOUNTER_ID_COLUMN_NAME,
                ENCOUNTER_BY_PATIENT_TABLE_NAME, HEALTH_ID_COLUMN_NAME);
        assertEquals(query, encountersQuery);
    }

    @Test
    public void shouldCreateSelectAllEncountersQueryByEncounterIds() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        List<String> encounterIds = asList("123", "234");
        String encountersQuery = new SHRQueryBuilder(configuration).findEncounterDetailsByEncounterIdsQuery(encounterIds).toString();

        String query = String.format("SELECT %s,%s FROM keyspace.%s WHERE %s IN ('123','234');", ENCOUNTER_ID_COLUMN_NAME,
                RECEIVED_AT_COLUMN_NAME, ENCOUNTER_TABLE_NAME, ENCOUNTER_ID_COLUMN_NAME);
        assertEquals(query, encountersQuery);

    }

    @Test
    public void shouldCreateUpdateEncounterQueryAsNormal() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        PatientUpdate patientUpdate = PatientUpdateMother.notConfidentialPatient();
        Date receivedDate = new Date();
        receivedDate.setTime(12345);
        EncounterDetail encounterDetail = new EncounterDetail("123", receivedDate);

        Statement updateEncounterQuery = new SHRQueryBuilder(configuration).updateEncounterQuery(patientUpdate, encounterDetail);

        String expectedQuery = String.format("UPDATE keyspace.%s SET %s='N' WHERE %s='123' AND %s=12345;",
                ENCOUNTER_TABLE_NAME, PATIENT_CONFIDENTIALITY_COLUMN_NAME, ENCOUNTER_ID_COLUMN_NAME, RECEIVED_AT_COLUMN_NAME);
        assertEquals(expectedQuery, updateEncounterQuery.toString());
    }

    @Test
    public void shouldCreateUpdateEncounterQueryAsVeryRestricted() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        PatientUpdate patientUpdate = PatientUpdateMother.confidentialPatient();
        Date receivedDate = new Date();
        receivedDate.setTime(12345);
        EncounterDetail encounterDetail = new EncounterDetail("123", receivedDate);

        Statement updateEncounterQuery = new SHRQueryBuilder(configuration).updateEncounterQuery(patientUpdate, encounterDetail);

        String expectedQuery = String.format("UPDATE keyspace.%s SET %s='V' WHERE %s='123' AND %s=12345;",
                ENCOUNTER_TABLE_NAME, PATIENT_CONFIDENTIALITY_COLUMN_NAME, ENCOUNTER_ID_COLUMN_NAME, RECEIVED_AT_COLUMN_NAME);
        assertEquals(expectedQuery, updateEncounterQuery.toString());
    }
    
    @Test
    public void shouldCreateUpdatePatientQueryOnMerge() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        PatientUpdate patientUpdate = PatientUpdateMother.merge("some health id", "12345");
        Date receivedDate = new Date();
        receivedDate.setTime(12345);

        Statement updatePatientQuery = new SHRQueryBuilder(configuration).updatePatientQuery("123", patientUpdate.getPatientMergeChanges());

        String expectedQuery = String.format("UPDATE keyspace.%s SET %s=false,%s='12345' WHERE %s='123';",
                PATIENT_TABLE_NAME, ACTIVE_COLUMN_NAME, MERGED_WITH_COLUMN_NAME, HEALTH_ID_COLUMN_NAME);
        assertEquals(expectedQuery, updatePatientQuery.toString());
    }

    @Test
    public void shouldCreateUpdateEncounterStatement() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        when(configuration.getFhirDocumentSchemaVersion()).thenReturn("v1");

        Date receivedAt = new Date();
        UUID uuid = TimeUuidUtil.uuidForDate(receivedAt);

        EncounterBundle encounterBundle = new EncounterBundle("E1", "P1", "E1 content", receivedAt);

        Statement updateEncStatement = new SHRQueryBuilder(configuration).updateEncounterOnMergeStatement(encounterBundle, "P2");
        String expectedQuery = String.format("UPDATE keyspace.%s SET content_v1='%s',health_id='%s' WHERE encounter_id='%s' AND received_at=%s;",
                ENCOUNTER_TABLE_NAME,encounterBundle.getEncounterContent(),"P2",encounterBundle.getEncounterId(), uuid);

        assertEquals(expectedQuery, updateEncStatement.toString());

    }

    @Test
    public void shouldCreateInsertToEncByPatientFeedTableStatement() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");

        UUID uuid = TimeUuidUtil.uuidForDate(new Date());

        EncounterBundle encounterBundle = new EncounterBundle("E1", "P1", "E1 content", null);

        Statement insert = new SHRQueryBuilder(configuration).insertEncByPatientStatement(encounterBundle, uuid, "P2");
        String expectedQuery = String.format("INSERT INTO keyspace.%s(%s,%s,%s) VALUES ('%s','%s',%s);",
                            ENCOUNTER_BY_PATIENT_TABLE_NAME, ENCOUNTER_ID_COLUMN_NAME, HEALTH_ID_COLUMN_NAME, CREATED_AT_COLUMN_NAME,
                            encounterBundle.getEncounterId(), "P2", uuid);

        assertEquals(expectedQuery, insert.toString());


    }
}