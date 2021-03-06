package org.sharedhealth.freeshrUpdate.eventWorker;

import com.sun.syndication.feed.atom.Content;
import com.sun.syndication.feed.atom.Entry;
import org.ict4h.atomfeed.client.domain.Event;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sharedhealth.freeshrUpdate.client.MciWebClient;
import org.sharedhealth.freeshrUpdate.domain.*;
import org.sharedhealth.freeshrUpdate.repository.EncounterRepository;
import org.sharedhealth.freeshrUpdate.repository.PatientRepository;
import org.sharedhealth.freeshrUpdate.utils.FileUtil;
import org.sharedhealth.freeshrUpdate.utils.StringUtils;
import org.sharedhealth.freeshrUpdate.utils.TimeUuidUtil;
import rx.Observable;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.sharedhealth.freeshrUpdate.utils.StringUtils.readFrom;

@RunWith(MockitoJUnitRunner.class)
public class PatientUpdateEventWorkerTest {
    private static final String ATOMFEED_MEDIA_TYPE = "application/vnd.atomfeed+xml";

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private EncounterRepository encounterRepository;
    @Mock
    private MciWebClient mciWebClient;
    @Mock
    private EncounterMovementTracker encounterMovementTracker;


    @Rule
    public ExpectedException thrown= ExpectedException.none();

    @InjectMocks
    private PatientUpdateEventWorker patientUpdateEventWorker = new PatientUpdateEventWorker();

    @Test
    public void shouldDeserializePatientUpdates() throws Exception {
        when(patientRepository.applyUpdate(any(PatientUpdate.class))).thenReturn(Observable.just(true));
        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_with_confidential.txt"));
        entry.setPublished(new Date());
        when(encounterRepository.applyUpdate(any(PatientUpdate.class))).thenReturn(Observable.just(true));
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
        when(encounterRepository.applyUpdate(any(PatientUpdate.class))).thenReturn(Observable.just(true));
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
        when(patientRepository.mergeUpdate(any(PatientUpdate.class))).thenReturn(Observable.just(true));
        Patient p2 = new Patient();
        p2.setHealthId("P2");
        when(patientRepository.fetchPatient("P2")).thenReturn(Observable.just(p2));
        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_for_merge.txt"));
        entry.setPublished(new Date());
        when(patientRepository.mergeUpdate(any(PatientUpdate.class))).thenReturn(Observable.just(true));
        when(encounterRepository.getAllEncounters(anyString())).thenReturn(Observable.from(new ArrayList()));
        patientUpdateEventWorker.process(new Event(entry));

        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);

        verify(patientRepository, times(1)).mergeUpdate(captor.capture());
        PatientUpdate actualUpdateApplied = captor.getValue();
        assertEquals("P1", actualUpdateApplied.getHealthId());
        assertEquals(new HashMap<String, Object>() {{
            put("active", false);
            put("merged_with", "P2");
        }}, actualUpdateApplied.getPatientMergeChanges());

