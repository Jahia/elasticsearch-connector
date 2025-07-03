package org.jahia.modules.elasticsearchconnector.rest;

import org.jahia.modules.elasticsearchconnector.ESConstants;
import org.jahia.modules.elasticsearchconnector.connection.ElasticSearchConnection;
import org.jahia.osgi.FrameworkService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@Component(service = {ElasticsearchConnectionRegistry.class}, immediate = true, scope = ServiceScope.SINGLETON)
public class ElasticsearchConnectionRegistryImpl implements ElasticsearchConnectionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConnectionRegistryImpl.class);

    private BundleContext ctx;
    private ServiceRegistration registration;
    private ElasticSearchConnection connection;

    @Activate
    public void activate(BundleContext ctx) {
        this.ctx = ctx;
    }

    public void registerAsService(ElasticSearchConnection connection) {
        // We don't actually know if it is connected need a check to make sure it can communicate with ES
        // Need to test for connectivity before and return true or false
        Object service = connection.beforeRegisterAsService();
        this.connection = connection;
        this.registration = registerAsService((ElasticsearchClientWrapper) service, connection);
        connection.isConnected(true);

        FrameworkService.sendEvent(ESConstants.EVENT_TOPIC,
                Collections.singletonMap("type", "elasticsearchClientAvailable"), true);
    }

    private ServiceRegistration registerAsService(ElasticsearchClientWrapper object, ElasticSearchConnection connection) {
        String[] messageArgs = new String[]{object.getClass().getSimpleName(), connection.getDisplayName(), connection.getId()};
        logger.info("Start registering OSGi service for {} for Elasticsearch connection of type {} with id '{}'", messageArgs);

        ServiceReference[] serviceReferences;
        try {
            serviceReferences = ctx.getAllServiceReferences(ElasticsearchClientWrapper.class.getName(), null);
        } catch (InvalidSyntaxException var6) {
            logger.error(var6.getMessage(), var6);
            return null;
        }

        if (serviceReferences != null) {
            logger.info("OSGi service for {} already registered for Elasticsearch connection of type {} with id '{}'", messageArgs);
            return null;
        } else {
            //TODO add props and use them on filter
            ServiceRegistration serviceRegistration = ctx.registerService(ElasticsearchClientWrapper.class.getName(), object, null);
            logger.info("OSGi service for {} successfully registered for Elasticsearch connection of type {} with id '{}'", messageArgs);
            return serviceRegistration;
        }
    }

    public void unregisterAsService() {
        if (connection != null) {
            connection.beforeUnregisterAsService();
            connection = null;
        }

        if (registration != null) {
            registration.unregister();
        }

        FrameworkService.sendEvent(ESConstants.EVENT_TOPIC,
                Collections.singletonMap("type", "elasticsearchClientUnregistered"), true);
    }
}
