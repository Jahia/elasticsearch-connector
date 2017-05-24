package org.jahia.modules.elasticsearchConnector.connection;

import org.jahia.modules.databaseConnector.connection.AbstractConnection;
import org.jahia.modules.databaseConnector.connection.AbstractDatabaseConnectionRegistry;
import org.jahia.modules.databaseConnector.connector.ConnectorMetaData;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * 2017-05-17
 *
 * @author Astrit Ademi
 */

public class ElasticSearchConnectionRegistry extends AbstractDatabaseConnectionRegistry<ElasticSearchConnection> {

    @Override
    public Map<String, ElasticSearchConnection> populateRegistry() {
        return null;
    }

    @Override
    public boolean addEditConnection(AbstractConnection connection, Boolean isEdition) {
        return false;
    }

    @Override
    public void importConnection(Map<String, Object> map) {

    }

    @Override
    public ConnectorMetaData getConnectorMetaData() {
        return null;
    }

    @Override
    public String getConnectionType() {
        return null;
    }

    @Override
    public String getConnectionDisplayName() {
        return null;
    }

    @Override
    public String getEntryPoint() {
        return null;
    }

    @Override
    public Map<String, Object> prepareConnectionMapFromJSON(Map<String, Object> result, JSONObject jsonConnectionData) throws JSONException {
        return null;
    }

    @Override
    public Map<String, Object> prepareConnectionMapFromConnection(AbstractConnection connection) {
        return null;
    }

    @Override
    public Object getDatabase(String connectionId) {
        return null;
    }
}
