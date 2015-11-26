package org.sharedhealth.freeshrUpdate.eventWorker;

import com.sun.syndication.feed.atom.Content;
import com.sun.syndication.feed.atom.Entry;
import org.ict4h.atomfeed.client.domain.Event;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.client.MciWebClient;
import org.sharedhealth.freeshrUpdate.domain.Address;
import org.sharedhealth.freeshrUpdate.domain.Patient;
import org.sharedhealth.freeshrUpdate.domain.PatientChangeSet;
import org.sharedhealth.freeshrUpdate.domain.PatientUpdate;
import org.sharedhealth.freeshrUpdate.repository.EncounterRepository;
import org.sharedhealth.freeshrUpdate.repository.PatientRepository;
import org.sharedhealth.freeshrUpdate.utils.FileUtil;
import org.sharedhealth.freeshrUpdate.utils.StringUtils;
import rx.Observable;

import java.io.IOException;
import java.util.*;

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
    @Mock
    private MciWebClient mciWebClient;

    @InjectMocks
    private PatientUpdateEventWorker patientUpdateEventWorker = new PatientUpdateEventWorker();

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
        patientUpdateEventWorker.process(new Event(entry));

        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);
        verify(patientRepository).applyUpdate(captor.capture());

        PatientUpdate patientUpdate = captor.getValue();
        assertEquals("11124262168", patientUpdate.getHealthId());
        assertTrue(patientUpdate.hasConfidentialChange());
        PatientChangeSet patientChangeSet = patientUpdate.getChangeSet();
        Address presentAddress = patientChangeSet.getAddressChange();
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
        patientUpdateEventWorker.process(new Event(entry));

        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);
        verify(patientRepository).applyUpdate(captor.capture());

        PatientUpdate patientUpdate = captor.getValue();
        verify(encounterRepository).applyUpdate(patientUpdate);

        assertEquals("11124262168", patientUpdate.getHealthId());
        assertTrue(patientUpdate.hasConfidentialChange());
        PatientChangeSet patientChangeSet = patientUpdate.getChangeSet();
        Address presentAddress = patientChangeSet.getAddressChange();
        assertEquals("new address", presentAddress.getAddressLine());
    }

    @Test
    public void shouldNotUpdateEncounterWhenPatientToBeMergedUpdateFails() throws Exception {
        when(patientRepository.applyUpdate(any(PatientUpdate.class))).thenReturn(Observable.just(false));
        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_with_confidential.txt"));
        entry.setPublished(new Date());
        patientUpdateEventWorker.process(new Event(entry));

        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);
        verify(patientRepository).applyUpdate(captor.capture());
        verify(encounterRepository, never()).applyUpdate(any(PatientUpdate.class));
    }

    @Test
    public void shouldMergeIfBothPatientsArePresentLocally() throws Exception {
        //P1 merged with P2
        //P1,P2 present locally
        when(patientRepository.mergeIfFound(any(PatientUpdate.class))).thenReturn(Observable.just(true));
        when(patientRepository.findPatient("P2")).thenReturn(Observable.just(true));
        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_for_merge.txt"));
        entry.setPublished(new Date());
        patientUpdateEventWorker.process(new Event(entry));

        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);

        verify(patientRepository, times(1)).mergeIfFound(captor.capture());
        PatientUpdate actualUpdateApplied = captor.getValue();
        assertEquals("P1", actualUpdateApplied.getHealthId());
        assertEquals(new HashMap<String, Object>() {{
            put("active", false);
            put("merged_with", "P2");
        }}, actualUpdateApplied.getPatientMergeChanges());


        verify(encounterRepository,times(1)).applyMerge(captor.capture());
        verify(patientRepository, never()).applyUpdate(any(PatientUpdate.class));

    }

    @Test
    public void shouldNotUpdateEncountersOfPatientIfPatientToBeMergedNotFound() throws Exception {
        //P1 merged with P2
        //P1 not present on SHR

        when(patientRepository.mergeIfFound(any(PatientUpdate.class))).thenReturn(Observable.just(false));
        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_for_merge.txt"));
        entry.setPublished(new Date());
        patientUpdateEventWorker.process(new Event(entry));

        verify(patientRepository, times(1)).mergeIfFound(any(PatientUpdate.class));
        verify(encounterRepository,never()).applyMerge(any(PatientUpdate.class));
    }

    @Test
    public void shouldDownloadPatientToBeMergedWithIfNotPresentToMergePatientEncounters() throws Exception {
        //P1 merged with P2
        //P1 present on SHR
        //P2 not present locally.Encounters are updated only after downloading P2

        when(patientRepository.mergeIfFound(any(PatientUpdate.class))).thenReturn(Observable.just(true));
        when(patientRepository.findPatient("P2")).thenReturn(Observable.just(false));
        when(mciWebClient.getPatient("P2")).thenReturn(FileUtil.asString("patients/p2.json"));
        when(patientRepository.save(any(Patient.class))).thenReturn(Observable.just(true));

        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_for_merge.txt"));
        entry.setPublished(new Date());
        patientUpdateEventWorker.process(new Event(entry));

        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);

        verify(patientRepository, times(1)).mergeIfFound(captor.capture());
        PatientUpdate actualUpdateApplied = captor.getValue();
        assertEquals("P1", actualUpdateApplied.getHealthId());

        verify(mciWebClient, times(1)).getPatient("P2");

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository, times(1)).save(patientCaptor.capture());
        assertEquals(patientCaptor.getValue(), StringUtils.readFrom(FileUtil.asString("patients/P2.json"), Patient.class));

        verify(encounterRepository, times(1)).applyMerge(eq(actualUpdateApplied));

    }


    @Test
    public void shouldNotProcessEncountersIfDownloadOfPatientToBeMergedWithFails() throws Exception {
        //P1 merged with P2
        //P1 present on SHR
        //P2 not present locally.Encounters are not updated when downloading of P2 fails

        when(patientRepository.mergeIfFound(any(PatientUpdate.class))).thenReturn(Observable.just(true));
        when(patientRepository.findPatient("P2")).thenReturn(Observable.just(false));
        when(mciWebClient.getPatient("P2")).thenThrow(new IOException());

        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_for_merge.txt"));
        entry.setPublished(new Date());
        patientUpdateEventWorker.process(new Event(entry));

        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);
        verify(patientRepository, times(1)).mergeIfFound(captor.capture());
        PatientUpdate actualUpdateApplied = captor.getValue();
        assertEquals("P1", actualUpdateApplied.getHealthId());

        verify(mciWebClient, times(1)).getPatient("P2");
        verify(patientRepository, never()).save(any(Patient.class));
        verify(encounterRepository, never()).applyMerge(any(PatientUpdate.class));
    }


    @Test
    public void shouldNotProcessEncountersIfSaveOfPatientToBeMergedWithFails() throws Exception {
        //P1 merged with P2
        //P1 present on SHR
        //P2 not present locally.Encounters are not updated when saving of P2 fails

        when(patientRepository.mergeIfFound(any(PatientUpdate.class))).thenReturn(Observable.just(true));
        when(patientRepository.findPatient("P2")).thenReturn(Observable.just(false));
        when(mciWebClient.getPatient("P2")).thenReturn(FileUtil.asString("patients/P2.json"));
        when(patientRepository.save(any(Patient.class))).thenReturn(Observable.just(false));

        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_for_merge.txt"));
        entry.setPublished(new Date());
        patientUpdateEventWorker.process(new Event(entry));

        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);

        verify(patientRepository, times(1)).mergeIfFound(captor.capture());
        PatientUpdate actualUpdateApplied = captor.getValue();
        assertEquals("P1", actualUpdateApplied.getHealthId());

        verify(mciWebClient, times(1)).getPatient("P2");

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository, times(1)).save(patientCaptor.capture());
        assertEquals(patientCaptor.getValue(), StringUtils.readFrom(FileUtil.asString("patients/P2.json"), Patient.class));

        verify(encounterRepository, never()).applyMerge(any(PatientUpdate.class));

    }

    private List genarateChangeContent(String path) {
        Content content = new Content();
        content.setType(ATOMFEED_MEDIA_TYPE);
        String contents = FileUtil.asString(path);
        content.setValue(contents);
        return Arrays.asList(content);
    }


}