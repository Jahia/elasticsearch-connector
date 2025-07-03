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

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.cluster.ClusterStatsResponse;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.ElasticsearchNodesSniffer;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.SniffOnFailureListener;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.Sniffer;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.reactor.ssl.SSLIOSession;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.jahia.modules.databaseConnector.connection.AbstractConnection;
import org.jahia.modules.databaseConnector.connection.ConnectionData;
import org.jahia.modules.elasticsearchconnector.ESConstants;
import org.jahia.modules.elasticsearchconnector.rest.ElasticsearchClientWrapper;
import org.jahia.modules.elasticsearchconnector.rest.ElasticsearchClientWrapperImpl;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

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

    private transient ElasticsearchClientWrapper esElasticsearchClientWrapper;
    private transient Sniffer esSniffer;
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
        if(esElasticsearchClientWrapper == null) {
            esElasticsearchClientWrapper = resolveClient(true, false);
        }
        return esElasticsearchClientWrapper;
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
        if (esElasticsearchClientWrapper != null) {
            try {
                esElasticsearchClientWrapper.getClient().close();
                esElasticsearchClientWrapper = null;
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean testConnectionCreation() {
        ElasticsearchClientWrapper transportClientService = null;
        try {
            transportClientService = resolveClient(false, true);
            return transportClientService.getClient().ping().value();
        } catch (ConnectException e) {
            logger.warn("Failed to create/ping connection due to: {}", e.getMessage());
            return false;
        } catch (IOException | ElasticsearchException e) {
            logger.error("Failed to create/ping connection due to: {}", e.getMessage(), e);
            return false;
        } finally {
            if (transportClientService != null) {
                try {
                    transportClientService.getClient().close();
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

            HealthResponse hr = esElasticsearchClientWrapper.getClient().cluster().health();

            // Compile cluster status information
            status.put("numberOfNodes", hr.numberOfNodes());
            status.put("activeShards", hr.activeShards());
            status.put("unassignedShards", hr.unassignedShards());
            status.put("numberOfPendingTasks", hr.numberOfPendingTasks());
            status.put("status", hr.status());

            InfoResponse info = esElasticsearchClientWrapper.getClient().info();
            aboutConnection.put("dbVersion", info.version().number());
            ClusterStatsResponse sr = esElasticsearchClientWrapper.getClient().cluster().stats();
            String responseBody = sr.nodeStats().toString();
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

    private ElasticsearchClientWrapper resolveClient(boolean snifferToBeAdded, boolean testingOnly) {
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
        addresses.add(0, new HttpHost(protocolScheme, host, port));

        Rest5ClientBuilder builder = Rest5Client.builder(addresses.stream().map(HttpHost::toURI).map(u -> {
            try {
                return new URI(u);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()));

        //If SSL or security is enabled handle the configuration
        if (useSSL || (StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(password))) {
            handleSecurityConfiguration(builder);
        }

        Rest5Client restClient = builder.build();
        ElasticsearchTransport transport = new Rest5ClientTransport(
                restClient, new JacksonJsonpMapper());
        ElasticsearchClient esClient = new ElasticsearchClient(transport);

        if (snifferToBeAdded && snifferInterval > 0) {
            SniffOnFailureListener sniffOnFailureListener = new SniffOnFailureListener();
            builder.setFailureListener(sniffOnFailureListener);
            //No need for sniffer during test of connection
            ElasticsearchNodesSniffer.Scheme scheme = ElasticsearchNodesSniffer.Scheme.HTTP;
            if (!protocolScheme.equals(DEFAULT_PROTOCOL_SCHEME)) {
                scheme = ElasticsearchNodesSniffer.Scheme.HTTPS;
            }
            ElasticsearchNodesSniffer nodesSniffer = new ElasticsearchNodesSniffer(restClient, SNIFF_REQUEST_TIMEOUT_MILLIS, scheme);
            esSniffer = Sniffer.builder(restClient)
                               .setSniffIntervalMillis(snifferInterval)
                               .setNodesSniffer(nodesSniffer)
                               .build();
            sniffOnFailureListener.setSniffer(esSniffer);
        }

        return new ElasticsearchClientWrapperImpl(esClient, restClient);
    }

    private void handleSecurityConfiguration(Rest5ClientBuilder restClientBuilder) {
        try {
            //Handle SSL
            // TODO looks like this might have been replaced by SSLConnectionSocketFactory in http5? Is it still necessary?
            //final SSLIOSessionStrategy sslioSessionStrategy;
            final SSLContext sslContext;
            if (SettingsBean.getInstance().isDevelopmentMode()) {
                //When in development trust own CA and all self-signed certs
                sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
                //Turn off hostname verification in development mode. Default SSL protocol and cipher suites are used.
                //sslioSessionStrategy = new SSLIOSessionStrategy(sslContext, NoopHostnameVerifier.INSTANCE);
            } else {
                //Use default jvm truststore
                sslContext = SSLContexts.custom().loadTrustMaterial((TrustStrategy) null).build();
                //Use default SSL protocol, cipher suites and host name verification
                //sslioSessionStrategy = new SSLIOSessionStrategy(sslContext);
            }
            //Set credentials for connection
            String cred = Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
            restClientBuilder.setDefaultHeaders(new Header[]{
                    new BasicHeader("Authorization", "Basic " + cred)
            });
            restClientBuilder.setSSLContext(sslContext);
//            restClientBuilder.setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder
//                    .setSSLContext(sslContext)
//                    .setSSLStrategy(sslioSessionStrategy)
//                    .setDefaultCredentialsProvider(credentialsProvider)
//            );
        } catch (GeneralSecurityException ex) {
            logger.error("Failed to configure SSL context when configuring ES Rest Client", ex);
        }
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
                    int port = hostAddress.optInt("port", DEFAULT_PORT);
                    addresses.add(new HttpHost(protocolScheme, hostAddress.getString("host"), port));
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
