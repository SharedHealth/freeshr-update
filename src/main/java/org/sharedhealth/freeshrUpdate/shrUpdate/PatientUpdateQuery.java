package org.sharedhealth.freeshrUpdate.shrUpdate;

import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import org.apache.commons.lang3.StringUtils;
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
    public static final String HEALTH_ID_COLUMN_NAME = "health_id";
    @Autowired
    public ShrUpdateConfiguration configuration;

    public PatientUpdateQuery(ShrUpdateConfiguration configuration) {
        this.configuration = configuration;
    }

    public Statement get(PatientUpdate patientUpdate) {
        PatientData patientData = patientUpdate.getChangeSet();

        Update update = QueryBuilder
                .update(configuration.getCassandraKeySpace(), PATIENT_TABLE_NAME);

        addConfidentialUpdate(patientData, update);
        addAddressLineUpdate(patientData, update);
        addDivisionIdUpdate(patientData, update);
        addDistrictIdUpdate(patientData, update);
        addUpazilaIdUpdate(patientData, update);
        addCityCorporationIdUpdate(patientData, update);
        addUnionOrUrbanWardIdUpdate(patientData, update);

        return update
                .where(eq(HEALTH_ID_COLUMN_NAME, patientUpdate.getHealthId()))
                .enableTracing();
    }

    private void addConfidentialUpdate(PatientData patientData, Update update) {
        if (StringUtils.isBlank(patientData.getConfidential())) return;
        update.with(set("confidential", "YES".equalsIgnoreCase(patientData.getConfidential())));
    }

    private void addAddressLineUpdate(PatientData patientData, Update update) {
        String addressLine = patientData.getAddress().getAddressLine();
        if (StringUtils.isBlank(addressLine)) return;
        update.with(set("address_line", addressLine));
    }

    private void addDivisionIdUpdate(PatientData patientData, Update update) {
        String divisionId = patientData.getAddress().getDivisionId();
        if (StringUtils.isBlank(divisionId)) return;
        update.with(set("division_id", divisionId));
    }

    private void addDistrictIdUpdate(PatientData patientData, Update update) {
        String districtId = patientData.getAddress().getDistrictId();
        if (StringUtils.isBlank(districtId)) return;
        update.with(set("district_id", districtId));
    }

    private void addUpazilaIdUpdate(PatientData patientData, Update update) {
        String upazilaId = patientData.getAddress().getUpazilaId();
        if (StringUtils.isBlank(upazilaId)) return;
        update.with(set("upazila_id", upazilaId));
    }

    private void addCityCorporationIdUpdate(PatientData patientData, Update update) {
        String cityCorporationId = patientData.getAddress().getCityCorporationId();
        if (StringUtils.isBlank(cityCorporationId)) return;
        update.with(set("city_corporation_id", cityCorporationId));
    }

    private void addUnionOrUrbanWardIdUpdate(PatientData patientData, Update update) {
        String unionOrUrbanWardId = patientData.getAddress().getUnionOrUrbanWardId();
        if (StringUtils.isBlank(unionOrUrbanWardId)) return;
        update.with(set("union_urban_ward_id", unionOrUrbanWardId));
    }
}
