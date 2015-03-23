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
import org.sharedhealth.freeshrUpdate.repository.EncounterRepository;
import org.sharedhealth.freeshrUpdate.repository.PatientRepository;
import org.sharedhealth.freeshrUpdate.utils.FileUtil;
import rx.Observable;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PatientUpdateEventWorkerTest {
    private static final String ATOMFEED_MEDIA_TYPE = "application/vnd.atomfeed+xml";

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private EncounterRepository encounterRepository;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldDeserializePatientUpdates() throws Exception {
        when(patientRepository.applyUpdate(any(PatientUpdate.class))).thenReturn(Observable.just(true));
        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_with_confidential.txt"));
        entry.setPublished(new Date());
        new PatientUpdateEventWorker(patientRepository, encounterRepository).process(new Event(entry));

        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);
        verify(patientRepository).applyUpdate(captor.capture());

        PatientUpdate patientUpdate = captor.getValue();
        assertEquals("11124262168", patientUpdate.getHealthId());
        assertTrue(patientUpdate.hasConfidentialChange());
        PatientData patientData = patientUpdate.getChangeSet();
        AddressData presentAddress = patientData.getAddressChange();
        assertEquals("new address", presentAddress.getAddressLine());
    }

    @Test
    public void shouldUpdateEncounterWhenPatientConfidentialityIsChanged() throws Exception {
        when(patientRepository.applyUpdate(any(PatientUpdate.class))).thenReturn(Observable.just(true));
        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_with_confidential.txt"));
        entry.setPublished(new Date());
        new PatientUpdateEventWorker(patientRepository, encounterRepository).process(new Event(entry));

        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);
        verify(patientRepository).applyUpdate(captor.capture());

        PatientUpdate patientUpdate = captor.getValue();
        verify(encounterRepository).applyUpdate(patientUpdate);

        assertEquals("11124262168", patientUpdate.getHealthId());
        assertTrue(patientUpdate.hasConfidentialChange());
        PatientData patientData = patientUpdate.getChangeSet();
        AddressData presentAddress = patientData.getAddressChange();
        assertEquals("new address", presentAddress.getAddressLine());
    }

    @Test
    public void shouldNotUpdateEncounterWhenPatientConfidentialityIsNotChanged() throws Exception {
        when(patientRepository.applyUpdate(any(PatientUpdate.class))).thenReturn(Observable.just(true));
        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_without_confidential.txt"));
        entry.setPublished(new Date());
        new PatientUpdateEventWorker(patientRepository, encounterRepository).process(new Event(entry));

        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);
        verify(patientRepository).applyUpdate(captor.capture());
        verify(encounterRepository, never()).applyUpdate(any(PatientUpdate.class));
    }

    @Test
    public void shouldNotUpdateEncounterWhenPatientUpdateFails() throws Exception {
        when(patientRepository.applyUpdate(any(PatientUpdate.class))).thenReturn(Observable.just(false));
        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_with_confidential.txt"));
        entry.setPublished(new Date());
        new PatientUpdateEventWorker(patientRepository, encounterRepository).process(new Event(entry));

        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);
        verify(patientRepository).applyUpdate(captor.capture());
        verify(encounterRepository, never()).applyUpdate(any(PatientUpdate.class));
    }


    private List genarateChangeContent(String path) {
        Content content = new Content();
        content.setType(ATOMFEED_MEDIA_TYPE);
        String contents = FileUtil.asString(path);
        content.setValue(contents);
        return Arrays.asList(content);
    }


}