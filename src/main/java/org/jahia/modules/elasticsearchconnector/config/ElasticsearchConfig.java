package org.jahia.modules.elasticsearchconnector.config;

import org.jahia.modules.elasticsearchconnector.ESConstants;
import org.jahia.modules.elasticsearchconnector.rest.ElasticsearchConnection;
import org.jahia.services.modulemanager.util.PropertiesList;
import org.jahia.services.modulemanager.util.PropertiesManager;
import org.jahia.services.modulemanager.util.PropertiesValues;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.util.*;
import java.util.stream.Collectors;

@Component(service = {ManagedService.class, ElasticsearchConfig.class}, property = {
        "service.pid=org.jahia.modules.elasticsearch_config",
        "service.description=Elasticsearch configuration service",
        "service.vendor=Jahia Solutions Group SA"
}, immediate = true)
public class ElasticsearchConfig implements ManagedService {

    private static final String CONFIG_NAMESPACE_PREFIX = "org.jahia.modules.elasticsearch-connector";
    private static final String PROPERTY_HOST = CONFIG_NAMESPACE_PREFIX + ".host";
    private static final String PROPERTY_PORT = CONFIG_NAMESPACE_PREFIX + ".port";
    private static final String PROPERTY_USER = CONFIG_NAMESPACE_PREFIX + ".user";
    private static final String PROPERTY_PASSWORD = CONFIG_NAMESPACE_PREFIX + ".password";
    private static final String PROPERTY_USE_XPACK_SECURITY = CONFIG_NAMESPACE_PREFIX + ".useXPackSecurity";
    private static final String PROPERTY_USE_ENCRYPTION = CONFIG_NAMESPACE_PREFIX + ".useEncryption";
    private static final String PROPERTY_NODES_SNIFFER_INTERVAL = CONFIG_NAMESPACE_PREFIX + ".nodesSnifferInterval";
    private static final String PROPERTY_ADDITIONAL_HOSTS = CONFIG_NAMESPACE_PREFIX + ".additionalHostAddresses";

    private static final String DEFAULT_HOST = "elasticsearch";
    private static final int DEFAULT_PORT = 9200;
    private static final String DEFAULT_ID = ESConstants.DB_ID;
    private static final boolean DEFAULT_USE_XPACK_SECURITY = false;
    private static final boolean DEFAULT_USE_ENCRYPTION = false;
    private static final String DEFAULT_SNIFFER_INTERVAL = "5s";
    private static final String DEFAULT_USER = "elastic";

    private ElasticsearchConnection connection;


    public ElasticsearchConfig() {
        super();
    }

    @Override
    public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {
        if (dictionary != null) {
            PropertiesManager pm = new PropertiesManager(getMap(dictionary));
            PropertiesValues values = pm.getValues();

            ElasticsearchConnection con = new ElasticsearchConnection(DEFAULT_ID);
            con.setPort(getPropertyOrDefault(values, PROPERTY_PORT, DEFAULT_PORT, Integer.class));
            con.setHost(getPropertyOrDefault(values, PROPERTY_HOST, DEFAULT_HOST, String.class));
            con.setSnifferInterval(getPropertyOrDefault(values, PROPERTY_NODES_SNIFFER_INTERVAL, DEFAULT_SNIFFER_INTERVAL, String.class));
            con.setUseXPackSecurity(getPropertyOrDefault(values, PROPERTY_USE_XPACK_SECURITY, DEFAULT_USE_XPACK_SECURITY, Boolean.class));
            con.setUseEncryption(getPropertyOrDefault(values, PROPERTY_USE_ENCRYPTION, DEFAULT_USE_ENCRYPTION, Boolean.class));
            con.setUser(getPropertyOrDefault(values, PROPERTY_USER, DEFAULT_USER, String.class));
            con.setPassword(getPropertyOrDefault(values, PROPERTY_PASSWORD, "", String.class));
            con.setAdditionalHostAddresses(getPropertyOrDefault(values, PROPERTY_ADDITIONAL_HOSTS, new ArrayList<>(), List.class));

            connection = con;
        }
    }

    public ElasticsearchConnection getConnection() {
        return connection;
    }

    private static Map<String, String> getMap(Dictionary<String, ?> d) {
        Map<String, String> m = new HashMap<>();
        if (d != null) {
            Enumeration<String> en = d.keys();
            while (en.hasMoreElements()) {
                String key = en.nextElement();
                if (!key.startsWith("felix.") && !key.startsWith("service.")) {
                    m.put(key, d.get(key).toString());
                }
            }
        }
        return m;
    }

    private <T> T getPropertyOrDefault(PropertiesValues values, String propertyName, T defaultValue, Class<T> type) {
        Object value = null;

        if (values != null) {
            if (type == String.class) {
                value = values.getProperty(propertyName);
            } else if (type == Integer.class) {
                value = values.getIntegerProperty(propertyName);
            } else if (type == Boolean.class) {
                value = values.getBooleanProperty(propertyName);
            } else if (type == List.class) {
                PropertiesList list = values.getList(propertyName);
                if (list != null) {
                    value = list.getStructuredList().stream()
                            .map(obj -> Objects.toString(obj, null))
                            .collect(Collectors.toList());
                }
            }
        }

        return value != null ? type.cast(value) : defaultValue;
    }
}
