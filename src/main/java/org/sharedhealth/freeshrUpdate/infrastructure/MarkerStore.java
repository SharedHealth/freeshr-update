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

    public String getLastMCIMarker() throws IOException {
        String markerFilePath = properties.getMarkerFilePath();
        createMarkerFileIfAbsent(markerFilePath);

        Properties markers = this.fileSystem.readMarkers(markerFilePath);
        return markers.get(ShrUpdateProperties.MCI_MARKER).toString();
    }

    public void updateMCIMarker(String mciMarker) throws IOException {
        String markerFilePath = properties.getMarkerFilePath();
        createMarkerFileIfAbsent(markerFilePath);
        Properties markers = fileSystem.readMarkers(markerFilePath);
        markers.put(ShrUpdateProperties.MCI_MARKER, mciMarker);
        fileSystem.updateMarkersStore(markerFilePath, markers);
    }


    private void createMarkerFileIfAbsent(String markerFilePath) throws IOException {
        if (!fileSystem.fileExists(markerFilePath))
            fileSystem.createMarkersStore(markerFilePath, ShrUpdateProperties.MCI_MARKER);
    }
}
