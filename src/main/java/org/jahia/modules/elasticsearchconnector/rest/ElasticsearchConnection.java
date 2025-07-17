package org.jahia.modules.elasticsearchconnector.rest;

import java.util.List;
import java.util.Objects;

/**
 * Represents an Elasticsearch connection configuration.
 * This class holds all the necessary parameters to establish and maintain a connection to an Elasticsearch cluster.
 * It includes connection details such as host, port, authentication credentials, SSL settings, and additional host addresses
 * for cluster nodes.
 *
 * The connection configuration supports:
 * - Basic connection parameters (host, port, URI)
 * - Authentication (username/password)
 * - Security settings (X-Pack security, encryption)
 * - Cluster configuration (additional hosts, sniffer interval)
 */
public class ElasticsearchConnection {

    protected String id;
    protected String host;
    protected Integer port;
    protected String uri;
    protected String user;
    protected String password;
    protected String snifferInterval;
    protected boolean useXPackSecurity;
    protected boolean useEncryption;
    protected List<String> additionalHostAddresses;

    public ElasticsearchConnection(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return this.port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUri() {
        return this.uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUser() {
        return this.user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSnifferInterval() {
        return snifferInterval;
    }

    public void setSnifferInterval(String snifferInterval) {
        this.snifferInterval = snifferInterval;
    }

    public boolean isUseXPackSecurity() {
        return useXPackSecurity;
    }

    public void setUseXPackSecurity(boolean useXPackSecurity) {
        this.useXPackSecurity = useXPackSecurity;
    }

    public boolean isUseEncryption() {
        return useEncryption;
    }

    public void setUseEncryption(boolean useEncryption) {
        this.useEncryption = useEncryption;
    }

    public List<String> getAdditionalHostAddresses() {
        return additionalHostAddresses;
    }

    public void setAdditionalHostAddresses(List<String> additionalHostAddresses) {
        this.additionalHostAddresses = additionalHostAddresses;
    }

    public String getJson() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElasticsearchConnection that = (ElasticsearchConnection) o;
        return useXPackSecurity == that.useXPackSecurity && useEncryption == that.useEncryption && Objects.equals(id, that.id) && Objects.equals(host, that.host) && Objects.equals(port, that.port) && Objects.equals(uri, that.uri) && Objects.equals(user, that.user) && Objects.equals(password, that.password) && Objects.equals(additionalHostAddresses, that.additionalHostAddresses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, host, port, uri, user, password, useXPackSecurity, useEncryption, additionalHostAddresses);
    }
}
