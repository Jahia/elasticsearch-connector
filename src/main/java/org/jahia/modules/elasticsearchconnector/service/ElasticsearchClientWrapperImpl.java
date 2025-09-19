package org.jahia.modules.elasticsearchconnector.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Response;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.ElasticsearchNodesSniffer;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.SniffOnFailureListener;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.Sniffer;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.jahia.modules.elasticsearchconnector.ESConstants;
import org.jahia.modules.elasticsearchconnector.config.ConnectionConfigException;
import org.jahia.modules.elasticsearchconnector.config.ElasticsearchConfig;
import org.jahia.modules.elasticsearchconnector.config.ElasticsearchConnectionConfig;
import org.jahia.settings.SettingsBean;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Implementation of the ElasticsearchClientWrapper interface that manages connections to Elasticsearch.
 * This class provides both high-level and low-level client access to Elasticsearch, handling connection
 * management, security configuration, and request execution.
 *
 * Key features:
 * - Manages both ElasticsearchClient (high-level) and Rest5Client (low-level) instances
 * - Supports SSL/TLS encryption and X-Pack security
 * - Implements node sniffing for cluster discovery
 * - Handles connection pooling and lifecycle management
 * - Supports development mode with self-signed certificates
 *
 * The wrapper maintains singleton instances of the clients and automatically reconnects
 * when the configuration changes. It also provides thread-safe access to the clients
 * through synchronized methods.
 *
 * @see ElasticsearchClientWrapper
 * @see ElasticsearchConfig
 * @see ElasticsearchConnectionConfig
 */
