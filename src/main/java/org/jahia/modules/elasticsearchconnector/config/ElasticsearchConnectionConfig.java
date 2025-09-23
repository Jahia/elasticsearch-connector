package org.jahia.modules.elasticsearchconnector.config;

import org.apache.commons.lang3.StringUtils;
import org.jahia.utils.EncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static org.jahia.modules.elasticsearchconnector.ESConstants.*;

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
public class ElasticsearchConnectionConfig {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConnectionConfig.class);

    private String id;
    private String host;
    private Integer port;
    private String user;
    private String password;
    private String snifferInterval;
    private boolean useXPackSecurity;
    private boolean useEncryption;
    private List<String> additionalHostAddresses;

    public ElasticsearchConnectionConfig() {
        this.id = DB_ID;
    }

    public ElasticsearchConnectionConfig(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setHost(String host) {
        this.host = (host == null || host.trim().isEmpty()) ? DEFAULT_HOST : host;
    }

    public String getHost() {
        return this.host;
    }

    public void setPort(Integer port) {
        this.port = (port == null) ? DEFAULT_PORT : port;
    }

    public Integer getPort() {
        return this.port;
    }

    public void setUser(String user) {
        this.user = (user == null || user.trim().isEmpty()) ? DEFAULT_USER : user;
    }

    public String getUser() {
        return this.user;
    }

    public String getPassword() {
        return this.password;
    }

    public char[] decodePassword() throws ConnectionConfigException {
        try {
            return EncryptionUtils.passwordBaseDecrypt(this.password).toCharArray();
        } catch (Exception e) {
            throw new ConnectionConfigException("Unable to decode password from existing configuration", e);
        }
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean hasCredentials() {
        return StringUtils.isNotEmpty(this.user) && StringUtils.isNotEmpty(this.password);
    }

    public void setSnifferInterval(String snifferInterval) {
        this.snifferInterval = (snifferInterval == null || snifferInterval.trim().isEmpty()) ? DEFAULT_SNIFFER_INTERVAL : snifferInterval;
    }

    public String getSnifferInterval() {
        return this.snifferInterval;
    }

    public int getSnifferIntervalMillis() {
        String interval = getSnifferInterval();
        return (interval != null) ? Integer.parseInt(interval.replaceAll("[^0-9]", "")) * 1000 : -1;
    }

    public boolean isUseXPackSecurity() {
        return this.useXPackSecurity;
    }

    public void setUseXPackSecurity(boolean useXPackSecurity) {
        this.useXPackSecurity = useXPackSecurity;
    }

    public boolean isUseEncryption() {
        return this.useEncryption;
    }

    public String getProtocolScheme() {
        return isUseEncryption() ? "https" : "http";
    }

    public void setUseEncryption(boolean useEncryption) {
        this.useEncryption = useEncryption;
    }

    public List<String> getAdditionalHostAddresses() {
        return this.additionalHostAddresses;
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
        ElasticsearchConnectionConfig that = (ElasticsearchConnectionConfig) o;
        return useXPackSecurity == that.useXPackSecurity
                && useEncryption == that.isUseEncryption()
                && Objects.equals(id, that.getId())
                && Objects.equals(host, that.getHost())
                && Objects.equals(port, that.getPort())
                && Objects.equals(user, that.getUser())
                && Objects.equals(password, that.getPassword())
                && Objects.equals(additionalHostAddresses, that.getAdditionalHostAddresses());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, host, port, user, password, useXPackSecurity, useEncryption, additionalHostAddresses);
    }
}
