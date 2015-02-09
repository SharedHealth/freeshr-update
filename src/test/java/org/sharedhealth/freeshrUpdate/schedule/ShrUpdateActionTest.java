package org.sharedhealth.freeshrUpdate.schedule;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.atomFeed.MciFeedProcessor;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ShrUpdateActionTest {

    @Mock
    MciFeedProcessor mciFeedProcessor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldRunUpdate() throws Exception {
        new ShrUpdateAction(mciFeedProcessor).onNext(1L);
        verify(mciFeedProcessor).pullLatest();

    }
}