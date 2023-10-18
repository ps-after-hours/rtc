package com.quadmeup;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quadmeup.dto.SinkDto;
import com.quadmeup.dto.SinkResponse;
import com.quadmeup.enums.SinkType;

public class SinkTask extends Task implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(SinkTask.class);

    private String url;
    private SinkType sinkType;
    private SampleStorage sampleStorage;
    private String id;
    private ObjectMapper objectMapper;

    public SinkTask(String url, SinkType sinkType, SampleStorage sampleStorage, String id) {
        this.url = url;
        this.sinkType = sinkType;
        this.sampleStorage = sampleStorage;
        this.id = id;
        this.objectMapper = new ObjectMapper();
    }

    private void sink() throws IOException {

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost request = new HttpPost(url);

            SinkDto dto = new SinkDto();
            dto.setId(id);
            dto.setKind(sinkType.toString());

            final HttpEntity entity = new StringEntity(objectMapper.writeValueAsString(dto));
            request.setEntity(entity);

            try (CloseableHttpResponse response = client.execute(request)) {

                if (200 == response.getStatusLine().getStatusCode()) {

                    String responseString = EntityUtils.toString(response.getEntity());
                    SinkResponse sinkResponse = objectMapper.readValue(responseString, SinkResponse.class);

                    if (!"OK".equalsIgnoreCase(sinkResponse.getStatus())) {
                        //It's an error on sink size, let's try a retry
                        throw new IOException();
                    }

                } else {
                    //It's an error, we have to report it up hoping for a retry
                    throw new IOException();
                }

            } finally {
                request.releaseConnection();
            }
            
        }
    }

    @Override
    public void run() {
        try {

            sink();

        } catch (Exception e) {

            //In case of any error we can have a simple retry mechanism by simply putting the id again on the correct list

            if (SinkType.JOINED == sinkType) {
                sampleStorage.addDuplicate(id);
            } else if (SinkType.ORPHANED == sinkType) {
                sampleStorage.add(id);
            } else {
                LOGGER.error("Error while reporting sink", e);
            }
        }


    }
     
}
