package org.jahia.modules.elasticsearchconnector.rest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.jahia.settings.SettingsBean;

import java.io.IOException;

/**
 * Implementation of ElasticRestHighLevelClient
 */
public class ElasticsearchClientWrapperImpl implements ElasticsearchClientWrapper {
    private ElasticsearchClient client;

    /**
     * Instantiate new wrapper around the specified client
     * @param client the client to wrap
     */
    public ElasticsearchClientWrapperImpl(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public ElasticsearchClient getClient() {
        return client;
    }

    @Override
    public String performRequest(Request request) throws IOException {
//        if (request.getMethod().equalsIgnoreCase("GET")) {
//            Response get = getClient().getLowLevelClient().performRequest(request);
//            return EntityUtils.toString(get.getEntity());
//        } else {
//            throw new IOException("Only GET methods are supported");
//        }
        return "Nothing for now";
    }

    @Override
    public String getEnvironmentPrefix() {
        return SettingsBean.getInstance().getPropertyValue("elasticsearch.prefix");
    }
}
