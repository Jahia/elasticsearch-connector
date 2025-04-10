package org.jahia.modules.elasticsearchconnector.connection;

import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public abstract class AbstractConnection {
    public static final String PROPERTY_ID = "id";
    public static final String PROPERTY_HOST = "host";
    public static final String PROPERTY_PORT = "port";
    public static final String PROPERTY_URI = "uri";
    public static final String PROPERTY_USER = "user";
    public static final String PROPERTY_PASSWORD = "password";
    public static final String PROPERTY_USEXPACKSECURITY = "useXPackSecurity";
    public static final String PROPERTY_NODESSNIFFERINTERVAL = "nodesSnifferInterval";
    public static final String PROPERTY_USEENCRYPTION = "useEncryption";
    public static final String PROPERTY_ADDITIONALHOSTS = "additionalHosts";
    public static final String DEFAULT_PROTOCOL_SCHEME = "http";
    public static final String SECURE_PROTOCOL_SCHEME = "https";
    public static final Integer DEFAULT_PORT = 9200;

    protected static final Logger logger = LoggerFactory.getLogger(AbstractConnection.class);

    protected String id;
    protected String host;
    protected Integer port;
    protected String uri;
    protected String user;
    protected String password;
    protected String nodesSnifferInterval;
    protected boolean useXPackSecurity;
    protected boolean useEncryption;
    protected List<AdditionalHost> additionalHosts = new ArrayList<>();

    public AbstractConnection() {
    }

    public abstract boolean testConnectionCreation();

    public abstract Object beforeRegisterAsService();

    public abstract void beforeUnregisterAsService();

    //public abstract T makeConnectionData();

    //public abstract E getServerStatus();

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

    public int getNodesSnifferInterval() {
        String interval = nodesSnifferInterval.replace("s", "000");
        return interval.contains("m") ? Integer.parseInt(interval.replace("m", "")) * 60000 : Integer.parseInt(interval);
    }

    public void setNodesSnifferInterval(String nodesSnifferInterval) {
        this.nodesSnifferInterval = nodesSnifferInterval;
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

    public String getProtocolScheme() {
        return isUseXPackSecurity() ? SECURE_PROTOCOL_SCHEME : DEFAULT_PROTOCOL_SCHEME;
    }

    public List<HttpHost> getAdditionalHosts() {
        List<HttpHost> l = new ArrayList<>();
        additionalHosts.stream().map(h -> new HttpHost(getHost(), h.getPort(), isUseEncryption() ? SECURE_PROTOCOL_SCHEME : DEFAULT_PROTOCOL_SCHEME)).forEach(l::add);
        return l;
    }

    public void setAdditionalHosts(List<AdditionalHost> additionalHosts) {
        this.additionalHosts = additionalHosts;
    }

    public static class AdditionalHost {
        private String host;
        private Integer port;

        public AdditionalHost(String host, Integer port) {
            this.host = host;
            this.port = port;
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
    }
}