@Component(service = ElasticsearchClientWrapper.class, immediate = true, property = {
        "databaseType=" + ESConstants.DB_TYPE,
        "databaseId=" + ESConstants.DB_ID
}, scope = ServiceScope.SINGLETON)
public class ElasticsearchClientWrapperImpl implements ElasticsearchClientWrapper {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchClientWrapperImpl.class);

    private ElasticsearchClient client;
    private Rest5Client rest5Client;
    private ElasticsearchConfig elasticsearchConfig;
    private ElasticsearchConnectionConfig connectionConfig;
    private Sniffer sniffer;

    @Activate
    public void activate() {
        logger.info("ElasticsearchClientWrapper service activated");
    }

    @Deactivate
    public void deactivate() {
        closeClients();
    }

    @Reference
    public void setElasticsearchConfig(ElasticsearchConfig elasticsearchConfig) {
        this.elasticsearchConfig = elasticsearchConfig;
    }

    @Override
    public synchronized ElasticsearchClient getClient() throws ConnectionUnavailableException {
        resolveConnection();
        return client;
    }

    @Override
    public synchronized Rest5Client getRest5Client() throws ConnectionUnavailableException {
        resolveConnection();
        return rest5Client;
    }

    @Override
    public String performRequest(GetRequest request) throws IOException, ConnectionUnavailableException {
        return getClient().get(request).toString();
    }

    @Override
    public String performRequest(Request request) throws IOException, ParseException, ConnectionUnavailableException {
        if (request.getMethod().equalsIgnoreCase("GET")) {
            Response get = getRest5Client().performRequest(request);
            return EntityUtils.toString(get.getEntity());
        } else {
            throw new IOException("Only GET methods are supported for low level client");
        }
    }

    @Override
    public String getEnvironmentPrefix() {
        return SettingsBean.getInstance().getPropertyValue("elasticsearch.prefix");
    }

    private ElasticsearchClients resolveClient() throws ConnectionUnavailableException {
        return resolveClient(false);
    }

    /**
     * Initialize client connections
     * <p>
     * <b>Precondition:</b> {@code connectionConfig} must be initialized. Call {@link #resolveConnectionConfig()} if needed.
     * </p>
     * @param isTestClient flag if client is a test client - skips sniffer configurations
     * @throws ConnectionUnavailableException
     */
    private ElasticsearchClients resolveClient(boolean isTestClient) throws ConnectionUnavailableException {
        logger.debug("Initializing elasticsearch clients...");
        if (connectionConfig == null) {
            throw new ConnectionUnavailableException("No Elasticsearch connection configured");
        }
        List<URI> addresses = ConnectionUtils.getAddresses(connectionConfig);
        Rest5ClientBuilder builder = Rest5Client.builder(addresses);

        // Configure TCP/IP socket params for the connection
        IOReactorConfig reactorConfig = ConnectionUtils.getTcpipConfig();

        // Handle security if SSL or credentials are configured
        if (connectionConfig.isUseEncryption() || connectionConfig.hasCredentials()) {
            logger.debug("Initializing security configurations for elasticsearch connection");
            handleSecurityConfiguration(builder, reactorConfig, connectionConfig);
        } else if (!isTestClient) {
            logger.debug("Initializing TCP/IP params for elasticsearch connection");
            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                httpClientBuilder.setIOReactorConfig(reactorConfig);
            });
        }

        // Create default sniff failure listener before building client
        SniffOnFailureListener sniffOnFailureListener = null;
        if (!isTestClient && connectionConfig.getSnifferInterval() != null) {
            logger.debug("Initializing Sniffer configurations");
            sniffOnFailureListener = new SniffOnFailureListener();
            builder.setFailureListener(sniffOnFailureListener);
        }

        ElasticsearchClients esClients = ElasticsearchClients.build(builder);

        // Configure the sniffer with the built client
        if (!isTestClient) {
            configureSniffer(esClients.getRest5Client(), sniffOnFailureListener, connectionConfig);
        }

        logger.debug("Elasticsearch clients initialized successfully");
        return esClients;
    }

    private void configureSniffer(Rest5Client restClient, SniffOnFailureListener sniffOnFailureListener, ElasticsearchConnectionConfig connConfig) {
        if (connConfig.getSnifferInterval() == null || sniffOnFailureListener == null) {
            return;
        }
        ElasticsearchNodesSniffer.Scheme scheme = connConfig.isUseEncryption() ?
                ElasticsearchNodesSniffer.Scheme.HTTPS :
                ElasticsearchNodesSniffer.Scheme.HTTP;
        ElasticsearchNodesSniffer nodesSniffer = new ElasticsearchNodesSniffer(restClient, 10000, scheme);
        int intervalMillis = connConfig.getSnifferIntervalMillis();

        sniffer = Sniffer.builder(restClient)
                .setSniffIntervalMillis(intervalMillis)
                .setNodesSniffer(nodesSniffer)
                .build();
        sniffOnFailureListener.setSniffer(sniffer);
    }

    private void handleSecurityConfiguration(Rest5ClientBuilder restClientBuilder,
            IOReactorConfig reactorConfig, ElasticsearchConnectionConfig connConfig) throws ConnectionUnavailableException {
        try {
            final SSLContext sslContext;
            boolean isDevMode = SettingsBean.getInstance().isDevelopmentMode();

            // Create SSL context - open in dev mode, otherwise use default (null)
            TrustStrategy sslTrustStrategy = isDevMode ? TrustAllStrategy.INSTANCE : null;
            sslContext = SSLContexts.custom().loadTrustMaterial(sslTrustStrategy).build();
            restClientBuilder.setSSLContext(sslContext);

            // Provide credentials, tcpip request config to client config callback
            final CredentialsProvider credentialsProvider = ConnectionUtils.getCredentialsProvider(connConfig);
            restClientBuilder.setHttpClientConfigCallback(httpClientBuilder -> {
                httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setIOReactorConfig(reactorConfig);
            });

            // Disable hostname verification in dev mode
            if (isDevMode) {
                restClientBuilder.setConnectionManagerCallback(connManager -> {
                    TlsStrategy noopHostnameStrategy = ConnectionUtils.getNoopHostnameStrategy(sslContext);
                    connManager.setTlsStrategy(noopHostnameStrategy);
                });
            }
        } catch (GeneralSecurityException ex) {
            throw new ConnectionUnavailableException("Failed to configure SSL context when configuring ES Rest Client", ex);
        } catch (ConnectionConfigException ex) {
            throw new ConnectionUnavailableException("Unable to decode credentials from existing configuration", ex);
        }
    }

    /**
     * - Resolve configuration
     * - Test connection (resolve test connections, send a ping and close test clients)
     * - If test connection succeeded, create and initialize clients
     * @throws ConnectionUnavailableException
     */
    private synchronized void resolveConnection() throws ConnectionUnavailableException {
        if (!resolveConnectionConfig()) {
            // Connection config hasn't changed; do not recreate clients
            return;
        }

        try {
            doTestConnection();
            // Create actual clients with sniffer
            ElasticsearchClients clients = resolveClient();
            this.client = clients.getElasticsearchClient();
            this.rest5Client = clients.getRest5Client();
        } catch (ConnectException e) {
            handleConnectionFailure("Failed to establish connection", e);
        } catch (IOException | ElasticsearchException e) {
            handleConnectionFailure("Error during connection setup", e);
        }
    }

    public boolean testConnection() {
        logger.info("Testing connection...");
        try {
            resolveConnectionConfig();
            doTestConnection();
        } catch (ConnectionUnavailableException | IOException e) {
            logger.error("Error validating test connection to elasticsearch", e);
            return false;
        } finally {
            logger.info("Finished testing connection");
        }
        return true;
    }

    /**
     * Initializes or resolves elasticsearch connection configuration
     * @return true if connection config has changed and need to refresh/resolve elasticsearch connections; false otherwise
     * @throws ConnectionUnavailableException if elasticsearch connection configuration is unavailable
     */
    private boolean resolveConnectionConfig() throws ConnectionUnavailableException {
        logger.debug("Getting connection configuration...");
        ElasticsearchConnectionConfig newConnConfig = elasticsearchConfig.getConnectionConfig();
        if (newConnConfig == null) {
            throw new ConnectionUnavailableException("No Elasticsearch connection configuration available");
        }

        // Skip if connection is already established and hasn't changed
        boolean isValidConnectionConfig = connectionConfig != null
                && connectionConfig.equals(newConnConfig)
                && client != null;
        if (isValidConnectionConfig) {
            logger.debug("Connection and client exist and have not changed, will use existing connection and client.");
            return false;
        }

        // Clean up existing resources
        closeClients();
        connectionConfig = newConnConfig;
        logger.debug("Connection configuration initialized/updated successfully.");
        return true;
    }

    /**
     * Test elasticsearch connections.
     * Use existing client if it exists, otherwise create a test client connection.
     * @precondition connectionConfig has been initialized; call resolveConnectionConfig() otherwise.
     * @throws IOException
     * @throws ConnectionUnavailableException
     */
    private void doTestConnection() throws IOException, ConnectionUnavailableException {
        ElasticsearchClient testClient = null;
        ElasticsearchClients testClients = null;
        boolean createdTestClient = false;

        try {
            // Use existing client or create a new test client
            if (this.client != null) {
                testClient = this.client;
            } else {
                logger.debug("Creating test client...");
                testClients = resolveClient(true);
                testClient = testClients.getElasticsearchClient();
                createdTestClient = true;
                logger.debug("Test client created successfully.");
            }

            logger.debug("Sending test ping elasticsearch connection...");
            if (!testClient.ping().value()) {
                throw new ConnectionUnavailableException("Elasticsearch cluster is not responding");
            }
            logger.debug("Elasticsearch test ping request sent successfully.");
        } finally {
            // Only close resources we created in this method
            if (createdTestClient && testClient != null) {
                try {
                    testClient.close();
                } catch (IOException e) {
                    logger.warn("Failed to close test clients: {}", e.getMessage());
                }
            }
        }
    }

    private void handleConnectionFailure(String message, Exception e)
            throws ConnectionUnavailableException {
        logger.error("{}: {}", message, e.getMessage());
        closeClients();
        throw new ConnectionUnavailableException(message + ": " + e.getMessage());
    }

    private void closeClients() {
        logger.debug("Starting client cleanup process");
        // Close sniffer first as it depends on the REST client
        if (sniffer != null) {
            try {
                sniffer.close();
            } catch (Exception e) {
                logger.warn("Error while closing sniffer: {}", e.getMessage());
            } finally {
                sniffer = null;
            }
        }

        // Close high-level client which will also close the low-level client
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                logger.warn("Error while closing client: {}", e.getMessage());
            } finally {
                client = null;
                rest5Client = null;
            }
        }

        connectionConfig = null;

        logger.debug("Client cleanup completed successfully");
    }
}
