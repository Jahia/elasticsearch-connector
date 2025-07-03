package org.jahia.modules.elasticsearchconnector.config;

import org.jahia.modules.elasticsearchconnector.connection.ElasticSearchConnection;
import org.jahia.modules.elasticsearchconnector.rest.ElasticsearchConnectionRegistry;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.util.*;

@Component(service = {ManagedService.class, ElasticsearchConfig.class}, property = {
        "service.pid=org.jahia.modules.elasticsearch_config",
        "service.description=Elasticsearch configuration service",
        "service.vendor=Jahia Solutions Group SA"
}, immediate = true)
public class ElasticsearchConfig implements ManagedService {

    private ElasticsearchConnectionRegistry elasticsearchConnectionRegistry;

    public ElasticsearchConfig() {
        super();
    }

    @Activate
    public void activate() {
    }

    @Deactivate
    public void deactivate() {
        if (elasticsearchConnectionRegistry != null) {
            elasticsearchConnectionRegistry.unregisterAsService();
        }
    }

    @Reference
    public void setConnectionRegistry(ElasticsearchConnectionRegistry connectionRegistry) {
        this.elasticsearchConnectionRegistry = connectionRegistry;
    }

    @Override
    public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {
        if (dictionary != null) {
            elasticsearchConnectionRegistry.unregisterAsService();

            ElasticSearchConnection con = new ElasticSearchConnection("myId");
            con.setPort(9200);
            con.setUser("elastic");
            con.setPassword("root1234");
            con.setHost("elasticsearch");
            con.setOptions("{useXPackSecurity: true}");

            elasticsearchConnectionRegistry.registerAsService(con);
        }
    }
}
