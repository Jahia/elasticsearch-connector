package org.jahia.modules.elasticsearchconnector.config;

import org.jahia.modules.elasticsearchconnector.connection.AbstractConnection;
import org.jahia.modules.elasticsearchconnector.connection.ElasticSearchConnection;
import org.jahia.modules.elasticsearchconnector.connection.ElasticSearchConnectionRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

@Component(service = {ManagedService.class, ElasticsearchConnectorConfig.class}, property = {
        "service.pid=org.jahia.modules.elasticsearchconnector",
        "service.description=Elasticsearch Connector configuration service",
        "service.vendor=Jahia Solutions Group SA"
}, immediate = true)
public class ElasticsearchConnectorConfig implements ManagedService {
    protected static final Logger logger = LoggerFactory.getLogger(ElasticsearchConnectorConfig.class);
    private ElasticSearchConnection elasticSearchConnection;
    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        logger.info("Elasticsearch connector config activated");
    }

    @Override
    public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {
        if (dictionary == null) {
            return;
        }

        if (elasticSearchConnection != null) {
            ElasticSearchConnectionRegistry.unregisterAsService(elasticSearchConnection);
            elasticSearchConnection = null;
        }

        elasticSearchConnection = new ElasticSearchConnection(getString(dictionary, AbstractConnection.PROPERTY_ID, "elasticsearchconnection"));
        elasticSearchConnection.setHost(getString(dictionary, AbstractConnection.PROPERTY_HOST, "elasticsearch"));
        elasticSearchConnection.setPort(getInt(dictionary, AbstractConnection.PROPERTY_PORT, 9200));
        elasticSearchConnection.setUser(getString(dictionary, AbstractConnection.PROPERTY_USER, null));
        elasticSearchConnection.setPassword(getString(dictionary, AbstractConnection.PROPERTY_PASSWORD, null));
        elasticSearchConnection.setUseXPackSecurity(getBoolean(dictionary, AbstractConnection.PROPERTY_USEXPACKSECURITY, false));
        elasticSearchConnection.setUseEncryption(getBoolean(dictionary, AbstractConnection.PROPERTY_USEENCRYPTION, false));
        elasticSearchConnection.setNodesSnifferInterval(getString(dictionary, AbstractConnection.PROPERTY_NODESSNIFFERINTERVAL, "0"));

        //todo additional hosts ... is it necessary???
        //Note that timeout value may be too much to do this here
        if (elasticSearchConnection.testConnectionCreation()) {
            logger.info("Successfully tested elasticsearch connection and will create a client.");
            if (ElasticSearchConnectionRegistry.registerAsService(elasticSearchConnection, bundleContext)) {
                logger.info("Successfully registered connection service");
            } else {
                logger.warn("Connection registration failed");
            }
        } else {
            logger.warn("Failed to connect to elasticsearch after updating configuration. Make sure you provide valid configuration parameters. In the meantime no connection will be available.");
        }
    }



    private int getInt(Dictionary<String, ?> properties, String key, int def) {
        if (properties.get(key) != null) {
            Object val = properties.get(key);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            } else if (val != null) {
                return Integer.parseInt(val.toString());
            }
        }
        return def;
    }

    private boolean getBoolean(Dictionary<String, ?> properties, String key, boolean def) {
        if (properties.get(key) != null) {
            Object val = properties.get(key);
            if (val instanceof Boolean) {
                return (Boolean) val;
            } else if (val != null) {
                return Boolean.parseBoolean(val.toString());
            }
        }
        return def;
    }

    private String getString(Dictionary<String, ?> properties, String key, String def) {
        if (properties.get(key) != null) {
            Object val = properties.get(key);
            if (val != null) {
                return val.toString();
            }
        }
        return def;
    }
}
