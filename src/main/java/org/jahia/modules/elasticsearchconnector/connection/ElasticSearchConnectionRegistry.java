package org.jahia.modules.elasticsearchconnector.connection;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.databaseConnector.connection.AbstractConnection;
import org.jahia.modules.databaseConnector.connection.AbstractDatabaseConnectionRegistry;
import org.jahia.modules.databaseConnector.connection.DatabaseConnectionRegistry;
import org.jahia.modules.databaseConnector.connector.ConnectorMetaData;
import org.jahia.modules.databaseConnector.services.DatabaseConnectorService;
import org.jahia.modules.databaseConnector.util.Utils;
import org.jahia.modules.elasticsearchconnector.api.ECApi;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.utils.EncryptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 2017-05-17
 *
 * @author Astrit Ademi
 */
@Component(service = {ElasticSearchConnectionRegistry.class, DatabaseConnectionRegistry.class}, immediate = true)
public class ElasticSearchConnectionRegistry extends AbstractDatabaseConnectionRegistry<ElasticSearchConnection> {

    private static Logger logger = LoggerFactory.getLogger(ElasticSearchConnectionRegistry.class);

    private DatabaseConnectorService databaseConnectorService = null;

    public ElasticSearchConnectionRegistry() {
        super();
    }

    @Activate
    public void activate(BundleContext context) {
        this.context = context;
        this.setConnectorProperties(this.context.getBundle().getSymbolicName(), ElasticSearchConnectionRegistry.class.getName());
        this.populateRegistry();
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, service = DatabaseConnectorService.class)
    public void setDatabaseConnectorService(DatabaseConnectorService databaseConnectorService) {
        this.databaseConnectorService = databaseConnectorService;
    }

    @Override
    public Map<String, ElasticSearchConnection> populateRegistry() {
        JCRCallback<Boolean> callback = new JCRCallback<Boolean>() {

            public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                QueryResult queryResult = Utils.query("SELECT * FROM [" + ElasticSearchConnection.NODE_TYPE + "]", session);
                NodeIterator it = queryResult.getNodes();
                while (it.hasNext()) {
                    JCRNodeWrapper connectionNode = (JCRNodeWrapper) it.next();
                    String id = setStringConnectionProperty(connectionNode, AbstractConnection.ID_KEY, true);
                    String host = setStringConnectionProperty(connectionNode, AbstractConnection.HOST_KEY, true);
                    Integer port = setIntegerConnectionProperty(connectionNode, AbstractConnection.PORT_KEY, true);
                    Boolean isConnected = setBooleanConnectionProperty(connectionNode, AbstractConnection.IS_CONNECTED_KEY);
                    String clusterName = setStringConnectionProperty(connectionNode, ElasticSearchConnection.CLUSTER_NAME, false);
                    String options = setStringConnectionProperty(connectionNode, AbstractConnection.OPTIONS_KEY, false);
                    ElasticSearchConnection storedConnection = new ElasticSearchConnection(id);
                    storedConnection.setOldId(id);
                    storedConnection.setHost(host);
                    storedConnection.setPort(port);
                    storedConnection.isConnected(isConnected);
                    storedConnection.setClusterName(clusterName);
                    storedConnection.setOptions(options);
                    registry.put(id, storedConnection);
                }
                return true;
            }
        };
        try {
            jcrTemplate.doExecuteWithSystemSession(callback);
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return registry;
    }

    @Override
    protected boolean beforeAddEditConnection(AbstractConnection connection, boolean isEdition) {
        return true;
    }

    @Override
    protected void storeAdvancedConfig(AbstractConnection connection, JCRNodeWrapper node) throws RepositoryException {

    }

