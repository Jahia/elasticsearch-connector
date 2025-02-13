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
 *     Copyright (C) 2002-2020 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.elasticsearchconnector.connection;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.databaseConnector.connection.AbstractConnection;
import org.jahia.modules.databaseConnector.connection.AbstractDatabaseConnectionRegistry;
import org.jahia.modules.databaseConnector.services.DatabaseConnectionRegistry;
import org.jahia.modules.databaseConnector.services.DatabaseConnectorService;
import org.jahia.modules.databaseConnector.util.Utils;
import org.jahia.modules.elasticsearchconnector.ESConstants;
import org.jahia.modules.elasticsearchconnector.api.ECApi;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.settings.SettingsBean;
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
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.QueryResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.jahia.modules.elasticsearchconnector.connection.ElasticSearchConnection.NODE_TYPE;

/**
 * 2017-05-17
 *
 * @author Astrit Ademi
 */
@Component(service = {ElasticSearchConnectionRegistry.class, DatabaseConnectionRegistry.class}, immediate = true)
public class ElasticSearchConnectionRegistry extends AbstractDatabaseConnectionRegistry<ElasticSearchConnection> {

    private static Logger logger = LoggerFactory.getLogger(ElasticSearchConnectionRegistry.class);

    static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[\\w]+[\\w\\-]+[\\w]+$");
    private static final long RETRY_INTERVAL_MS = 1000; // 1 second
    private static final long MAX_RETRIES_COUNT = 60; // wait for about one minute at most

    private DatabaseConnectorService databaseConnectorService = null;

    /**
     * Instantiate a new Registry
     */
    public ElasticSearchConnectionRegistry() {
        super();
    }

    /**
     * Activation method called when bundle is activated
     *
     * @param context BundleContext of teh current OSGI context
     */
    @Activate
    public void activate(BundleContext context) {
        this.context = context;
        this.setConnectorProperties(this.context.getBundle().getSymbolicName(), ElasticSearchConnectionRegistry.class.getName());
        this.populateRegistry();
        this.registerServices();
    }

    /**
     * Deactivation method called when bundle is deactivated
     *
     * @param context BundleContext of teh current OSGI context
     */
    @Deactivate
    public void deactivate(BundleContext context) {
        this.closeConnections();
    }

