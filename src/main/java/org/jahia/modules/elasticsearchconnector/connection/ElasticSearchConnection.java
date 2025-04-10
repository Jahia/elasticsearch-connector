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
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.client.*;
import org.elasticsearch.client.sniff.ElasticsearchNodesSniffer;
import org.elasticsearch.client.sniff.SniffOnFailureListener;
import org.elasticsearch.client.sniff.Sniffer;
import org.jahia.modules.elasticsearchconnector.rest.ElasticRestHighLevelClient;
import org.jahia.modules.elasticsearchconnector.rest.ElasticRestHighLevelClientImpl;
import org.jahia.settings.SettingsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.ConnectException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * 2017-05-17
 *
 * @author Astrit Ademi
 */
public class ElasticSearchConnection extends AbstractConnection {
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchConnection.class);
    private static final long SNIFF_REQUEST_TIMEOUT_MILLIS = 5000L;
    private transient ElasticRestHighLevelClient esRestHighLevelClient;
    private transient Sniffer esSniffer;
    private transient PoolingNHttpClientConnectionManager connectionManager = null;

    /**
     * Constructor
     *
     * @param id identifier for this connection
     */
    public ElasticSearchConnection(String id) {
        this.id = id;
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

    private RestHighLevelClient resolveClient(boolean snifferToBeAdded, boolean testingOnly) {
        List<HttpHost> addresses = getAdditionalHosts();
        int snifferInterval = getNodesSnifferInterval();
        boolean useSSL = isUseXPackSecurity();
        String protocolScheme = getProtocolScheme();

        addresses.add(0, new HttpHost(host, port, protocolScheme));

        RestClientBuilder builder = RestClient.builder(addresses.toArray(new HttpHost[0]));
        builder.setRequestConfigCallback(
                requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(30000)
                        .setSocketTimeout(60000 * 5));

        PoolingNHttpClientConnectionManager manager = getConnectionManager();
        if (!testingOnly && manager != null) {
            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setConnectionManager(manager)
                    .setDefaultIOReactorConfig(IOReactorConfig.custom()
                            .setTcpNoDelay(false)
                            .setSoKeepAlive(true)
                            .build()));
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
}
