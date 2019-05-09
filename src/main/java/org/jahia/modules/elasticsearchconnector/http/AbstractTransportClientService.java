package org.jahia.modules.elasticsearchconnector.http;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.jahia.modules.databaseConnector.services.ConnectionService;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Abstract implementation that contains code that is common between the two TransportClientService implementations
 */
public abstract class AbstractTransportClientService implements TransportClientService, ConnectionService {

    TransportClient transportClient = null;

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
        return transportClient;
    }

    public void close() {
        if (transportClient != null) {
            transportClient.close();
        }
        transportClient = null;
    }

}
