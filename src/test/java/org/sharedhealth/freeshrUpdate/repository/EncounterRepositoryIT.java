package org.sharedhealth.freeshrUpdate.repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sharedhealth.freeshrUpdate.config.SHREnvironmentMock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfig;
import org.sharedhealth.freeshrUpdate.domain.AddressData;
import org.sharedhealth.freeshrUpdate.domain.EncounterBundle;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils;
import org.sharedhealth.freeshrUpdate.utils.TimeUuidUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother.addressChange;
import static org.sharedhealth.freeshrUpdate.utils.KeySpaceUtils.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = ShrUpdateConfig.class)
public class EncounterRepositoryIT {

    @Autowired
    EncounterRepository encounterRepository;

    @Autowired
    SHRQueryBuilder queryBuilder;

    @Autowired
    @Qualifier("SHRCassandraTemplate")
    CqlOperations cqlOperations;


    @Test
    public void shouldFetchEncounterIdsForAGivenPatient(){

        insertEncByPatient("E1", "P1", new DateTime(2015, 07, 8, 0, 0).toDate());
        insertEncByPatient("E2", "P1", new DateTime(2015, 07, 9, 0, 0).toDate());
        insertEncByPatient("E2", "P2", new DateTime(2015, 07, 8, 0, 0).toDate());

        List<String> encounterIdsForP1 = encounterRepository.getEncounterIdsForPatient("P1").toBlocking().first();
        assertEquals(encounterIdsForP1, asList("E1","E2"));
    }

    @Test
    public void shouldFetchEncountersForAGivenPatient() {
        insertEncByPatient("E1", "P1", new DateTime(2015, 07, 8, 0, 0).toDate());
        insertEncByPatient("E2", "P1", new DateTime(2015, 07, 9, 0, 0).toDate());
        insertEncByPatient("E3", "P2", new DateTime(2015, 07, 8, 0, 0).toDate());

        insertEncounter("E1", "P1", new DateTime(2015, 07, 8, 0, 0).toDate(), "e1 content");
        insertEncounter("E2", "P1", new DateTime(2015, 07, 9, 0, 0).toDate(), "e2 content");
        insertEncounter("E3", "P2", new DateTime(2015, 07, 8, 0, 0).toDate(), "e3 content");

        Iterable<EncounterBundle> encounters = encounterRepository.getEncounterBundles("P1").toBlocking().toIterable();

        ArrayList<EncounterBundle> encountersForP1 = Lists.newArrayList(encounters);

        assertThat(encountersForP1.size(), is(2));
        assertEncounter(encountersForP1.get(0), "E1", "P1", "e1 content");
        assertEncounter(encountersForP1.get(1),"E2","P1","e2 content");

    }

    @Test
    public void shouldAssociateAnEncounterWithNewHealthId(){
        EncounterBundle encounterBundle = new EncounterBundle("E1", "P1", "E1 content for P1", new DateTime(2015,07,8,0,0).toDate());
        insertEncounter(encounterBundle.getEncounterId(), encounterBundle.getHealthId(), encounterBundle.getReceivedAt(), encounterBundle.getEncounterContent());
        EncounterBundle encounterBeforeMerge = fetchEncounterBundle("E1");
        assertEncounter(encounterBeforeMerge, "E1", "P1", "E1 content for P1");

        encounterRepository.associateEncounterBundleTo(encounterBundle, "P2").toBlocking().first();
        EncounterBundle encounterBundleAfterMerge = fetchEncounterBundle("E1");
        List<Row> encByPatient = fetchEncounterByPatientFeed();

        assertEncounter(encounterBundleAfterMerge, "E1", "P2", "E1 content for P2");
        assertThat(encByPatient.size(), is(1));
        assertThat("E1", is(encByPatient.get(0).getString("encounter_id")));
        assertThat("P2", is(encByPatient.get(0).getString("health_id")));
    }

