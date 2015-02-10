package org.sharedhealth.freeshrUpdate.atomFeed;

import com.sun.syndication.feed.atom.Feed;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.client.MciWebClient;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.freeshrUpdate.helpers.ResourceHelper.asString;

public class MciPatientUpdateFeedsTest {

    @Mock
    MciWebClient mciWebClient;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldReadResponseAsFeed() throws Exception {
        URI feedUri = URI.create("foo");
        when(mciWebClient.get(feedUri)).thenReturn(asString("feeds/patientUpdatesFeed.xml"));
        MciPatientUpdateFeeds mciPatientUpdateFeeds = new MciPatientUpdateFeeds(mciWebClient);
        Feed feed = mciPatientUpdateFeeds.getFor(feedUri);
        assertEquals("a4b002e6-dbef-4b97-a6d2-72339838a0e3", feed.getId());
    }
}