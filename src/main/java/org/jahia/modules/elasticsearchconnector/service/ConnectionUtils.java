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
 *     Copyright (C) 2002-2025 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.elasticsearchconnector.service;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.ssl.BasicClientTlsStrategy;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.net.NamedEndpoint;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.reactor.ssl.SSLSessionVerifier;
import org.apache.hc.core5.reactor.ssl.TlsDetails;
import org.jahia.modules.elasticsearchconnector.config.ElasticsearchConnectionConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ConnectionUtils {

    private ConnectionUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * @return {@code CredentialsProvider} that includes username, password credentials
     * from the ES connection config and uses host:port as scope for applicable auth requests.
     */
    public static CredentialsProvider getCredentialsProvider(ElasticsearchConnectionConfig connConfig) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(connConfig.getHost(), connConfig.getPort()), // Do we need AuthScope.ANY here?
                new UsernamePasswordCredentials(connConfig.getUser(), connConfig.decodePassword())
        );
        return credentialsProvider;
    }

    /**
     * @return List of addresses that includes host:port from ES connection config, as well as additional host
     * addresses if specified, and converted into URI objects.
     */
    public static List<URI> getAddresses(ElasticsearchConnectionConfig connConfig) {
        List<HttpHost> addresses = new ArrayList<>();
        String protocolScheme = connConfig.getProtocolScheme();

        // Add base address
        addresses.add(new HttpHost(protocolScheme, connConfig.getHost(), connConfig.getPort()));

        // Add additional hosts if available
        if (connConfig.getAdditionalHostAddresses() != null) {
            connConfig.getAdditionalHostAddresses().forEach(host -> {
                String[] hostParts = host.split(":");
                int port = hostParts.length > 1 ? Integer.parseInt(hostParts[1]) : connConfig.getPort();
                addresses.add(new HttpHost(protocolScheme, hostParts[0], port));
            });
        }

        Function<HttpHost, URI> httpHostToURIFunction = httpHost -> {
            try {
                return new URI(httpHost.toURI());
            } catch (URISyntaxException e) {
                String msg = String.format("Unable to parse http host address [%s] to URI", httpHost);
                throw new RuntimeException(msg, e);
            }
        };

        return addresses.stream()
                .map(httpHostToURIFunction)
                .collect(Collectors.toList());
    }

    /**
     * @return TlsStrategy that disables hostname verification
     */
    public static TlsStrategy getNoopHostnameStrategy(SSLContext sslContext) {
        // SSLSessionVerifier that effectively disables hostname verification
        SSLSessionVerifier noopVerifier = new SSLSessionVerifier() {
            @Override
            public TlsDetails verify(NamedEndpoint namedEndpoint, SSLEngine sslEngine) throws SSLException {
                return new TlsDetails(sslEngine.getSession(), sslEngine.getSession().getProtocol());
            }
        };
        return new BasicClientTlsStrategy(sslContext, noopVerifier);
    }

    /**
     * @return Custom TCP/IP socket param configs
     */
    public static IOReactorConfig getTcpipConfig() {
        return IOReactorConfig.custom()
                .setTcpNoDelay(false)
                .setSoKeepAlive(true)
                .build();
    }
}
