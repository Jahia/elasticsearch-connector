package org.jahia.modules.elasticsearchconnector.connection;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.jahia.modules.databaseConnector.connection.AbstractConnection;
import org.jahia.modules.databaseConnector.connection.ConnectionData;
import org.jahia.modules.elasticsearchconnector.http.ElasticSearchTransportClient;
import org.jahia.utils.EncryptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;

import static org.jahia.modules.databaseConnector.util.Utils.DOUBLE_QUOTE;
import static org.jahia.modules.databaseConnector.util.Utils.NEW_LINE;
import static org.jahia.modules.databaseConnector.util.Utils.TABU;

/**
 * 2017-05-17
 *
 * @author Astrit Ademi
 */

public class ElasticSearchConnection extends AbstractConnection {

    public static final String NODE_TYPE = "ec:elasticsearchConnection";
    public static final Integer DEFAULT_PORT = 9300;
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchConnection.class);
    public static final String CLUSTER_NAME = "ec:clusterName";

    public static final String DEFAULT_CLUSTER_NAME = "elasticsearch";
    public static final String DEFAULT_PING_TIMEOUT = "5s";
    public static final String DEFAULT_NODES_SAMPLER_INTERVAL = "5s";
    public static final boolean DEFAULT_IGNORE_CLUSTER_NAME = true;

    public static final String DATABASE_TYPE = "ELASTICSEARCH";
    public static final String DISPLAY_NAME = "ElasticSearchDB";

    private ElasticSearchTransportClient esTransportClient = null;
    private Settings settings = null;

    private String clusterName = null;


    public ElasticSearchConnection(String id) {
        this.id = id;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    private void prepareSettings() {
        //Add any other settings here
        Settings.Builder builder = Settings.builder();
        builder.put("cluster.name", this.clusterName != null ? this.clusterName : DEFAULT_CLUSTER_NAME);

        if (!StringUtils.isEmpty(options)) {
            try {
                JSONObject jsonOptions = new JSONObject(options);
                builder.put("client.transport.ignore_cluster_name", jsonOptions.has("ignoreClusterName") ? jsonOptions.getBoolean("ignoreClusterName") : DEFAULT_IGNORE_CLUSTER_NAME);
                builder.put("client.transport.ping_timeout", jsonOptions.has("pingTimeout") ? jsonOptions.getInt("pingTimeout") : DEFAULT_PING_TIMEOUT);
                builder.put("client.transport.nodes_sampler_interval", jsonOptions.has("nodesSamplerInterval") ? jsonOptions.getInt("nodesSamplerInterval") : DEFAULT_NODES_SAMPLER_INTERVAL);
            } catch (JSONException ex) {
                logger.warn("Failed to parse options for ElasticSearch connection with id: " + this.id + " " + ex.getMessage());
            }
        }
        settings = builder.build();
    }

    private void addAdditionalTransportClients(ElasticSearchTransportClient estc) {
        //Add any additional transport addresses, that may be specified in advanced options
        if (!StringUtils.isEmpty(options)) {
            try {
                JSONObject jsonOptions = new JSONObject(options);
                if (jsonOptions.has("additionalTransportAddresses")) {
                    JSONArray transportAddressesHolder = jsonOptions.getJSONArray("additionalTransportAddresses");
                    for (int i = 0; i < transportAddressesHolder.length(); i++) {
                        JSONObject transportAddress = new JSONObject(transportAddressesHolder.get(i));
                        String host = transportAddress.getString("host");
                        String port = transportAddress.getString("port");
                        if (StringUtils.isEmpty(port)) {
                            port = String.valueOf(DEFAULT_PORT);
                        }
                        try {
                            if (host != null) {
                                estc.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), Integer.valueOf(port)));
                            }
                        } catch (UnknownHostException ex) {
                            logger.warn("Unable to add additional transport address (" + host + ":" + port + ") for ElasticSearch connection with id: " + this.id + " " + ex.getMessage());
                        }
                    }
                }
            } catch (JSONException ex) {
                logger.warn("Failed to parse options for ElasticSearch connection with id: " + this.id + " " + ex.getMessage());
            }
        }
    }

    private ElasticSearchTransportClient createTransportClient() {
        prepareSettings();
        ElasticSearchTransportClient estc = new ElasticSearchTransportClient(settings);
        try {
            estc.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port != null ? port : DEFAULT_PORT));
        } catch (UnknownHostException ex) {
            logger.error("Failed to add transport address (" + host + ":" + port + ") for ElasticSearch connection with id: " + this.id + " " + ex.getMessage());
        }
        //Add any additional addresses that may be configured in the advanced settings.
        addAdditionalTransportClients(estc);
        return estc;
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
        serializedString.append(TABU).append("type ").append(DOUBLE_QUOTE).append(DATABASE_TYPE).append(DOUBLE_QUOTE).append(NEW_LINE);
        serializedString.append(TABU).append("host ").append(DOUBLE_QUOTE).append(this.host).append(DOUBLE_QUOTE).append(NEW_LINE);
        serializedString.append(TABU).append("identifier ").append(DOUBLE_QUOTE).append(this.id).append(DOUBLE_QUOTE).append(NEW_LINE);
        serializedString.append(TABU).append("isConnected ").append(DOUBLE_QUOTE).append(this.isConnected()).append(DOUBLE_QUOTE).append(NEW_LINE);
        serializedString.append(TABU + "port " + DOUBLE_QUOTE).append(this.port != null ? this.port : DEFAULT_PORT).append(DOUBLE_QUOTE).append(NEW_LINE);
        serializedString.append(TABU + "clusterName " + DOUBLE_QUOTE).append(this.clusterName != null ? this.clusterName : DEFAULT_CLUSTER_NAME).append(DOUBLE_QUOTE).append(NEW_LINE);

        if (this.options != null) {
            try {
                JSONObject jsonOptions = new JSONObject(this.options);
                serializedString.append(TABU + "options {");
                //Handle connection pool settings
                if (jsonOptions.has("transport")) {
                    JSONObject transport = jsonOptions.getJSONObject("transport");
                    serializedString.append(NEW_LINE).append(TABU).append(TABU).append("transport {");
                    if (transport.has("ignore_cluster_name") && !StringUtils.isEmpty(transport.getString("ignore_cluster_name"))) {
                        serializedString.append(NEW_LINE).append(TABU).append(TABU).append(TABU).append("ignore_cluster_name ").append(DOUBLE_QUOTE).append(transport.getString("ignore_cluster_name")).append(DOUBLE_QUOTE);
                    }
                    if (transport.has("pingTimeout") && !StringUtils.isEmpty(transport.getString("pingTimeout"))) {
                        serializedString.append(NEW_LINE).append(TABU).append(TABU).append(TABU).append("pingTimeout ").append(DOUBLE_QUOTE).append(transport.getString("pingTimeout")).append(DOUBLE_QUOTE);
                    }
                    if (transport.has("nodesSamplerInterval") && !StringUtils.isEmpty(transport.getString("nodesSamplerInterval"))) {
                        serializedString.append(NEW_LINE).append(TABU).append(TABU).append(TABU).append("nodesSamplerInterval ").append(DOUBLE_QUOTE).append(transport.getString("nodesSamplerInterval")).append(DOUBLE_QUOTE);
                    }
                    serializedString.append(NEW_LINE).append(TABU).append(TABU).append("}");
                }
                if (jsonOptions.has("additionalTransportAddresses")) {
                    JSONArray ata = jsonOptions.getJSONArray("additionalTransportAddresses");
                    serializedString.append(NEW_LINE).append(TABU).append(TABU).append("additionalTransportAddresses [");
                    for (int i = 0; i < ata.length(); i++) {
                        if (i != 0) {
                            serializedString.append(", ");
                        }
                        JSONObject member = ata.getJSONObject(i);
                        serializedString.append(NEW_LINE).append(TABU).append(TABU).append(TABU).append(DOUBLE_QUOTE).append(member.getString("host")).append(member.has("port") && !StringUtils.isEmpty(member.getString("port")) ? ":" + member.getString("port") : "").append(DOUBLE_QUOTE);
                    }
                    serializedString.append(NEW_LINE).append(TABU).append(TABU).append("]");
                }
                serializedString.append(NEW_LINE).append(TABU).append("}");
            } catch (JSONException ex) {
                logger.error("Failed to parse connection options json", ex.getMessage());
            }
        }

        return serializedString.toString();
    }

    @Override
    public Object beforeRegisterAsService() {
        esTransportClient = createTransportClient();
        return esTransportClient;
    }

    @Override
    public void beforeUnregisterAsService() {
        if (esTransportClient != null) {
            esTransportClient.close();
        }
    }

    @Override
    public boolean testConnectionCreation() {
        ElasticSearchTransportClient estc = null;
        try {
            estc = createTransportClient();
            return estc.testConnection();
        } finally {
            if (estc != null) {
                estc.close();
            }
        }
    }

    @Override
    public ConnectionData makeConnectionData() {
        ElasticSearchConnectionData elasticSearchConnectionData = new ElasticSearchConnectionData(id);
        elasticSearchConnectionData.setHost(host);
        elasticSearchConnectionData.setPort(port == null ? DEFAULT_PORT : port);
        elasticSearchConnectionData.isConnected(isConnected);
        elasticSearchConnectionData.setClusterName(clusterName == null ? DEFAULT_CLUSTER_NAME : clusterName);
        elasticSearchConnectionData.setPassword(password);
        elasticSearchConnectionData.setDatabaseType(DATABASE_TYPE);
        elasticSearchConnectionData.setOptions(options);
        elasticSearchConnectionData.setDisplayName(DISPLAY_NAME);
        return elasticSearchConnectionData;
    }

    @Override
    public Object getServerStatus() {
        Gson gson = new Gson();
        JSONObject obj = null;
        try {
            obj = new JSONObject(gson.toJson(esTransportClient.admin().cluster().prepareClusterStats().get()));
            JSONObject aboutConnection = new JSONObject();
            aboutConnection.put("host", this.host);
            aboutConnection.put("port", this.port);
            aboutConnection.put("dbname", this.dbName);
            aboutConnection.put("id", this.id);
            aboutConnection.put("uri", this.uri);
            obj.put("aboutConnection", aboutConnection);
        } catch (JSONException e) {
            logger.error("Failed to parse connection statistics to response");
        }
        return obj.toString();
    }

    @Override
    public String getNodeType() {
        return NODE_TYPE;
    }

    @Override
    public String parseOptions(LinkedHashMap options) {
        //@TODO implement parsing options
        return null;
    }
}
