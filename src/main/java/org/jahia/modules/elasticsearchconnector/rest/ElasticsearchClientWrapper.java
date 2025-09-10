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
    /**
     * Returns the Elasticsearch high-level client instance.
     *
     * @return The configured ElasticsearchClient
     * @throws ConnectionUnavailableException If connection to Elasticsearch cannot be established
     */
    ElasticsearchClient getClient() throws ConnectionUnavailableException;

    /**
     * Returns the low-level REST client instance.
     *
     * @return The configured Rest5Client
     * @throws ConnectionUnavailableException If connection to Elasticsearch cannot be established
     */
    Rest5Client getRest5Client() throws ConnectionUnavailableException;

    /**
     * Performs a GET request using the high-level client.
     *
     * @param request The Elasticsearch GET request
     * @return Response string from Elasticsearch
     * @throws IOException If there's an error executing the request
     * @throws ConnectionUnavailableException If connection to Elasticsearch cannot be established
     */
    String performRequest(GetRequest request) throws IOException, ConnectionUnavailableException;

    /**
     * Performs a request using the low-level client. Only GET methods are supported.
     *
     * @param request The low-level REST request
     * @return Response string from Elasticsearch
     * @throws IOException If there's an error executing the request
     * @throws ParseException If response cannot be parsed
     * @throws ConnectionUnavailableException If connection to Elasticsearch cannot be established
     */
    String performRequest(Request request) throws IOException, ParseException, ConnectionUnavailableException;

    /**
     * Returns the current Elasticsearch connection configuration.
     *
     * @return The current ElasticsearchConnection instance
     */
    ElasticsearchConnection getConnection();

    /**
     *  Returns prefix for index names
     *
     * @return the content of the property elasticsearch.prefix, null if not available/defined
     */
    String getEnvironmentPrefix();
}