    @Override
    public void importConnection(Map<String, Object> map) {
        try {
            if (!ALPHA_NUMERIC_PATTERN.matcher((String)map.get("identifier")).matches()) {
                map.put("status", "failed");
                map.put("statusMessage", "invalidIdentifier");
                //Create instance to be able to parse the options of a failed connection.
                if (map.containsKey("options")) {
                    ElasticSearchConnection connection = new ElasticSearchConnection((String) map.get("identifier"));
                    map.put("options", map.containsKey("options") ? connection.parseOptions((LinkedHashMap) map.get("options")) : null);
                }
            } else if (databaseConnectorService.hasConnection((String) map.get("identifier"), (String) map.get("type"))) {
                map.put("status", "failed");
                map.put("statusMessage", "connectionExists");
                //Create instance to be able to parse the options of a failed connection.
                if (map.containsKey("options")) {
                    ElasticSearchConnection connection = new ElasticSearchConnection((String) map.get("identifier"));
                    map.put("options", map.containsKey("options") ? connection.parseOptions((LinkedHashMap) map.get("options")) : null);
                }
            } else {
                //Create connection object
                ElasticSearchConnection connection = new ElasticSearchConnection((String) map.get("identifier"));
                String host = map.containsKey("host") ? (String) map.get("host") : null;
                Integer port = map.containsKey("port") ? Integer.parseInt((String) map.get("port")) : ElasticSearchConnection.DEFAULT_PORT;
                Boolean isConnected = map.containsKey("isConnected") && Boolean.parseBoolean((String) map.get("isConnected"));
                String clusterName = map.containsKey("clusterName") ? (String) map.get("clusterName") : ElasticSearchConnection.DEFAULT_CLUSTER_NAME;
                String options = map.containsKey("options") ? connection.parseOptions((LinkedHashMap) map.get("options")) : null;
                map.put("options", options);
                String password = (String) map.get("password");

                password = databaseConnectorService.setPassword(map, password);

                connection.setHost(host);
                connection.setPort(port);
                connection.isConnected(isConnected);
                connection.setClusterName(clusterName);
                connection.setPassword(password);
                connection.setOptions(options);

                addEditConnection(connection, false);
                map.put("status", "success");
            }

        } catch (Exception ex) {
            map.put("status", "failed");
            map.put("statusMessage", "creationFailed");
            //try to parse options if the exist otherwise we will just remove them.
            try {
                if (map.containsKey("options")) {
                    ElasticSearchConnection connection = new ElasticSearchConnection((String) map.get("identifier"));
                    map.put("options", map.containsKey("options") ? connection.parseOptions((LinkedHashMap) map.get("options")) : null);
                }
            } catch (Exception e) {
                map.remove("options");
            }
            logger.info("Import " + (map.containsKey("identifier") ? "for connection: '" + map.get("identifier") + "'" : "") + " failed", ex.getMessage(), ex);
        }
    }

    @Override
    public String getConnectionType() {
        return ElasticSearchConnection.DATABASE_TYPE;
    }

    @Override
    public String getConnectionDisplayName() {
        return ElasticSearchConnection.DISPLAY_NAME;
    }

    @Override
    public String getEntryPoint() {
        return ECApi.ENTRY_POINT;
    }

    @Override
    public Map<String, Object> prepareConnectionMapFromJSON(Map<String, Object> result, JSONObject jsonConnectionData) throws JSONException {
        JSONArray missingParameters = new JSONArray();
        if (jsonConnectionData.has("reImport")) {
            result.put("reImport", jsonConnectionData.getString("reImport"));
        }
        if (!jsonConnectionData.has("id") || StringUtils.isEmpty(jsonConnectionData.getString("id"))) {
            missingParameters.put("id");
        }
        if (!jsonConnectionData.has("host") || StringUtils.isEmpty(jsonConnectionData.getString("host"))) {
            missingParameters.put("host");
        }
        if (missingParameters.length() > 0) {
            result.put("connectionStatus", "failed");
        } else {
            String id = jsonConnectionData.getString("id");
            String host = jsonConnectionData.getString("host");
            Integer port = jsonConnectionData.has("port") && !StringUtils.isEmpty(jsonConnectionData.getString("port")) ? jsonConnectionData.getInt("port") : null;
            Boolean isConnected = jsonConnectionData.has("isConnected") && jsonConnectionData.getBoolean("isConnected");
            String clusterName  = jsonConnectionData.has("clusterName") ? jsonConnectionData.getString("clusterName") : ElasticSearchConnection.DEFAULT_CLUSTER_NAME;
            String password = jsonConnectionData.has("password") ? jsonConnectionData.getString("password") : null;
            String options = jsonConnectionData.has("options") ? jsonConnectionData.getString("options") : null;

            ElasticSearchConnection connection = new ElasticSearchConnection(id);

            connection.setHost(host);
            connection.setPort(port);
            connection.isConnected(isConnected);
            connection.setClusterName(clusterName);
            if (password != null && password.contains("_ENC")) {
                password = password.substring(0, 32);
                password = EncryptionUtils.passwordBaseDecrypt(password);
            }
            connection.setPassword(password);
            connection.setOptions(options);
            result.put("connectionStatus", "success");
            result.put("connection", connection);
        }
        return result;
    }

    @Override
    public Map<String, Object> prepareConnectionMapFromConnection(AbstractConnection connection) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", connection.getId());
        result.put("host", connection.getHost());
        result.put("isConnected", connection.isConnected());
        result.put("clusterName", ((ElasticSearchConnection) connection).getClusterName());
        result.put("databaseType", connection.getDatabaseType());
        result.put("options", connection.getOptions());
        if (!StringUtils.isEmpty(connection.getPassword())) {
            result.put("password", EncryptionUtils.passwordBaseEncrypt(connection.getPassword()) + "_ENC");

        }
        return  result;
    }

}
