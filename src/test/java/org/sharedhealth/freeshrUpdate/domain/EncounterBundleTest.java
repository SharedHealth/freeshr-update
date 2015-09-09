package org.sharedhealth.freeshrUpdate.domain;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class EncounterBundleTest {

    @Test
    public void shouldAssociateAnEncounterBundleWithANewHealthId() throws Exception {
        EncounterBundle bundle = new EncounterBundle("E1", "P1", "Patient P1's encounter content.\"Patient P1.json\"", new Date());

        bundle.associateTo("P2");

        assertEquals("Patient P2's encounter content.\"Patient P2.json\"", bundle.getEncounterContent());
        assertEquals("P2", bundle.getHealthId());

    }
}