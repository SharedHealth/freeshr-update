package org.sharedhealth.freeshrUpdate.infrastructure;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateProperties;

import java.util.Properties;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class MarkerStoreTest {

    @Mock
    ShrUpdateProperties properties;

    @Mock
    FileSystem fileSystem;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

    }

    @Test
    public void shouldCreateMarkerFileIfNotPresent() throws Exception {
        String filePath = "/opt/foo";
        when(properties.getMarkerFilePath()).thenReturn(filePath);
        when(fileSystem.fileExists(filePath)).thenReturn(false);
        when(fileSystem.createMarkersStore(filePath, ShrUpdateProperties.MCI_MARKER + "=")).thenReturn(true);
        when(fileSystem.readMarkers(filePath)).thenReturn(new Properties());
        Object lastMCIMarker = new MarkerStore(properties, fileSystem).getLastMCIMarker();
        verify(fileSystem, times(1)).fileExists(filePath);
        verify(fileSystem, times(1)).createMarkersStore(filePath, ShrUpdateProperties.MCI_MARKER + "=");
        verify(fileSystem, times(1)).readMarkers(filePath);
    }

    @Test
    public void shouldUpdateMarkerFileIfNotPresent() throws Exception {
        String filePath = "/opt/foo";
        String newMarker = "boom";


        Properties markers = new Properties();
        markers.put(ShrUpdateProperties.MCI_MARKER, "");


        when(properties.getMarkerFilePath()).thenReturn(filePath);
        when(fileSystem.fileExists(filePath)).thenReturn(false);
        when(fileSystem.createMarkersStore(filePath, ShrUpdateProperties.MCI_MARKER + "=")).thenReturn(true);
        when(fileSystem.readMarkers(filePath)).thenReturn(markers);

        new MarkerStore(properties, fileSystem).updateMCIMarker(newMarker);

        verify(fileSystem, times(1)).fileExists(filePath);
        verify(fileSystem, times(1)).createMarkersStore(filePath, ShrUpdateProperties.MCI_MARKER + "=");
        verify(fileSystem, times(1)).readMarkers(filePath);

        verify(fileSystem, times(1)).updateMarkersStore(filePath, markers);
    }
}