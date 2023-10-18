package com.quadmeup;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.quadmeup.dto.DataRecord;
import com.quadmeup.enums.SourceType;
import com.quadmeup.exceptions.MalformedRecordException;
import com.quadmeup.parser.ParserInterface;

public class AcquisitionTask extends Task implements Runnable {

    private SampleStorage sampleStorage;
    private String url;
    private SourceType sourceType;
    private ParserInterface parser;
    private static final Logger LOGGER = LogManager.getLogger(AcquisitionTask.class);

    public AcquisitionTask(final 
        SampleStorage sampleStorage,
        String url,
        SourceType sourceType,
        ParserInterface parser
    ) {
        this.sampleStorage = sampleStorage;
        this.url = url;
        this.sourceType = sourceType;
        this.parser = parser;
    }

    private String read(String url) throws ClientProtocolException, IOException {
        CloseableHttpResponse response = null;

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpGet request = new HttpGet(url);

            response = client.execute(request);

            if (200 == response.getStatusLine().getStatusCode()) {
                //We have a response, let's return it
                //To simplify processing, we treat it as a String
                return EntityUtils.toString(response.getEntity());
            } else if (406 == response.getStatusLine().getStatusCode()) {
                //Endpoint is overloaded, we have to handle this case by informing
                //The TaskProvider that it should not create new jobs for a moment
                sampleStorage.lock(sourceType);
                throw new IOException(url + " returned " + response.getStatusLine().getStatusCode());
            } else {
                //This is not an error to that we process, so we just log it
                LOGGER.error("Error reading from " + url + ". Response code: " + response.getStatusLine().getStatusCode());
                throw new IOException(url + " returned " + response.getStatusLine().getStatusCode());
            }

        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    public void run() {

        try {
            final String response = read(url);

            final DataRecord value = parser.parse(response);

            if ("DONE".equalsIgnoreCase(value.getStatus())) {
                //This source is done, we have to report it
                sampleStorage.markAsDone(sourceType);
            } else {
                boolean state = sampleStorage.add(value.getId());

                if (!state) {
                    //We have a duplicate, we have to report it
                    sampleStorage.addDuplicate(value.getId());

                    //As duplcate appears only 1 in set, we can safely delete it as it will not show up again
                    sampleStorage.delete(value.getId());

                    LOGGER.error("Duplicate found: " + value.getId());
                }
            }

            value.getClass();
        } catch (MalformedRecordException e) {
            LOGGER.error("Error parsing response from " + url);
        } catch (Exception e) {
            LOGGER.error("Error reading from " + url);
        }
        
    }
    
}