    /**
     * Inject Database Connector service
     *
     * @param databaseConnectorService the injected service
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, service = DatabaseConnectorService.class)
    public void setDatabaseConnectorService(DatabaseConnectorService databaseConnectorService) {
        this.databaseConnectorService = databaseConnectorService;
    }

    @Override
    public Map<String, ElasticSearchConnection> populateRegistry() {
        JCRCallback<Boolean> callback = session -> {
            QueryResult queryResult = getNodeTypes(session);
            NodeIterator it = queryResult.getNodes();
            while (it.hasNext()) {
                JCRNodeWrapper connectionNode = (JCRNodeWrapper) it.next();
                ElasticSearchConnection connection = (ElasticSearchConnection) nodeToConnection(connectionNode);
                registry.put(connection.getId(), connection);
            }
            return true;
        };
        try {
            jcrTemplate.doExecuteWithSystemSession(callback);
        } catch (RepositoryException e) {
            logger.error("Unable to populate the registry", e);
        }
        return registry;
    }

    /**
     * Get the node types (as JCR nodes) from the repository.
     * If the server is not a processing server, wait for the cluster to synchronize the node types definition.
     * TODO review this as per <a href="https://github.com/Jahia/jahia-private/issues/3394">Race condition when using newly created node types on bundle activation (in clustering env)</a>
     *
     * @param session the JCR session
     * @return the query result
     * @throws RepositoryException if an error occurs
     */
    private static QueryResult getNodeTypes(JCRSessionWrapper session) throws RepositoryException {
        if (SettingsBean.getInstance().isProcessingServer()) {
            // on a processing server, the node type should be available at this point
            return query(session);
        }
        int retriesCount = 0;
        while (retriesCount < MAX_RETRIES_COUNT) {
            try {
                return query(session);
            } catch (NoSuchNodeTypeException e) {
                logger.warn("Node type {} not available ({}), retrying in {} ms (attempt {}/{}) waiting for the cluster to synchronize the node types definition...", ElasticSearchConnection.NODE_TYPE, e.getMessage(), RETRY_INTERVAL_MS, retriesCount, MAX_RETRIES_COUNT);
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread was interrupted during retry sleep", ie);
                }
            }
            retriesCount++;
        }
        throw new RepositoryException("Failed to get node types after " + MAX_RETRIES_COUNT + " retries");
    }

    private static QueryResult query(JCRSessionWrapper session) throws RepositoryException {
        return Utils.query("SELECT * FROM [" + ElasticSearchConnection.NODE_TYPE + "]", session);
    }

    @Override
    protected boolean beforeAddEditConnection(AbstractConnection connection, boolean isEdition) {
        return true;
    }

    @Override
    protected void storeAdvancedConfig(AbstractConnection connection, JCRNodeWrapper node) throws RepositoryException {
        // Historically bad interface design will be fixed in next Major overhaul
    }

    @Override
    protected String getConnectionNodeType() {
        return NODE_TYPE;
    }

    @Override
    public void importConnection(Map<String, Object> map) {
        String identifier = (String) map.get(ESConstants.IDENTIFIERKEY);
        try {
            if (!IDENTIFIER_PATTERN.matcher(identifier).matches()) {
                map.put(ESConstants.STATUSKEY, ESConstants.FAILED);
                map.put(ESConstants.STATUS_MESSAGEKEY, "invalidIdentifier");
                //Create instance to be able to parse the options of a failed connection.
                if (map.containsKey(ESConstants.OPTIONSKEY)) {
                    ElasticSearchConnection connection = new ElasticSearchConnection(identifier);
                    map.put(ESConstants.OPTIONSKEY, connection.parseOptions((LinkedHashMap) map.get(ESConstants.OPTIONSKEY)));
                }
            } else if (databaseConnectorService.hasConnection(identifier, (String) map.get("type"))) {
                map.put(ESConstants.STATUSKEY, ESConstants.FAILED);
                map.put(ESConstants.STATUS_MESSAGEKEY, "connectionExists");
                //Create instance to be able to parse the options of a failed connection.
                if (map.containsKey(ESConstants.OPTIONSKEY)) {
                    ElasticSearchConnection connection = new ElasticSearchConnection(identifier);
                    map.put(ESConstants.OPTIONSKEY, connection.parseOptions((LinkedHashMap) map.get(ESConstants.OPTIONSKEY)));
                }
            } else {
                createConnectionFromImportedMap(map, identifier);
            }

        } catch (Exception ex) {
            map.put(ESConstants.STATUSKEY, ESConstants.FAILED);
            map.put(ESConstants.STATUS_MESSAGEKEY, "creationFailed");
            if (map.containsKey(ESConstants.OPTIONSKEY) && map.get(ESConstants.OPTIONSKEY) instanceof LinkedHashMap) {
                ElasticSearchConnection connection = new ElasticSearchConnection(identifier);
                map.put(ESConstants.OPTIONSKEY, connection.parseOptions((LinkedHashMap) map.get(ESConstants.OPTIONSKEY)));
            }
            logger.error("Import of {} failed with {}", new Object[]{identifier, ex.getMessage()}, ex);
        }
    }

    private void createConnectionFromImportedMap(Map<String, Object> map, String identifier) {
        //Create connection object
        ElasticSearchConnection connection = new ElasticSearchConnection(identifier);
        String host = map.containsKey("host") ? (String) map.get("host") : null;
        Integer port = map.containsKey("port") ? Integer.parseInt((String) map.get("port")) : ElasticSearchConnection.DEFAULT_PORT;
        Boolean isConnected = map.containsKey(ESConstants.IS_CONNECTED) && Boolean.parseBoolean((String) map.get(ESConstants.IS_CONNECTED));
        String username = map.containsKey("user") ? (String) map.get("user") : null;
        String options = map.containsKey(ESConstants.OPTIONSKEY) ? connection.parseOptions((LinkedHashMap) map.get(ESConstants.OPTIONSKEY)) : null;
        map.put(ESConstants.OPTIONSKEY, options);

        connection.setHost(host);
        connection.setPort(port);
        connection.isConnected(isConnected);
        connection.setOptions(options);
        connection.setUser(username);

        addEditConnection(connection, false);
        map.put(ESConstants.STATUSKEY, "success");
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
        String reImportKey = "reImport";
        if (jsonConnectionData.has(reImportKey)) {
            result.put(reImportKey, jsonConnectionData.getString(reImportKey));
        }
        if (!jsonConnectionData.has("id") || StringUtils.isEmpty(jsonConnectionData.getString("id"))) {
            missingParameters.put("id");
        }
        if (!jsonConnectionData.has("host") || StringUtils.isEmpty(jsonConnectionData.getString("host"))) {
            missingParameters.put("host");
        }
        if (missingParameters.length() > 0) {
            result.put("connectionStatus", ESConstants.FAILED);
        } else {
            createConnectionFromJSON(result, jsonConnectionData);
        }
        return result;
    }

    private void createConnectionFromJSON(Map<String, Object> result, JSONObject jsonConnectionData) throws JSONException {
        String id = jsonConnectionData.getString("id");
        String host = jsonConnectionData.getString("host");
        Integer port = null;
        try {
            port = jsonConnectionData.getInt("port");
        } catch (JSONException e) { /* Do nothing; default to null */ }
        Boolean isConnected = jsonConnectionData.has(ESConstants.IS_CONNECTED) && jsonConnectionData.getBoolean(ESConstants.IS_CONNECTED);
        String options = jsonConnectionData.has(ESConstants.OPTIONSKEY) ? jsonConnectionData.get(ESConstants.OPTIONSKEY).toString() : null;
        String password = jsonConnectionData.has(ESConstants.CREDKEY) ? jsonConnectionData.getString(ESConstants.CREDKEY) : null;
        String user = jsonConnectionData.has("user") ? jsonConnectionData.getString("user") : null;

        ElasticSearchConnection connection = new ElasticSearchConnection(id);

        connection.setHost(host);
        connection.setPort(port);
        connection.setUser(user);
        connection.isConnected(isConnected);
        if (password != null && password.contains("_ENC")) {
            password = password.substring(0, 32);
            password = EncryptionUtils.passwordBaseDecrypt(password);
        }
        connection.setPassword(password);
        connection.setOptions(options);
        result.put("connectionStatus", "success");
        result.put("connection", connection);
    }

    @Override
    public Map<String, Object> prepareConnectionMapFromConnection(AbstractConnection connection) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", connection.getId());
        result.put("host", connection.getHost());
        result.put(ESConstants.IS_CONNECTED, connection.isConnected());
        result.put("databaseType", connection.getDatabaseType());
        result.put(ESConstants.OPTIONSKEY, connection.getOptions());
        if (!StringUtils.isEmpty(connection.getPassword())) {
            result.put(ESConstants.CREDKEY, EncryptionUtils.passwordBaseEncrypt(connection.getPassword()) + "_ENC");

        }
        result.put("user", connection.getUser());
        return result;
    }

    @Override
    public AbstractConnection nodeToConnection(JCRNodeWrapper connectionNode) throws RepositoryException {
        //TODO why do we call this method a setter when it is not setting anything and is there a way we can simplify "isMandatory" mechanism???
        String id = setStringConnectionProperty(connectionNode, ElasticSearchConnection.ID_PROPERTY, true);
        String host = setStringConnectionProperty(connectionNode, ElasticSearchConnection.HOST_PROPERTY, true);
        String connectionType = setStringConnectionProperty(connectionNode, ElasticSearchConnection.DATABASE_TYPE_PROPETRY, true);
        Integer port = setIntegerConnectionProperty(connectionNode, ElasticSearchConnection.PORT_PROPERTY, true);
        Boolean isConnected = setBooleanConnectionProperty(connectionNode, ElasticSearchConnection.IS_CONNECTED_PROPERTY);
        String options = setStringConnectionProperty(connectionNode, ElasticSearchConnection.OPTIONS_PROPERTY, false);
        String password = decodePassword(connectionNode, ElasticSearchConnection.PASSWORD_PROPERTY);
        String user = setStringConnectionProperty(connectionNode, ElasticSearchConnection.USER_PROPERTY, false);
        ElasticSearchConnection storedConnection = new ElasticSearchConnection(id);
        storedConnection.setOldId(id);
        storedConnection.setHost(host);
        storedConnection.setPort(port);
        storedConnection.isConnected(isConnected);
        storedConnection.setOptions(options);
        storedConnection.setDatabaseType(connectionType);
        storedConnection.setUser(user);
        storedConnection.setPassword(password);
        return storedConnection;
    }
}
