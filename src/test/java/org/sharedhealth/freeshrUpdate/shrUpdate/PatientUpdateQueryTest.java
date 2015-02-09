package org.sharedhealth.freeshrUpdate.shrUpdate;

import com.datastax.driver.core.Statement;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.sharedhealth.freeshrUpdate.domain.AddressData;
import org.sharedhealth.freeshrUpdate.domain.PatientData;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PatientUpdateQueryTest {
    @Mock
    ShrUpdateConfiguration configuration;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldCreateUpdateQuery() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        PatientUpdate patientUpdate = PatientUpdateMother.confidentialPatient();
        Statement update = new PatientUpdateQuery(configuration).get(patientUpdate);
        assertTrue(update.toString().contains("UPDATE"));
    }

    @Test
    public void shouldCreateUpdateQueryForKeySpaceAndTable() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        PatientUpdate patientUpdate = PatientUpdateMother.confidentialPatient();
        Statement update = new PatientUpdateQuery(configuration).get(patientUpdate);
        assertTrue(update.toString().contains("keyspace." + PatientUpdateQuery.PATIENT_TABLE_NAME));
    }

    @Test
    public void shouldCreateUpdateQueryBasedOnHealthId() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        PatientUpdate patientUpdate = PatientUpdateMother.confidentialPatient("bar");
        Statement update = new PatientUpdateQuery(configuration).get(patientUpdate);
        String whereClause = String.format("WHERE %s='bar'", PatientUpdateQuery
                .HEALTH_ID_COLUMN_NAME);
        assertTrue(update.toString().contains(whereClause));
    }

    @Test
    public void shouldCreateUpdateQueryForConfidential() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        PatientUpdate patientUpdate = PatientUpdateMother.confidentialPatient();
        Statement update = new PatientUpdateQuery(configuration).get(patientUpdate);
        assertTrue(update.toString().contains("SET confidential=true"));
    }

    @Test
    public void shouldCreateUpdateQueryIgnoreConfidentialIfNotSet() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        PatientUpdate patientUpdate = PatientUpdateMother.addressLineUpdated("new address line");
        Statement update = new PatientUpdateQuery(configuration).get(patientUpdate);
        assertFalse(update.toString().contains("confidential"));
    }

    @Test
    public void shouldCreateUpdateQueryForAddressLine() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        String addressLine = "new address line";
        PatientUpdate patientUpdate = PatientUpdateMother.addressLineUpdated(addressLine);
        Statement update = new PatientUpdateQuery(configuration).get(patientUpdate);
        assertTrue(update.toString().contains(String.format("address_line='%s'", addressLine)));
    }

    @Test
    public void shouldCreateUpdateQueryForDivisionId() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        AddressData addressData = new AddressData();
        addressData.setDivisionId("foo");
        PatientUpdate patientUpdate = PatientUpdateMother.patientAddressUpdate(addressData);
        Statement update = new PatientUpdateQuery(configuration).get(patientUpdate);
        assertTrue(update.toString().contains("division_id='foo'"));
    }

    @Test
    public void shouldCreateUpdateQueryForDistrictId() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        AddressData addressData = new AddressData();
        addressData.setDistrictId("foo");
        PatientUpdate patientUpdate = PatientUpdateMother.patientAddressUpdate(addressData);
        Statement update = new PatientUpdateQuery(configuration).get(patientUpdate);
        assertTrue(update.toString().contains("district_id='foo'"));
    }

    @Test
    public void shouldCreateUpdateQueryForUpazilaId() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        AddressData addressData = new AddressData();
        addressData.setUpazilaId("foo");
        PatientUpdate patientUpdate = PatientUpdateMother.patientAddressUpdate(addressData);
        Statement update = new PatientUpdateQuery(configuration).get(patientUpdate);
        assertTrue(update.toString().contains("upazila_id='foo'"));
    }

    @Test
    public void shouldCreateUpdateQueryForCityCorporationId() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        AddressData addressData = new AddressData();
        addressData.setCityCorporationId("foo");
        PatientUpdate patientUpdate = PatientUpdateMother.patientAddressUpdate(addressData);
        Statement update = new PatientUpdateQuery(configuration).get(patientUpdate);
        assertTrue(update.toString().contains("city_corporation_id='foo'"));
    }

    @Test
    public void shouldCreateUpdateQueryForWardId() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("keyspace");
        AddressData addressData = new AddressData();
        addressData.setUnionOrUrbanWardId("foo");
        PatientUpdate patientUpdate = PatientUpdateMother.patientAddressUpdate(addressData);
        Statement update = new PatientUpdateQuery(configuration).get(patientUpdate);
        assertTrue(update.toString().contains("union_urban_ward_id='foo'"));
    }
}