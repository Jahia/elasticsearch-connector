package org.jahia.modules.elasticsearchconnector.api;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.jahia.modules.databaseConnector.api.filters.HeadersResponseFilter;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

import org.jahia.osgi.BundleUtils;


/**
 * 2017-05-17
 *
 * @author Astrit Ademi
 */
public class ECApiApplication extends Application {

    private final Set<Object> singletons;

    public ECApiApplication() {
        //Add Main Resource
        ECApi api = BundleUtils.getOsgiService(ECApi.class, null);
        singletons = new HashSet<>();
        singletons.add(api);
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<Class<?>>();
        // register resources and features
        classes.add(MultiPartFeature.class);
        classes.add(LoggingFilter.class);
        classes.add(JacksonJaxbJsonProvider.class);
        classes.add(HeadersResponseFilter.class);
        return classes;
    }
}
