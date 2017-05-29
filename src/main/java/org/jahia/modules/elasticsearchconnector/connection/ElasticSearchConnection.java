package org.jahia.modules.elasticsearchconnector.connection;

import org.jahia.modules.databaseConnector.connection.AbstractConnection;
import org.jahia.modules.databaseConnector.connection.ConnectionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;

/**
 * 2017-05-17
 *
 * @author Astrit Ademi
 */

public class ElasticSearchConnection extends AbstractConnection {

    public static final String NODE_TYPE = "ec:elasticsearchConnection";
    public static final Integer DEFAULT_PORT = 9200;
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchConnection.class);
    public static final String DATABASE_TYPE = "ELASTICSEARCH";
    public static final String DISPLAY_NAME = "ElasticSearchDB";

    public ElasticSearchConnection(String id) {
        this.id = id;
    }

    @Override
    public Object beforeRegisterAsService() {
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
    public Object getServerStatus() { return null;
//        BsonDocument serverStatusCommand = new BsonDocument()
//                .append("serverStatus", new BsonInt32(1))
//                .append("metrics", new BsonInt32(1))
//                .append("locks", new BsonInt32(0))
//                .append("dbStats", new BsonInt32(1))
//                .append("collStats", new BsonInt32(1));
//        if (!StringUtils.isEmpty(options)) {
//            try {
//                JSONObject jsonOptions = new JSONObject(options);
//                if (jsonOptions.has("repl")) {
//                    serverStatusCommand.append("repl", new BsonInt32(1));
//                    return this.databaseConnection.runCommand(serverStatusCommand);
//                }
//            } catch (JSONException ex) {
//                logger.error("Failed to parse connection options json", ex.getMessage());
//                return null;
//            }
//        }
//        serverStatusCommand.append("repl", new BsonInt32(0));
//
//        return this.databaseConnection.runCommand(serverStatusCommand);
    }

    @Override
    public Object establishConnection() {
        return null;
    }

    @Override
    public void forgetConnection() {

    }

    @Override
    public Object getClient(String connectionId) {
        return null;
    }

    @Override
    public String getNodeType() {
        return null;
    }

    @Override
    public String parseOptions(LinkedHashMap options) {
        return null;
    }
}
