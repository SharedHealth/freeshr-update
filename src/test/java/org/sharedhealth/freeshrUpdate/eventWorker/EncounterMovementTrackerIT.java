package org.sharedhealth.freeshrUpdate.eventWorker;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sharedhealth.freeshrUpdate.atomFeed.AtomFeedSpringTransactionManager;
import org.sharedhealth.freeshrUpdate.config.SHREnvironmentMock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfig;
import org.sharedhealth.freeshrUpdate.domain.EncounterBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = ShrUpdateConfig.class)
public class EncounterMovementTrackerIT {

    @Autowired
    EncounterMovementTracker encounterMovementTracker;

    @Autowired
    AtomFeedSpringTransactionManager txManager;

    @After
    public void tearDown() throws SQLException {
        try (Statement stmt = txManager.getConnection().createStatement()) {
            stmt.executeUpdate("delete from encounter_movement_status");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testTrackPatientEncounterMovement() throws Exception {
        List<EncounterBundle> bundles = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            bundles.add(new EncounterBundle("e"+i, "P1", "c"+i, new Date() ));
        }
        List<EncounterBundle> bundleList = encounterMovementTracker.trackPatientEncounterMovement("P1", "P2", bundles);
        Assert.assertEquals(3, bundleList.size());

        encounterMovementTracker.doneMovingEncounter("e1", "P2");
        bundleList = encounterMovementTracker.trackPatientEncounterMovement("P1", "P2", bundles);
        Assert.assertEquals(2, bundleList.size());

        Assert.assertEquals("e2", bundleList.get(0).getEncounterId());
        Assert.assertEquals("e3", bundleList.get(1).getEncounterId());
    }


}