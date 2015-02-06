package org.sharedhealth.freeshrUpdate.atomFeed;

import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;
import org.ict4h.atomfeed.client.repository.AllFeeds;
import org.sharedhealth.freeshrUpdate.client.MciWebClient;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class MciFeeds extends AllFeeds {
    private MciWebClient mciWebClient;

    public MciFeeds(MciWebClient mciWebClient) {
        this.mciWebClient = mciWebClient;
    }

    @Override
    public com.sun.syndication.feed.atom.Feed getFor(URI uri) {
        try {
            String response = mciWebClient.get(uri);
            WireFeedInput input = new WireFeedInput();
            return (Feed) input.build(new StringReader(response));
        } catch (URISyntaxException | IOException | FeedException e) {
            e.printStackTrace();
        }
        return null;
    }

}
