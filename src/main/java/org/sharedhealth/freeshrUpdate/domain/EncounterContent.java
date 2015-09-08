package org.sharedhealth.freeshrUpdate.domain;

public class EncounterContent {

    private String content = null;

    public EncounterContent() {}

    public EncounterContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }
}