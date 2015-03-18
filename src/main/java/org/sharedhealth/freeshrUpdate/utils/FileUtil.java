package org.sharedhealth.freeshrUpdate.utils;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import java.io.IOException;

public class FileUtil {
    public static String asString(String path) {
        try {
            return Resources.toString(Resources.getResource(path), Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("File not found", e);
        }
    }
}
