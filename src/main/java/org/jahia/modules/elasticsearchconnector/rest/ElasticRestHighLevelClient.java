package org.jahia.modules.elasticsearchconnector.rest;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestHighLevelClient;
import org.jahia.modules.databaseConnector.services.ConnectionService;

import java.io.IOException;

/**
 * Wrapper for Elasticsearch High Level Rest Client
 */
public interface ElasticRestHighLevelClient extends ConnectionService {
    RestHighLevelClient getClient();
    String performRequest(Request request) throws IOException;
}
