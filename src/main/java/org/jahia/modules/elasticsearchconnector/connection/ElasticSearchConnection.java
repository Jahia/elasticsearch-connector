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

import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.client.core.MainResponse;
import org.elasticsearch.client.sniff.ElasticsearchNodesSniffer;
import org.elasticsearch.client.sniff.SniffOnFailureListener;
import org.elasticsearch.client.sniff.Sniffer;
import org.jahia.modules.databaseConnector.connection.AbstractConnection;
import org.jahia.modules.databaseConnector.connection.ConnectionData;
import org.jahia.modules.elasticsearchconnector.ESConstants;
import org.jahia.modules.elasticsearchconnector.rest.ElasticRestHighLevelClient;
import org.jahia.modules.elasticsearchconnector.rest.ElasticRestHighLevelClientImpl;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.settings.SettingsBean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.ConnectException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.jahia.modules.databaseConnector.util.Utils.*;

/**
 * 2017-05-17
 *
 * @author Astrit Ademi
 */
public class ElasticSearchConnection extends AbstractConnection {

    private static final long serialVersionUID = 1;
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchConnection.class);

    static final String NODE_TYPE = "ec:elasticsearchConnection";
    static final Integer DEFAULT_PORT = 9200;
    private static final String DEFAULT_PROTOCOL_SCHEME = "http";
    private static final String SECURE_PROTOCOL_SCHEME = "https";
    private static final int DEFAULT_NODES_SNIFFER_INTERVAL = 0;

    public static final String DATABASE_TYPE = "ELASTICSEARCH";
    static final String DISPLAY_NAME = "ElasticSearchDB";

    private static final char APOSTROPHE = 0x0027;
    private static final long SNIFF_REQUEST_TIMEOUT_MILLIS = 5000L;

    private transient ElasticRestHighLevelClient esRestHighLevelClient;
    private transient Sniffer esSniffer;
    private transient PoolingNHttpClientConnectionManager connectionManager = null;
    private static final String USE_SECURITY_KEY = "useXPackSecurity";
    private static final String NODE_SNIFFER_INTERVAL_KEY = "nodesSnifferInterval";
    private static final String USE_ENCRYPTION_KEY = "useEncryption";
    private static final String ADDITIONAL_HOST_KEY = "additionalHostAddresses";

    /**
     * Constructor
     *
     * @param id identifier for this connection
     */
    public ElasticSearchConnection(String id) {
        this.id = id;
    }

    @Override
    public String getDatabaseType() {
        return DATABASE_TYPE;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getSerializedExportData() {
        StringBuilder serializedString = new StringBuilder();
        appendProperty(serializedString, "type", DATABASE_TYPE);
        appendProperty(serializedString, "host", host);
        appendProperty(serializedString, ESConstants.IDENTIFIERKEY, id);
        appendProperty(serializedString, ESConstants.IS_CONNECTED, isConnected);
        appendProperty(serializedString, "port ", this.port != null ? this.port : DEFAULT_PORT);
        if (this.password != null) {
            appendProperty(serializedString, "user ", this.user);
        }

        if (this.options != null) {
            serializeOptions(serializedString);
        }

        return serializedString.toString();
    }

    private void serializeOptions(StringBuilder serializedString) {
        try {
            JSONObject jsonOptions = new JSONObject(this.options);
            serializedString.append(NEW_LINE).append(TABU).append(ESConstants.OPTIONSKEY).append(" {");
            //Handle connection pool settings
            appendProperty(serializedString, USE_SECURITY_KEY, jsonOptions.has(USE_SECURITY_KEY) && jsonOptions.getBoolean(USE_SECURITY_KEY), 2);
            appendProperty(serializedString, USE_ENCRYPTION_KEY, jsonOptions.has(USE_ENCRYPTION_KEY) && jsonOptions.getBoolean(USE_ENCRYPTION_KEY), 2);

            if (jsonOptions.has(NODE_SNIFFER_INTERVAL_KEY) && !StringUtils.isEmpty(jsonOptions.getString(NODE_SNIFFER_INTERVAL_KEY))) {
                serializedString.append(TABU);
                appendProperty(serializedString, NODE_SNIFFER_INTERVAL_KEY, jsonOptions.getString(NODE_SNIFFER_INTERVAL_KEY), 2);
            }

            if (jsonOptions.has(ADDITIONAL_HOST_KEY)) {
                JSONArray additionalHostAddresses = jsonOptions.getJSONArray(ADDITIONAL_HOST_KEY);
                serializedString.append(NEW_LINE).append(TABU).append(TABU).append(ADDITIONAL_HOST_KEY).append(DOUBLE_QUOTE).append("[");
                for (int i = 0; i < additionalHostAddresses.length(); i++) {
                    if (i != 0) {
                        serializedString.append(", ");
                    }
                    JSONObject address = additionalHostAddresses.getJSONObject(i);
                    appendAddress(serializedString, address);
                }
                serializedString.append("]").append(DOUBLE_QUOTE);
            }
            serializedString.append(NEW_LINE).append(TABU).append("}");
        } catch (JSONException ex) {
            logger.error("Failed to parse connection options json due to: {}", ex.getMessage(), ex);
        }
    }

    private void appendAddress(StringBuilder serializedString, JSONObject address) throws JSONException {
        boolean hasPort = address.has("port") && !StringUtils.isEmpty(address.getString("port"));
        serializedString.append(APOSTROPHE).append(address.getString("host")).append(hasPort ? ":" + address.getString("port") : "").append(APOSTROPHE);
    }

    private void appendProperty(StringBuilder serializedString, String key, Object value) {
        serializedString.append(NEW_LINE).append(TABU).append(key).append(" ").append(DOUBLE_QUOTE).append(value).append(DOUBLE_QUOTE);
    }

    private void appendProperty(StringBuilder serializedString, String key, Object value, int nbTabs) {
        serializedString.append(NEW_LINE);
        for (int i = 0; i < nbTabs; i++) {
            serializedString.append(TABU);
        }
        serializedString.append(key).append(" ").append(DOUBLE_QUOTE).append(value).append(DOUBLE_QUOTE);
    }

    public JSONObject getSerializedExportDataAsJSON() {
        JSONObject connectionExport = new JSONObject();
        try {
            connectionExport.put("type", DATABASE_TYPE);
            connectionExport.put("host", host);
            connectionExport.put("identifier", id);
            connectionExport.put("isConnected", isConnected);
            connectionExport.put("port", port != null ? port : DEFAULT_PORT);
            if (user != null) {
                connectionExport.put("user", user);
            }
            if (options != null) {
                JSONObject optionsJson = new JSONObject(options);
                for (String key : JSONObject.getNames(optionsJson)) {
                    connectionExport.put(key, optionsJson.get(key));
                }
            }
        } catch (JSONException ex) {
            logger.error("Failed to create connection export due to json exception: {}", ex.getMessage(), ex);
        }
        return connectionExport;
    }

    @Override
    public Object beforeRegisterAsService() {
        if(esRestHighLevelClient == null) {
            esRestHighLevelClient = new ElasticRestHighLevelClientImpl(resolveClient(true, false));
        }
        return esRestHighLevelClient;
    }

    @Override
    public void beforeUnregisterAsService() {
        closeUnderlyingConnections();
    }

    private void closeUnderlyingConnections() {
        if (esSniffer != null) {
            esSniffer.close();
            esSniffer = null;
        }
        if (esRestHighLevelClient != null) {
            try {
                esRestHighLevelClient.getClient().close();
                esRestHighLevelClient = null;
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        PoolingNHttpClientConnectionManager manager = getConnectionManager();
        if (manager != null) {
            try {
                if (logger.isInfoEnabled()) {
                    logger.info("Shutting down underlying connection manager,\n statistics {}", manager.getTotalStats());
                }
                manager.shutdown();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            } finally {
                connectionManager = null;
            }
        }
    }

    @Override
    public boolean testConnectionCreation() {
        RestHighLevelClient transportClientService = null;
        try {
            transportClientService = resolveClient(false, true);
            return transportClientService.ping(RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException | ConnectException e) {
            logger.warn("Failed to create/ping connection due to: {}", e.getMessage());
            return false;
        } catch (IOException | ElasticsearchException e) {
            logger.error("Failed to create/ping connection due to: {}", e.getMessage(), e);
            return false;
        } finally {
            if (transportClientService != null) {
                try {
                    transportClientService.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public ConnectionData makeConnectionData() {
        ElasticSearchConnectionData elasticSearchConnectionData = new ElasticSearchConnectionData(id);
        elasticSearchConnectionData.setHost(host);
        elasticSearchConnectionData.setPort(port == null ? DEFAULT_PORT : port);
        elasticSearchConnectionData.isConnected(isConnected);
        elasticSearchConnectionData.setUser(user);
        elasticSearchConnectionData.setDatabaseType(DATABASE_TYPE);
        elasticSearchConnectionData.setOptions(options);
        elasticSearchConnectionData.setDisplayName(DISPLAY_NAME);
        return elasticSearchConnectionData;
    }

    @Override
    public Object getServerStatus() {
        JSONObject connectionData = new JSONObject();
        JSONObject status = new JSONObject();
        JSONObject aboutConnection = new JSONObject();
        try {
            connectionData.put("host", this.host);
            connectionData.put("port", this.port);
            connectionData.put("dbname", this.dbName);
            connectionData.put("user", this.user);
            connectionData.put("id", this.id);
            connectionData.put("uri", this.uri);
            connectionData.put("status", status);
            connectionData.put("aboutConnection", aboutConnection);

            ClusterHealthRequest request = new ClusterHealthRequest();
            ClusterHealthResponse healthResponse = esRestHighLevelClient.getClient().cluster().health(request, RequestOptions.DEFAULT);
            // Compile cluster status information
            status.put("numberOfNodes", healthResponse.getNumberOfNodes());
            status.put("activeShards", healthResponse.getActiveShards());
            status.put("unassignedShards", healthResponse.getUnassignedShards());
            status.put("numberOfPendingTasks", healthResponse.getNumberOfPendingTasks());
            status.put("status", healthResponse.getStatus());

            MainResponse mainResponse = esRestHighLevelClient.getClient().info(RequestOptions.DEFAULT);
            aboutConnection.put("dbVersion", mainResponse.getVersion().getNumber());
            Response get = esRestHighLevelClient.getClient().getLowLevelClient().performRequest(new Request("GET", "/_cluster/stats"));
            String responseBody = EntityUtils.toString(get.getEntity());
            JSONObject jsonObject = new JSONObject(responseBody);
            status.put("statistics", jsonObject);
        } catch (JSONException | IOException e) {
            logger.error("Failed to parse connection statistics to response: {}", e.getMessage(), e);
        }
        return connectionData.toString();
    }

    @Override
    public String getNodeType() {
        return NODE_TYPE;
    }

    @Override
    public String parseOptions(LinkedHashMap options) {
        JSONObject jsonOptions = new JSONObject();
        try {
            if (options.containsKey(NODE_SNIFFER_INTERVAL_KEY)) {
                jsonOptions.put(NODE_SNIFFER_INTERVAL_KEY, options.get(NODE_SNIFFER_INTERVAL_KEY));
            }
            if (options.containsKey(USE_SECURITY_KEY)) {
                jsonOptions.put(USE_SECURITY_KEY, Boolean.valueOf((String) options.get(USE_SECURITY_KEY)));
            }
            if (options.containsKey(USE_ENCRYPTION_KEY)) {
                jsonOptions.put(USE_ENCRYPTION_KEY, Boolean.valueOf((String) options.get(USE_ENCRYPTION_KEY)));
            }
            if (options.containsKey(ADDITIONAL_HOST_KEY)) {
                //Add additional hosts settings
                JSONArray parsedAdditionalHostAddresses = new JSONArray();
                jsonOptions.put(ADDITIONAL_HOST_KEY, parsedAdditionalHostAddresses);
                JSONArray rawAdditionalHostAddresses = new JSONArray((String) options.get(ADDITIONAL_HOST_KEY));
                //Process additional host addresses
                for (int i = 0; i < rawAdditionalHostAddresses.length(); i++) {
                    String address = rawAdditionalHostAddresses.getString(i);
                    JSONObject addressObj = new JSONObject();
                    if (address.contains(":")) {
                        addressObj.put("host", address.substring(0, address.indexOf(':')));
                        addressObj.put("port", address.substring(address.indexOf(':') + 1));
                    } else {
                        addressObj.put("host", address);
                    }
                    parsedAdditionalHostAddresses.put(addressObj);
                }
            }
        } catch (JSONException ex) {
            logger.error("Failed to build json object when parsing options due to: {}", ex.getMessage(), ex);
        }
        return jsonOptions.toString();
    }

    @Override
    public String getPath() {
        return CONNECTION_BASE + "/" + JCRContentUtils.generateNodeName(getId());
    }

    private RestHighLevelClient resolveClient(boolean snifferToBeAdded, boolean testingOnly) {
        List<HttpHost> addresses = new ArrayList<>();
        int snifferInterval = DEFAULT_NODES_SNIFFER_INTERVAL;
        boolean useSSL = false;
        //Add base address
        String protocolScheme = DEFAULT_PROTOCOL_SCHEME;

        if (StringUtils.isNotEmpty(options)) {
            ElasticsearchClientSettings elasticsearchClientSettings = new ElasticsearchClientSettings().init();
            snifferInterval = elasticsearchClientSettings.getSnifferInterval();
            useSSL = elasticsearchClientSettings.isUseSSL();
            protocolScheme = elasticsearchClientSettings.getProtocolScheme();
            addresses.addAll(elasticsearchClientSettings.getAddresses());
        }
        addresses.add(0, new HttpHost(host, port, protocolScheme));

        RestClientBuilder builder = RestClient.builder(Iterables.toArray(addresses, HttpHost.class));
        builder.setRequestConfigCallback(
                requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(30000)
                        .setSocketTimeout(60000 * 5));

        PoolingNHttpClientConnectionManager manager = getConnectionManager();
        if (!testingOnly && manager != null) {
            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setConnectionManager(manager)
                                                                                      .setDefaultIOReactorConfig(IOReactorConfig.custom().setTcpNoDelay(false).build()));
        }
        //If SSL or security is enabled handle the configuration
        if (useSSL || (StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(password))) {
            handleSecurityConfiguration(builder);
        }
        RestHighLevelClient restHighLevelClient = new RestHighLevelClient(builder);
        RestClient lowLevelClient = restHighLevelClient.getLowLevelClient();
        if (snifferToBeAdded && snifferInterval > 0) {
            SniffOnFailureListener sniffOnFailureListener = new SniffOnFailureListener();
            builder.setFailureListener(sniffOnFailureListener);
            //No need for sniffer during test of connection
            ElasticsearchNodesSniffer.Scheme scheme = ElasticsearchNodesSniffer.Scheme.HTTP;
            if (!protocolScheme.equals(DEFAULT_PROTOCOL_SCHEME)) {
                scheme = ElasticsearchNodesSniffer.Scheme.HTTPS;
            }
            ElasticsearchNodesSniffer nodesSniffer = new ElasticsearchNodesSniffer(lowLevelClient, SNIFF_REQUEST_TIMEOUT_MILLIS, scheme);
            esSniffer = Sniffer.builder(lowLevelClient)
                               .setSniffIntervalMillis(snifferInterval)
                               .setNodesSniffer(nodesSniffer)
                               .build();
            sniffOnFailureListener.setSniffer(esSniffer);
        }
        return restHighLevelClient;
    }

    private void handleSecurityConfiguration(RestClientBuilder restClientBuilder) {
        try {
            //Handle SSL
            final SSLIOSessionStrategy sslioSessionStrategy;
            final SSLContext sslContext;
            if (SettingsBean.getInstance().isDevelopmentMode()) {
                //When in development trust own CA and all self-signed certs
                sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
                //Turn off hostname verification in development mode. Default SSL protocol and cipher suites are used.
                sslioSessionStrategy = new SSLIOSessionStrategy(sslContext, NoopHostnameVerifier.INSTANCE);
            } else {
                //Use default jvm truststore
                sslContext = SSLContexts.custom().loadTrustMaterial((TrustStrategy) null).build();
                //Use default SSL protocol, cipher suites and host name verification
                sslioSessionStrategy = new SSLIOSessionStrategy(sslContext);
            }
            //Set credentials for connection
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                                               new UsernamePasswordCredentials(user, password));
            restClientBuilder.setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder
                    .setSSLContext(sslContext)
                    .setSSLStrategy(sslioSessionStrategy)
                    .setDefaultCredentialsProvider(credentialsProvider)
            );
        } catch (GeneralSecurityException ex) {
            logger.error("Failed to configure SSL context when configuring ES Rest Client", ex);
        }
    }

    private PoolingNHttpClientConnectionManager getConnectionManager() {
        if (connectionManager == null) {
            try {
                connectionManager = new PoolingNHttpClientConnectionManager(new DefaultConnectingIOReactor());
                connectionManager.setDefaultMaxPerRoute(10);
                connectionManager.setMaxTotal(30);
            } catch (IOReactorException ex) {
                logger.error("Failed to create connection Manager due to: {}", ex.getMessage(), ex);
            }
        }
        return connectionManager;
    }

    private class ElasticsearchClientSettings {

        private List<HttpHost> addresses;
        private int snifferInterval = DEFAULT_NODES_SNIFFER_INTERVAL;
        private boolean useSSL = false;
        private String protocolScheme = DEFAULT_PROTOCOL_SCHEME;

        ElasticsearchClientSettings() {
            this.addresses = new ArrayList<>();
        }

        int getSnifferInterval() {
            return snifferInterval;
        }

        boolean isUseSSL() {
            return useSSL;
        }

        String getProtocolScheme() {
            return protocolScheme;
        }

        List<HttpHost> getAddresses() {
            return addresses;
        }

        ElasticsearchClientSettings init() {
            try {
                JSONObject optionsJson = new JSONObject(options);
                initProtocolScheme(optionsJson);
                initAdditionalAddresses(optionsJson);
                initSnifferInterval(optionsJson);
            } catch (JSONException ex) {
                logger.error("Failed to parse connection options as json {}", options, ex);
            }
            return this;
        }

        private void initSnifferInterval(JSONObject optionsJson) throws JSONException {
            if (optionsJson.has(NODE_SNIFFER_INTERVAL_KEY)) {
                //Get sniffer interval value and account for seconds time unit if present.
                String nodeInterval = optionsJson.getString(NODE_SNIFFER_INTERVAL_KEY).replace("s", "000");
                try {
                    //If time unit is minutes, multiply by 60000 to determine value in milliseconds
                    snifferInterval = nodeInterval.contains("m") ? Integer.parseInt(nodeInterval.replace("m", "")) * 60000 : Integer.parseInt(nodeInterval);
                } catch (NumberFormatException ex) {
                    logger.error("Failed to parse Sniffer Interval parameter {}", nodeInterval, ex);
                }
            }
        }

        private void initAdditionalAddresses(JSONObject optionsJson) throws JSONException {
            if (optionsJson.has(ADDITIONAL_HOST_KEY)) {
                JSONArray hostAddresses = optionsJson.getJSONArray(ADDITIONAL_HOST_KEY);
                //Build HttpHost for each additional address provided in settings
                for (int i = 0; i < hostAddresses.length(); i++) {
                    JSONObject hostAddress = hostAddresses.getJSONObject(i);
                    int port = hostAddress.has("port") && hostAddress.getString("port") != null ? hostAddress.getInt("port") : DEFAULT_PORT;
                    addresses.add(new HttpHost(hostAddress.getString("host"), port, protocolScheme));
                }
            }
        }

        private void initProtocolScheme(JSONObject optionsJson) throws JSONException {
            if (optionsJson.has(USE_SECURITY_KEY) && optionsJson.getBoolean(USE_SECURITY_KEY)) {
                useSSL = true;
                boolean useEncrytion = optionsJson.has(USE_ENCRYPTION_KEY) && optionsJson.getBoolean(USE_ENCRYPTION_KEY);
                protocolScheme = useEncrytion ? SECURE_PROTOCOL_SCHEME : DEFAULT_PROTOCOL_SCHEME;
            }
        }
    }
}
