package org.jahia.modules.elasticsearchconnector.rest;

import org.jahia.modules.elasticsearchconnector.connection.ElasticSearchConnection;

public interface ElasticsearchConnectionRegistry {

    public void registerAsService(ElasticSearchConnection connection);
    public void unregisterAsService();
}
