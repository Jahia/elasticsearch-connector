package org.jahia.modules.elasticsearchconnector.rest;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.jahia.settings.SettingsBean;

import java.io.IOException;

/**
 * Implementation of ElasticRestHighLevelClient
 */
public class ElasticRestHighLevelClientImpl implements ElasticRestHighLevelClient {
    private RestHighLevelClient client;

    /**
     * Instantiate new wrapper around the specified client
     * @param client the client to wrap
     */
    public ElasticRestHighLevelClientImpl(RestHighLevelClient client) {
        this.client = client;
    }

    @Override
    public RestHighLevelClient getClient() {
        return client;
    }

    @Override
    public String performRequest(Request request) throws IOException {
        if (request.getMethod().equalsIgnoreCase("GET")) {
            Response get = getClient().getLowLevelClient().performRequest(request);
            return EntityUtils.toString(get.getEntity());
        } else {
            throw new IOException("Only GET methods are supported");
        }
    }

    @Override
    public String getEnvironmentPrefix() {
        return SettingsBean.getInstance().getPropertiesFile().getProperty("elasticsearch.prefix");
    }
}
