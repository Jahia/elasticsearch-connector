package org.jahia.modules.elasticsearchconnector.http;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.jahia.modules.databaseConnector.services.ConnectionService;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Created by stefan on 2017-05-30.
 */
public class ElasticSearchTransportClient extends PreBuiltTransportClient implements TransportClientService, ConnectionService {

    public ElasticSearchTransportClient(Settings settings, Class<? extends Plugin>... plugins) {
        super(settings, Arrays.asList(plugins));
    }

    @Override
    public JSONObject getStatus() throws JSONException {
        ClusterAdminClient clusterAdminClient = this.admin().cluster();
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
            this.admin().cluster().prepareHealth().get();
        } catch (NoNodeAvailableException ex) {
            connectionValid = false;
        }

        return connectionValid;
    }

    @Override
    public Object getClient() {
        return this;
    }
}
