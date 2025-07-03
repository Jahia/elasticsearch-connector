package org.jahia.modules.elasticsearchconnector.rest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.elasticsearch.client.Request;
import org.jahia.modules.databaseConnector.services.ConnectionService;

import java.io.IOException;

/**
 * Wrapper for Elasticsearch High Level Rest Client
 */
public interface ElasticsearchClientWrapper extends ConnectionService {
    ElasticsearchClient getClient();
    String performRequest(Request request) throws IOException;

    /**
     *
     * @return the content of the property elasticsearch.prefix, null if not available/defined
     */
    String getEnvironmentPrefix();
}
