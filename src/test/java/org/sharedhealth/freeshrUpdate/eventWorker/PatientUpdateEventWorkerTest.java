package org.sharedhealth.freeshrUpdate.eventWorker;

import com.sun.syndication.feed.atom.Content;
import com.sun.syndication.feed.atom.Entry;
import org.ict4h.atomfeed.client.domain.Event;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.domain.AddressData;
import org.sharedhealth.freeshrUpdate.domain.PatientData;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.shrUpdate.PatientRepository;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PatientUpdateEventWorkerTest {
    private static final String ATOMFEED_MEDIA_TYPE = "application/vnd.atomfeed+xml";

    @Mock
    PatientRepository patientRepository;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldDeserializePatientUpdates() throws Exception {
        when(patientRepository.applyUpdate(any(PatientUpdate.class))).thenReturn(true);
        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(generateAddressChangeContents());
        entry.setPublished(new Date());
        new PatientUpdateEventWorker(patientRepository).process(new Event(entry));

        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);
        verify(patientRepository).applyUpdate(captor.capture());
        PatientUpdate patientUpdate = captor.getValue();
        assertEquals("5960887819567104001", patientUpdate.getHealthId());
        assertEquals(2015, patientUpdate.getYear());
        PatientData patientData = patientUpdate.getChangeSet();
        AddressData presentAddress = patientData.getPresentAddress();
        assertEquals("Test", presentAddress.getAddressLine());
    }

    private List generateAddressChangeContents() {
        Content content = new Content();
        content.setType(ATOMFEED_MEDIA_TYPE);
        String contents = "<![CDATA[{\"year\":2015,\"eventId\":\"81d534c0-a785-11e4-ad63-6d5f88e0f020\"," +
                "\"healthId\":\"5960887819567104001\"," +
                "\"changeSet\":\"{\\\"present_address\\\":{\\\"address_line\\\":\\\"Test\\\"," +
                "\\\"division_id\\\":\\\"20\\\",\\\"district_id\\\":\\\"19\\\",\\\"upazila_id\\\":\\\"15\\\"," +
                "\\\"country_code\\\":\\\"050\\\"}}\",\"eventTime\":\"2015-01-29T07:07:37.868Z\"," +
                "\"eventTimeAsString\":\"2015-01-29T07:07:37.868Z\"," +
                "\"changeSetMap\":{\"present_address\":{\"address_line\":\"Test\",\"division_id\":\"20\"," +
                "\"district_id\":\"19\",\"upazila_id\":\"15\",\"country_code\":\"050\"}}}]]>";


        content.setValue(contents);
        return Arrays.asList(content);

    }

    private List generateNameChangeContents() {
        Content content = new Content();
        content.setType(ATOMFEED_MEDIA_TYPE);
        String contents = "<![CDATA[{\"year\":2015,\"eventId\":\"2557dbe0-a798-11e4-ad63-6d5f88e0f020\"," +
                "\"healthId\":\"5960887819567104001\",\"changeSet\":\"{\\\"given_name\\\":\\\"updated\\\"}\"," +
                "\"eventTime\":\"2015-01-29T09:21:03.134Z\",\"eventTimeAsString\":\"2015-01-29T09:21:03.134Z\"," +
                "\"changeSetMap\":{\"given_name\":\"updated\"}}]]>";

        content.setValue(contents);
        return Arrays.asList(content);

    }


}