package org.sharedhealth.freeshrUpdate.infrastructure;

import org.sharedhealth.freeshrUpdate.config.ShrUpdateProperties;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Properties;

public class MarkerStore {

    @Autowired
    ShrUpdateProperties properties;

    @Autowired
    FileSystem fileSystem;

    public MarkerStore(ShrUpdateProperties properties, FileSystem fileSystem) {
        this.properties = properties;
        this.fileSystem = fileSystem;
    }

    public Object getLastMCIMarker() throws IOException {
        String markerFilePath = properties.getMarkerFilePath();
        createMarkerFileIfAbsent(markerFilePath);

        Properties markers = this.fileSystem.readMarkers(markerFilePath);
        return markers.get(ShrUpdateProperties.MCI_MARKER);
    }

    public void updateMCIMarker(String mciMarker) throws IOException {
        String markerFilePath = properties.getMarkerFilePath();
        createMarkerFileIfAbsent(markerFilePath);
        Properties markers = fileSystem.readMarkers(markerFilePath);
        markers.put(ShrUpdateProperties.MCI_MARKER, mciMarker);
        fileSystem.updateMarkersStore(markerFilePath, markers);
    }


    private void createMarkerFileIfAbsent(String markerFilePath) {
        if (!fileSystem.fileExists(markerFilePath))
            fileSystem.createMarkersStore(markerFilePath, getDefaultMarkerStoreContent());
    }

    private String getDefaultMarkerStoreContent() {
        return ShrUpdateProperties.MCI_MARKER + "=";
    }
}
