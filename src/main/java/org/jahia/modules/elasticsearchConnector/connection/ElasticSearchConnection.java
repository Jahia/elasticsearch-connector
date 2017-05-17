package org.jahia.modules.elasticsearchConnector.connection;

import org.jahia.modules.databaseConnector.connection.AbstractConnection;
import org.jahia.modules.databaseConnector.connection.ConnectionData;

import java.util.LinkedHashMap;

/**
 * 2017-05-17
 *
 * @author Astrit Ademi
 */

public class ElasticSearchConnection extends AbstractConnection {

    @Override
    protected Object beforeRegisterAsService() {
        return null;
    }

    @Override
    public void beforeUnregisterAsService() {

    }

    @Override
    public boolean testConnectionCreation() {
        return false;
    }

    @Override
    public String getDatabaseType() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getSerializedExportData() {
        return null;
    }

    @Override
    public ConnectionData makeConnectionData() {
        return null;
    }

    @Override
    public Object getServerStatus() {
        return null;
    }

    @Override
    public String parseOptions(LinkedHashMap options) {
        return null;
    }
}
