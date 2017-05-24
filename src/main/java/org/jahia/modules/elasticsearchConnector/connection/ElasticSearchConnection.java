package org.jahia.modules.elasticsearchConnector.connection;

import org.apache.commons.lang.StringUtils;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.jahia.modules.databaseConnector.connection.AbstractConnection;
import org.jahia.modules.databaseConnector.connection.ConnectionData;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * 2017-05-17
 *
 * @author Astrit Ademi
 */

public class ElasticSearchConnection extends AbstractConnection {

//    public static final String NODE_TYPE = "dc:elasticsearchConnection";
//    public static final String WRITE_CONCERN_KEY = "dc:writeConcern";
//    public static final String AUTH_DB_KEY = "dc:authDb";
//    public static final String WRITE_CONCERN_DEFAULT_VALUE = "ACKNOWLEDGED";
//    public static final Integer DEFAULT_PORT = 27017;
//
//    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchConnection.class);
    public static final String DATABASE_TYPE = "ELASTIC";
//    public static final String DISPLAY_NAME = "ElasticSearchDB";
//    private static List WRITE_CONCERN_OPTIONS = null;
//
//    private ElasticSearchDatabase databaseConnection;
//
//   private ElasticSearchClient mongoClient;
//
//    private String writeConcern;
//
//    private String authDb;

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
    public String parseOptions(LinkedHashMap options) {
        return null;
    }
}
