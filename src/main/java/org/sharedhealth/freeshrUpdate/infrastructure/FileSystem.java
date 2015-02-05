package org.sharedhealth.freeshrUpdate.infrastructure;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
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

    public boolean createMarkersStore(String markerFilePath, String content) {
        return false;
    }

    public boolean updateMarkersStore(String filePath, Properties markers) {
        return false;
    }
}