        verify(patientRepository,times(1)).mergeUpdate(actualUpdateApplied);
        verify(patientRepository, never()).applyUpdate(any(PatientUpdate.class));

    }

    @Test
    public void shouldNotUpdateEncountersOfPatientIfPatientToBeMergedNotFound() throws Exception {
        //P1 merged with P2
        //P1 not present on SHR
        String patientResponse = FileUtil.asString("patients/P2.json");
        Patient p2 = readFrom(patientResponse, Patient.class);
        when(patientRepository.fetchPatient("P2")).thenReturn(Observable.just(p2));

        when(patientRepository.fetchPatient("P1")).thenReturn(Observable.<Patient>just(null));

        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_for_merge.txt"));
        entry.setPublished(new Date());

        when(patientRepository.mergeUpdate(any(PatientUpdate.class))).thenReturn(Observable.just(false));
        patientUpdateEventWorker.process(new Event(entry));
        verify(patientRepository, times(1)).mergeUpdate(any(PatientUpdate.class));
    }

    @Test
    public void shouldDownloadPatientToBeMergedWithIfNotPresentToMergePatientEncounters() throws Exception {
        //P1 merged with P2
        //P1 present on SHR
        //P2 not present locally.Encounters are updated only after downloading P2

        when(patientRepository.mergeUpdate(any(PatientUpdate.class))).thenReturn(Observable.just(true));
        when(patientRepository.fetchPatient("P2")).thenReturn(Observable.<Patient>just(null));
        String patientResponse = FileUtil.asString("patients/P2.json");
        when(mciWebClient.getPatient("P2")).thenReturn(patientResponse);
        when(patientRepository.save(any(Patient.class))).thenReturn(Observable.just(true));

        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_for_merge.txt"));
        entry.setPublished(new Date());

        Date receivedAt = new Date();
        UUID receivedAtUuid = TimeUuidUtil.uuidForDate(receivedAt);

        final EncounterBundle p1E1 = new EncounterBundle("E1", "P1", "C1", new Date(), receivedAtUuid);
        when(encounterRepository.getAllEncounters(anyString())).thenReturn(Observable.<List<EncounterBundle>>just(new ArrayList<EncounterBundle>(Arrays.asList(p1E1))));
        // when(encounterRepository.getAllEncounters(anyString())).thenReturn(Observable.<List<EncounterBundle>>just(new ArrayList<EncounterBundle>()));
        //when(encounterRepository.applyMerge(any(PatientUpdate.class), any(Patient.class))).thenReturn(Observable.just(true));

        when(encounterMovementTracker.trackPatientEncounterMovement(anyString(), anyString(), any(List.class))).thenReturn(new ArrayList<EncounterBundle>(Arrays.asList(p1E1)));
        when(encounterRepository.associateEncounterBundleTo(any(EncounterBundle.class), any(Patient.class))).thenReturn(Observable.just(true));

        patientUpdateEventWorker.process(new Event(entry));

        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);

        verify(patientRepository, times(1)).mergeUpdate(captor.capture());
        PatientUpdate actualUpdateApplied = captor.getValue();
        assertEquals("P1", actualUpdateApplied.getHealthId());

        //mciWebClient will be called twice since patientRepository.fetchPatient('P2') returns null
        verify(mciWebClient, times(2)).getPatient("P2");

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository, times(2)).save(patientCaptor.capture());
        Patient expectedPatient = StringUtils.readFrom(patientResponse, Patient.class);
        assertEquals(expectedPatient, patientCaptor.getValue());
        //verify(encounterRepository, times(1)).applyMerge(eq(actualUpdateApplied), eq(patientCaptor.getValue()));
        verify(patientRepository,times(1)).mergeUpdate(actualUpdateApplied);

        ArgumentCaptor<EncounterBundle> bundleArgumentCaptor = ArgumentCaptor.forClass(EncounterBundle.class);
        verify(encounterRepository, times(1)).associateEncounterBundleTo(bundleArgumentCaptor.capture(), any(Patient.class));


        //verify(encounterRepository, times(1)).associateEncounterBundleTo(p1E1, any(Patient.class));
    }


    @Test
    public void shouldNotProcessEncountersIfDownloadOfPatientToBeMergedWithFails() throws Exception {
        //P1 merged with P2
        //P1 present on SHR
        //P2 not present locally.Encounters are not updated when downloading of P2 fails

        when(patientRepository.mergeUpdate(any(PatientUpdate.class))).thenReturn(Observable.just(true));
        when(patientRepository.fetchPatient("P2")).thenReturn(Observable.<Patient>just(null));
        when(mciWebClient.getPatient("P2")).thenThrow(new IOException());

        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_for_merge.txt"));
        entry.setPublished(new Date());
        thrown.expect(RuntimeException.class);

        patientUpdateEventWorker.process(new Event(entry));


        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);
        verify(patientRepository, times(1)).mergeUpdate(captor.capture());
        PatientUpdate actualUpdateApplied = captor.getValue();
        assertEquals("P1", actualUpdateApplied.getHealthId());

        verify(mciWebClient, times(1)).getPatient("P2");
        verify(patientRepository, never()).save(any(Patient.class));

        verify(encounterRepository, never()).applyMerge(any(PatientUpdate.class), any(Patient.class));
    }


    @Test
    public void shouldNotProcessEncountersIfSaveOfPatientToBeMergedWithFails() throws Exception {
        //P1 merged with P2
        //P1 present on SHR
        //P2 not present locally.Encounters are not updated when saving of P2 fails

        when(patientRepository.mergeUpdate(any(PatientUpdate.class))).thenReturn(Observable.just(true));
        when(patientRepository.fetchPatient("P2")).thenReturn(Observable.<Patient>just(null));
        String patientResponse = FileUtil.asString("patients/P2.json");
        when(mciWebClient.getPatient("P2")).thenReturn(patientResponse);
        when(patientRepository.save(any(Patient.class))).thenReturn(Observable.just(false));

        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTitle("foo");
        entry.setContents(genarateChangeContent("feeds/update_feed_for_merge.txt"));
        entry.setPublished(new Date());
        try {
            patientUpdateEventWorker.process(new Event(entry));
        } catch (Exception e) {
            assertTrue("Should have thrown error saying Patient P2 does not exist.", e.getMessage().contains("Active patient [P2] does not exist"));
        }

//        ArgumentCaptor<PatientUpdate> captor = ArgumentCaptor.forClass(PatientUpdate.class);
//
//        verify(patientRepository, times(1)).mergeUpdate(captor.capture());
//        PatientUpdate actualUpdateApplied = captor.getValue();
//        assertEquals("P1", actualUpdateApplied.getHealthId());
//
//        verify(mciWebClient, times(1)).getPatient("P2");
//
//        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
//        verify(patientRepository, times(1)).save(patientCaptor.capture());
//        assertEquals(patientCaptor.getValue(), StringUtils.readFrom(FileUtil.asString("patients/P2.json"), Patient.class));
//
//        verify(encounterRepository, never()).applyMerge(any(PatientUpdate.class), any(Patient.class));

    }

    private List genarateChangeContent(String path) {
        Content content = new Content();
        content.setType(ATOMFEED_MEDIA_TYPE);
        String contents = FileUtil.asString(path);
        content.setValue(contents);
        return Arrays.asList(content);
    }


}