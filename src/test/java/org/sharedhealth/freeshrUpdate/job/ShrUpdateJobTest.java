package org.sharedhealth.freeshrUpdate.job;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.atomFeed.MciFeedProcessor;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ShrUpdateJobTest {

    @Mock
    MciFeedProcessor mciFeedProcessor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldRunUpdate() throws Exception {
        new ShrUpdateJob(mciFeedProcessor).start();
        verify(mciFeedProcessor).pullLatest();

    }
}