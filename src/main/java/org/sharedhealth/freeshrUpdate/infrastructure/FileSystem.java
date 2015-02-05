package org.sharedhealth.freeshrUpdate.infrastructure;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

@Component
public class FileSystem {
    public Properties readMarkers(String markerFilePath) throws IOException {
        Properties markers = new Properties();
        markers.load(new FileInputStream(markerFilePath));
        return markers;
    }

    public boolean fileExists(String markerFilePath) {
        return new File(markerFilePath).isFile();
    }

    public boolean createMarkersStore(String markerFilePath, String defaultKey) throws IOException {
        Properties markers = new Properties();
        markers.put(defaultKey, "");
        markers.store(new FileOutputStream(markerFilePath), "Saving markers");
        return true;
    }

    public boolean updateMarkersStore(String markerFilePath, Properties markers) throws IOException {
        markers.store(new FileOutputStream(markerFilePath), "Saving markers");
        return true;
    }
}
