package org.sharedhealth.freeshrUpdate.shrUpdate;

import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Update;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.mothers.PatientUpdateMother;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
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
    public void shouldCreateUpdateQueryForConfidential() throws Exception {
        when(configuration.getCassandraKeySpace()).thenReturn("foo");
        PatientUpdate patientUpdate = PatientUpdateMother.confidentialPatient();
        Statement update = new PatientUpdateQuery(configuration).get(patientUpdate);
        assertTrue(update.toString().contains("SET confidential='YES'"));
    }

}