    @Test
    public void shouldAddEntriesToCatchmentFeedForAddressChange() throws Exception {
        AddressData addressData = new AddressData();
        addressData.setDivisionId("30");
        addressData.setDistrictId("26");
        addressData.setUpazilaId("18");
        addressData.setCityCorporationId("60");
        addressData.setUnionOrUrbanWardId("45");

        insertEncounter("E1", "P1", new DateTime(2015, 07, 8, 0, 0).toDate(), "e1 content");
        insertEncounter("E2", "P1", new DateTime(2015, 07, 9, 0, 0).toDate(), "e2 content");
        insertEncounter("E3", "P1", new DateTime(2015, 07, 10, 0, 0).toDate(), "e3 content");

        insertEncByPatient("E1", "P1", new DateTime(2015, 07, 8, 0, 0).toDate());
        insertEncByPatient("E2", "P1", new DateTime(2015, 07, 9, 0, 0).toDate());
        insertEncByPatient("E3", "P1", new DateTime(2015, 07, 10, 0, 0).toDate());

        insertEncounterByCatchment("E1","20","15", new DateTime(2015, 8, 8, 0,0).toDate());
        insertEncounterByCatchment("E2","20","15", new DateTime(2015, 8, 9, 0,0).toDate());
        insertEncounterByCatchment("E3","20","15", new DateTime(2015, 8, 10, 0,0).toDate());

        assertThat(fetchCatchmentFeed("20","15").size(), is(3));
        assertThat(fetchCatchmentFeed("30","3026").size(), is(0));

        PatientUpdate patientUpdate = new PatientUpdate();
        patientUpdate.setChangeSetMap(addressChange(addressData));
        patientUpdate.setHealthId("P1");

        encounterRepository.applyUpdate(patientUpdate).toBlocking().last();

        assertThat(fetchCatchmentFeed("20","15").size(), is(3));
        List<Row> rows = fetchCatchmentFeed("30", "3026");
        assertThat(rows.size(), is(3));
        assertThat(rows.get(0).getString("upazila_id"), is("302618"));
        assertThat(rows.get(0).getString("city_corporation_id"), is("30261860"));
        assertThat(rows.get(0).getString("union_urban_ward_id"), is("3026186045"));
        assertThat(rows.get(0).getString("encounter_id"), is("E1"));
        assertThat(rows.get(1).getString("encounter_id"), is("E2"));
        assertThat(rows.get(2).getString("encounter_id"), is("E3"));


    }

    private void insertEncByPatient(String encounterId, String healthId, Date createdAt) {
        Insert insert = QueryBuilder.insertInto("freeshr", "enc_by_patient").value("encounter_id", encounterId).value("health_id", healthId).value("created_at", TimeUuidUtil.uuidForDate(createdAt));
        cqlOperations.execute(insert);
    }

    private EncounterBundle fetchEncounterBundle(String encounterId) {
        String encounterContentColumnName = queryBuilder.getEncounterContentColumnName();
        ResultSet rs = cqlOperations.query(QueryBuilder.select(ENCOUNTER_ID_COLUMN_NAME, encounterContentColumnName, HEALTH_ID_COLUMN_NAME).from("freeshr", "encounter").where(eq(KeySpaceUtils.ENCOUNTER_ID_COLUMN_NAME, encounterId)).limit(1));
        Row row = rs.all().get(0);
        String healthId = row.getString(HEALTH_ID_COLUMN_NAME);
        String content = row.getString(encounterContentColumnName);
        return new EncounterBundle(encounterId, healthId, content, null);
    }

    private List<Row> fetchEncounterByPatientFeed() {
        ResultSet rs = cqlOperations.query(QueryBuilder.select().all().from("freeshr", ENCOUNTER_BY_PATIENT_TABLE_NAME));
        return rs.all();
    }

    private List<Row> fetchCatchmentFeed(String divisionId, String districtId){
        Select select = QueryBuilder.select().all()
                .from("freeshr", ENCOUNTER_BY_CATCHMENT_TABLE_NAME);
        select.where(eq("division_id", divisionId))
                .and(eq("district_id", districtId))
                .and(eq("year", Calendar.getInstance().get(Calendar.YEAR)));
        ResultSet rs = cqlOperations.query(select);

        return rs.all();
    }

    private void insertEncounter(String encounterId, String healthId, Date recievedAt, String content) {
        Insert insert = QueryBuilder.insertInto("freeshr", "encounter").value("encounter_id", encounterId).value("health_id", healthId).value("received_at", TimeUuidUtil.uuidForDate(recievedAt)).value(queryBuilder.getEncounterContentColumnName(), content);
        cqlOperations.execute(insert);
    }

    private void insertEncounterByCatchment(String encounterId, String divisionId, String districtId, Date createdAt) {
        Insert insert = QueryBuilder.insertInto("freeshr", "enc_by_catchment")
                .value("encounter_id", encounterId).value("division_id", divisionId)
                .value("district_id", districtId)
                .value("year", Calendar.getInstance().get(Calendar.YEAR))
                .value("created_at", TimeUuidUtil.uuidForDate(createdAt));
        cqlOperations.execute(insert);
    }

    private void assertEncounter(EncounterBundle encounterBundle, String encounterId, String healthId, String content) {
        assertEquals(encounterId, encounterBundle.getEncounterId());
        assertEquals(healthId, encounterBundle.getHealthId());
        assertEquals(content, encounterBundle.getEncounterContent());
    }

    @After
    public void tearDown(){
        cqlOperations.execute("truncate encounter");
        cqlOperations.execute("truncate enc_by_catchment");
        cqlOperations.execute("truncate enc_by_patient");
    }
}
