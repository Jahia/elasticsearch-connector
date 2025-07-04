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
import org.jahia.modules.elasticsearchconnector.config.ElasticsearchConfig;
import org.jahia.settings.SettingsBean;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
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
 * Implementation of Elasticsearch client
 */
@Component(service = ElasticsearchClientWrapper.class, immediate = true, property = {"databaseType=ELASTICSEARCH", "databaseId=myId"})
public class ElasticsearchClientWrapperImpl implements ElasticsearchClientWrapper {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchClientWrapperImpl.class);

    private ElasticsearchClient client;
    private Rest5Client rest5Client;
    private ElasticsearchConfig elasticsearchConfig;
    private ElasticsearchConnection connection;
    private Sniffer sniffer;

    @Deactivate
    public void deactivate() {
        connection = null;

        if (sniffer != null) {
            sniffer.close();
            sniffer = null;
        }

        if (client != null) {
            try {
                client.close();
                client = null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Reference
    public void setElasticsearchConfig(ElasticsearchConfig elasticsearchConfig) {
        this.elasticsearchConfig = elasticsearchConfig;
    }

    // TODO: is it possible to have issues with this in if multiple threads compete for the client?
    @Override
    public ElasticsearchClient getClient() throws ConnectionUnavailableException {
        resolveConnection();
        return client;
    }

    @Override
    public Rest5Client getRest5Client() throws ConnectionUnavailableException {
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

    // TODO handle res5 lowlevel client
    private ElasticsearchClient resolveClient(boolean snifferToBeAdded) {
        List<HttpHost> addresses = new ArrayList<>();
        int snifferInterval = 5000;
        boolean useSSL = false;
        //Add base address
        String protocolScheme = "http";
        useSSL = connection.isUseXPackSecurity();

        addresses.add(0, new HttpHost(protocolScheme, connection.host, connection.port));

        Rest5ClientBuilder builder = Rest5Client.builder(addresses.stream().map(HttpHost::toURI).map(u -> {
            try {
                return new URI(u);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()));

        //If SSL or security is enabled handle the configuration
        if (useSSL || (StringUtils.isNotEmpty(connection.user) && StringUtils.isNotEmpty(connection.password))) {
            handleSecurityConfiguration(builder, connection);
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
            if (!protocolScheme.equals("http")) {
                scheme = ElasticsearchNodesSniffer.Scheme.HTTPS;
            }
            ElasticsearchNodesSniffer nodesSniffer = new ElasticsearchNodesSniffer(restClient, 10000, scheme);
            sniffer = Sniffer.builder(restClient)
                    .setSniffIntervalMillis(snifferInterval)
                    .setNodesSniffer(nodesSniffer)
                    .build();
            sniffOnFailureListener.setSniffer(sniffer);
        }

        //this.rest5Client = restClient;
        return esClient;
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

    private void resolveConnection() throws ConnectionUnavailableException {
        ElasticsearchConnection con = elasticsearchConfig.getConnection();

        if (connection == null || !connection.equals(con) || this.client == null) {
            connection = con;
            this.client = resolveClient(false);

            ElasticsearchClient c = resolveClient(false);

            try {
                if (!c.ping().value()) {
                    throw new ConnectionUnavailableException("Could not establish connection with Elasticsearch");
                }
            } catch (ConnectException e) {
                logger.warn("Failed to create/ping connection due to: {}", e.getMessage());
                // TODO close client
                throw new ConnectionUnavailableException("Could not establish connection with Elasticsearch: " + e.getMessage());
            } catch (IOException | ElasticsearchException e) {
                logger.error("Failed to create/ping connection due to: {}", e.getMessage(), e);
                // TODO close client
                throw new ConnectionUnavailableException("Could not establish connection with Elasticsearch: " + e.getMessage());
            } finally {
                try {
                    c.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
}
