package org.jahia.modules.elasticsearchconnector.rest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Response;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.ElasticsearchNodesSniffer;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.SniffOnFailureListener;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.Sniffer;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.jahia.modules.elasticsearchconnector.ESConstants;
import org.jahia.modules.elasticsearchconnector.config.ElasticsearchConfig;
import org.jahia.settings.SettingsBean;
import org.osgi.service.component.annotations.*;
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
import java.util.List;
import java.util.stream.Collectors;

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
 * @see ElasticsearchConnection
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
    private ElasticsearchConnection connection;
    private Sniffer sniffer;

    @Activate
    public void activate() {
        logger.info("ElasticsearchClientWrapper activated");
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

    @Override
    public ElasticsearchConnection getConnection() {
        return connection;
    }

    private ElasticsearchClients resolveClient(boolean snifferToBeAdded) {
        List<HttpHost> addresses = new ArrayList<>();
        String protocolScheme = connection.isUseEncryption() ? "https" : "http";

        // Add base address
        addresses.add(new HttpHost(protocolScheme, connection.getHost(), connection.getPort()));

        // Add additional hosts if available
        if (connection.getAdditionalHostAddresses() != null) {
            connection.getAdditionalHostAddresses().forEach(host -> {
                String[] hostParts = host.split(":");
                addresses.add(new HttpHost(protocolScheme, hostParts[0],
                        hostParts.length > 1 ? Integer.parseInt(hostParts[1]) : connection.getPort()));
            });
        }

        Rest5ClientBuilder builder = Rest5Client.builder(addresses.stream()
                .map(HttpHost::toURI)
                .map(u -> {
                    try {
                        return new URI(u);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList()));

        // Handle security if SSL or credentials are configured
        if (connection.isUseEncryption() || hasCredentials(connection)) {
            handleSecurityConfiguration(builder, connection);
        }

        Rest5Client restClient = builder.build();
        ElasticsearchTransport transport = new Rest5ClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchClient esClient = new ElasticsearchClient(transport);

        // Configure sniffer if needed
        if (snifferToBeAdded && connection.getSnifferInterval() != null) {
            configureSniffer(builder, restClient, protocolScheme, connection.getSnifferInterval());
        }

        return new ElasticsearchClients(esClient, restClient);
    }

    private boolean hasCredentials(ElasticsearchConnection connection) {
        return StringUtils.isNotEmpty(connection.getUser()) &&
                StringUtils.isNotEmpty(connection.getPassword());
    }

    private void configureSniffer(Rest5ClientBuilder builder, Rest5Client restClient,
                                  String protocolScheme, String snifferInterval) {
        SniffOnFailureListener sniffOnFailureListener = new SniffOnFailureListener();
        builder.setFailureListener(sniffOnFailureListener);

        ElasticsearchNodesSniffer.Scheme scheme = protocolScheme.equals("http") ?
                ElasticsearchNodesSniffer.Scheme.HTTP :
                ElasticsearchNodesSniffer.Scheme.HTTPS;

        ElasticsearchNodesSniffer nodesSniffer = new ElasticsearchNodesSniffer(restClient, 10000, scheme);
        int intervalMillis = parseSnifferInterval(snifferInterval);

        sniffer = Sniffer.builder(restClient)
                .setSniffIntervalMillis(intervalMillis)
                .setNodesSniffer(nodesSniffer)
                .build();
        sniffOnFailureListener.setSniffer(sniffer);
    }

    private int parseSnifferInterval(String interval) {
        return Integer.parseInt(interval.replaceAll("[^0-9]", "")) * 1000;
    }

    private void handleSecurityConfiguration(Rest5ClientBuilder restClientBuilder, ElasticsearchConnection connection) {
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
            String cred = Base64.getEncoder().encodeToString((connection.user + ":" + connection.password).getBytes());
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

    private synchronized void resolveConnection() throws ConnectionUnavailableException {
        ElasticsearchConnection newConnection = elasticsearchConfig.getConnection();
        if (newConnection == null) {
            throw new ConnectionUnavailableException("No Elasticsearch connection configuration available");
        }

        // Skip if connection is already established and hasn't changed
        if (isConnectionValid(newConnection)) {
            return;
        }

        // Clean up existing resources
        closeClients();
        connection = newConnection;

        try {
            // Create test client first to verify connection
            ElasticsearchClients testClients = createTestClients();
            validateConnection(testClients.getElasticsearchClient());
            cleanupTestClients(testClients);

            // Create actual clients with sniffer
            ElasticsearchClients clients = resolveClient(true);
            this.client = clients.getElasticsearchClient();
            this.rest5Client = clients.getRest5Client();

        } catch (ConnectException e) {
            handleConnectionFailure("Failed to establish connection", e);
        } catch (IOException | ElasticsearchException e) {
            handleConnectionFailure("Error during connection setup", e);
        }
    }

    private boolean isConnectionValid(ElasticsearchConnection newConnection) {
        return connection != null
                && connection.equals(newConnection)
                && client != null;
    }

    private ElasticsearchClients createTestClients() {
        return resolveClient(false);
    }

    private void validateConnection(ElasticsearchClient testClient)
            throws IOException, ConnectionUnavailableException {
        if (!testClient.ping().value()) {
            throw new ConnectionUnavailableException("Elasticsearch cluster is not responding");
        }
    }

    private void cleanupTestClients(ElasticsearchClients testClients) {
        try {
            testClients.getElasticsearchClient().close();
        } catch (IOException e) {
            logger.warn("Failed to close test clients: {}", e.getMessage());
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

        // Clear connection last
        connection = null;

        logger.debug("Client cleanup completed successfully");
    }

    private static class ElasticsearchClients {
        private ElasticsearchClient elasticsearchClient;
        private Rest5Client rest5Client;

        public ElasticsearchClients(ElasticsearchClient elasticsearchClient, Rest5Client rest5Client) {
            this.elasticsearchClient = elasticsearchClient;
            this.rest5Client = rest5Client;
        }

        public ElasticsearchClient getElasticsearchClient() {
            return elasticsearchClient;
        }

        public void setElasticsearchClient(ElasticsearchClient elasticsearchClient) {
            this.elasticsearchClient = elasticsearchClient;
        }

        public Rest5Client getRest5Client() {
            return rest5Client;
        }

        public void setRest5Client(Rest5Client rest5Client) {
            this.rest5Client = rest5Client;
        }
    }
}
