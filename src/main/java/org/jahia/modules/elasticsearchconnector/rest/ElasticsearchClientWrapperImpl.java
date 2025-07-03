package org.jahia.modules.elasticsearchconnector.rest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Response;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jahia.settings.SettingsBean;

import java.io.IOException;

/**
 * Implementation of ElasticRestHighLevelClient
 */
public class ElasticsearchClientWrapperImpl implements ElasticsearchClientWrapper {
    private ElasticsearchClient client;
    private Rest5Client rest5Client;

    /**
     * Instantiate new wrapper around the specified client
     * @param client the client to wrap
     */
    public ElasticsearchClientWrapperImpl(ElasticsearchClient client, Rest5Client rest5Client) {
        this.client = client;
        this.rest5Client = rest5Client;
    }

    @Override
    public ElasticsearchClient getClient() {
        return client;
    }

    @Override
    public Rest5Client getRest5Client() {
        return rest5Client;
    }

    @Override
    public String performRequest(GetRequest request) throws IOException {
        return getClient().get(request).toString();
    }

    @Override
    public String performRequest(Request request) throws IOException, ParseException {
        if (request.getMethod().equalsIgnoreCase("GET")) {
            Response get = getRest5Client().performRequest(request);
            return EntityUtils.toString(get.getEntity());
        } else {
            throw new IOException("Only GET methods are supported for low level client");
        }
    }

    @Override
    public String getEnvironmentPrefix() {
        return SettingsBean.getInstance().getPropertyValue("elasticsearch.prefix");
    }
}
