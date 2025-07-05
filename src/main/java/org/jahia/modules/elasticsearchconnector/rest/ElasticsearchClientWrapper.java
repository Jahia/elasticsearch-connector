package org.jahia.modules.elasticsearchconnector.rest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;

/**
 * Wrapper for Elasticsearch High Level Rest Client
 */
public interface ElasticsearchClientWrapper {
    ElasticsearchClient getClient() throws ConnectionUnavailableException;
    Rest5Client getRest5Client() throws ConnectionUnavailableException;
    String performRequest(GetRequest request) throws IOException, ConnectionUnavailableException;
    String performRequest(Request request) throws IOException, ParseException, ConnectionUnavailableException;
    ElasticsearchConnection getConnection();

    /**
     *
     * @return the content of the property elasticsearch.prefix, null if not available/defined
     */
    String getEnvironmentPrefix();
}
