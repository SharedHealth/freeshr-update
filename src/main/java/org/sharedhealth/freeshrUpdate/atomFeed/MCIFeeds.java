package org.sharedhealth.freeshrUpdate.atomFeed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.ict4h.atomfeed.client.repository.AllFeeds;
import org.sharedhealth.freeshrUpdate.client.ConnectionException;
import org.sharedhealth.freeshrUpdate.client.WebClient;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Map;

public class MCIFeeds extends AllFeeds {
    Map<String, String> headers;

    public MCIFeeds(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public com.sun.syndication.feed.atom.Feed getFor(URI uri) {
        WebClient webClient = new WebClient();
        try {
            String response = webClient.get(uri, headers);
            WireFeedInput input = new WireFeedInput();
            return (Feed) input.build(new StringReader(response));
        } catch (IOException | FeedException e) {
            e.printStackTrace();
        }
        return null;
    }

}
