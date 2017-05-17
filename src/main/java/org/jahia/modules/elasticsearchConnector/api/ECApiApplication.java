package org.jahia.modules.elasticsearchConnector.api;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.jahia.modules.databaseConnector.api.filters.HeadersResponseFilter;

/**
 * 2017-05-17
 *
 * @author Astrit Ademi
 */
public class ECApiApplication extends ResourceConfig {


    public ECApiApplication() {
        super(
                ECApi.class,
                JacksonJaxbJsonProvider.class,
                HeadersResponseFilter.class,
                MultiPartFeature.class,
                LoggingFilter.class
        );
    }
}
