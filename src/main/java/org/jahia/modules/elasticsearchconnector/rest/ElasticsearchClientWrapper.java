package org.jahia.modules.elasticsearchconnector.rest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.apache.hc.core5.http.ParseException;
import org.jahia.modules.databaseConnector.services.ConnectionService;

import java.io.IOException;

/**
 * Wrapper for Elasticsearch High Level Rest Client
 */
public interface ElasticsearchClientWrapper extends ConnectionService {
    ElasticsearchClient getClient();
    Rest5Client getRest5Client();
    String performRequest(GetRequest request) throws IOException;
    String performRequest(Request request) throws IOException, ParseException;

    /**
     *
     * @return the content of the property elasticsearch.prefix, null if not available/defined
     */
    String getEnvironmentPrefix();
}
