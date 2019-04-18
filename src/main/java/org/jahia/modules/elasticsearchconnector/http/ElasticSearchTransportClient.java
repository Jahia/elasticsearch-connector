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
 *     Copyright (C) 2002-2019 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.elasticsearchconnector.http;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.jahia.modules.databaseConnector.services.ConnectionService;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by stefan on 2017-05-30.
 */
public class ElasticSearchTransportClient implements TransportClientService, ConnectionService {

    TransportClient transportClient;

    public ElasticSearchTransportClient(Settings.Builder settingsBuilder, Class<? extends Plugin>... plugins) {
        transportClient = new PreBuiltTransportClient(settingsBuilder.build(), plugins);
    }

    @Override
    public JSONObject getStatus() throws JSONException {
        ClusterAdminClient clusterAdminClient = transportClient.admin().cluster();
        ClusterHealthResponse healths = clusterAdminClient.prepareHealth().get();

        JSONObject status = new JSONObject();
        status.put("clusterName", healths.getClusterName());
        status.put("status", healths.getStatus().name());

        return status;
    }

    @Override
    public boolean testConnection() {
        boolean connectionValid = true;
        try {
            //If we do not through an exception that means the cluster node is available.
            transportClient.admin().cluster().prepareHealth().get();
        } catch (NoNodeAvailableException ex) {
            connectionValid = false;
        }

        return connectionValid;
    }

    @Override
    public TransportClient getTransportClient() {
        return transportClient;
    }

    @Override
    public Object getClient() {
        return this;
    }
}
