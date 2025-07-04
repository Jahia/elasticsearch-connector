package org.jahia.modules.elasticsearchconnector.config;

import org.jahia.modules.elasticsearchconnector.rest.ElasticsearchConnection;
import org.jahia.services.modulemanager.util.PropertiesManager;
import org.jahia.services.modulemanager.util.PropertiesValues;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.util.*;

@Component(service = {ManagedService.class, ElasticsearchConfig.class}, property = {
        "service.pid=org.jahia.modules.elasticsearch_config",
        "service.description=Elasticsearch configuration service",
        "service.vendor=Jahia Solutions Group SA"
}, immediate = true)
public class ElasticsearchConfig implements ManagedService {

    private static final String CONFIG_NAMESPACE_PREFIX = "org.jahia.modules.elasticsearch-connector";

    private ElasticsearchConnection connection;


    public ElasticsearchConfig() {
        super();
    }

    @Activate
    public void activate() {
    }

    @Deactivate
    public void deactivate() {
    }


    @Override
    public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {
        if (dictionary != null) {
            PropertiesManager pm = new PropertiesManager(getMap(dictionary));
            PropertiesValues values = pm.getValues();
            // TODO handle value extraction here

            connection = new ElasticsearchConnection("myId");
            connection.setPort(9200);
            connection.setUser("elastic");
            connection.setPassword("root1234");
            connection.setHost("elasticsearch");
            connection.setUseXPackSecurity(true);
            connection.setUseEncryption(true);
            //connection.setAdditionalHostAddresses();

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
}
