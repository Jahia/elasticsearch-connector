/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2025 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.elasticsearchconnector.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;

/**
 * Helper class to instantiate and organize clients
 */
class ElasticsearchClients {

    private final ElasticsearchClient elasticsearchClient;
    private final Rest5Client rest5Client;

    private ElasticsearchClients(ElasticsearchClient elasticsearchClient, Rest5Client rest5Client) {
        this.elasticsearchClient = elasticsearchClient;
        this.rest5Client = rest5Client;
    }

    public ElasticsearchClient getElasticsearchClient() {
        return elasticsearchClient;
    }

    public Rest5Client getRest5Client() {
        return rest5Client;
    }

    /**
     * Creates the REST5 client and elasticsearch client from the given Rest5ClientBuilder
     * @return ElasticsearchClients instance with elasticsearch and REST5 clients
     */
    public static ElasticsearchClients build(Rest5ClientBuilder rest5ClientBuilder) {
        Rest5Client restClient = rest5ClientBuilder.build();
        ElasticsearchTransport transport = new Rest5ClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchClient esClient = new ElasticsearchClient(transport);
        return new ElasticsearchClients(esClient, restClient);
    }
}
