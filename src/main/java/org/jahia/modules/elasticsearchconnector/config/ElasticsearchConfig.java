package org.jahia.modules.elasticsearchconnector.config;

import org.jahia.modules.elasticsearchconnector.rest.ElasticsearchConnection;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component(
        configurationPid = "org.jahia.modules.elasticsearchConnector",
        service = {ElasticsearchConfig.class},
        immediate = true,
        property = {
                "service.description=Elasticsearch configuration service",
                "service.vendor=Jahia Solutions Group SA"
        }
)
@Designate(ocd = ElasticsearchConfigMetatype.class)
public class ElasticsearchConfig {

    private final AtomicReference<ElasticsearchConnection> connection = new AtomicReference<>();

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public ElasticsearchConfig() {
        super();
    }

    @Deactivate
    protected void deactivate() {
        logger.info("Shutting down Elasticsearch connection...");
        connection.set(null);
    }

    @Activate
    @Modified
    protected void activate(ElasticsearchConfigMetatype config) {
        logger.info("Activating/Updating elasticsearch configuration...");
        ElasticsearchConnection connConfig = new ElasticsearchConnection();
        connConfig.setHost(config.elasticsearchConnector_host());
        connConfig.setPort(config.elasticsearchConnector_port());
        connConfig.setUser(config.elasticsearchConnector_user());
        connConfig.setPassword(config.elasticsearchConnector_password());
        connConfig.setUseXPackSecurity(config.elasticsearchConnector_useXPackSecurity());
        connConfig.setUseEncryption(config.elasticsearchConnector_useEncryption());
        connConfig.setSnifferInterval(config.elasticsearchConnector_snifferInterval());

        // Handle additionalHostAddresses as a String array
        String[] additionalHosts = config.elasticsearchConnector_additionalHostAddresses();
        if (additionalHosts != null && additionalHosts.length > 0) {
            List<String> hosts = Arrays.stream(additionalHosts)
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .collect(Collectors.toList());
            connConfig.setAdditionalHostAddresses(hosts);
        } else {
            connConfig.setAdditionalHostAddresses(new ArrayList<>());
        }
        connection.set(connConfig);
        logger.info("Elasticsearch connection configuration updated.");
    }

    public ElasticsearchConnection getConnectionConfig() {
        return connection.get();
    }
}
