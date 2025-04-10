/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.elasticsearchconnector.connection;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.elasticsearchconnector.ESConstants;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.utils.EncryptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 2017-05-17
 *
 * @author Astrit Ademi
 */
public class ElasticSearchConnectionRegistry {

    private static Logger logger = LoggerFactory.getLogger(ElasticSearchConnectionRegistry.class);

    private static Map<String, ServiceRegistration> serviceRegistrations = new LinkedHashMap();


    public static boolean registerAsService(AbstractConnection connection, BundleContext bundleContext) {
        Object service = connection.beforeRegisterAsService();
        return registerAsService(service, connection, bundleContext);
    }

    private static boolean registerAsService(Object object, AbstractConnection connection, BundleContext bundleContext) {
        String[] messageArgs = new String[]{object.getClass().getSimpleName(), connection.getId()};
        logger.info("Start registering OSGi service for {} for DatabaseConnection of type {} with id '{}'", messageArgs);

        ServiceReference[] serviceReferences;
        try {
            serviceReferences = bundleContext.getAllServiceReferences(ConnectionService.class.getName(), createFilter(connection.getId()));
        } catch (InvalidSyntaxException var6) {
            logger.error(var6.getMessage(), var6);
            return false;
        }

        if (serviceReferences != null) {
            logger.info("OSGi service for {} already registered for DatabaseConnection of type {} with id '{}'", messageArgs);
            return true;
        } else {
            ServiceRegistration serviceRegistration = bundleContext.registerService(getInterfacesNames(object), object, createProperties(connection.getId()));
            serviceRegistrations.put(connection.getId(), serviceRegistration);
            logger.info("OSGi service for {} successfully registered for DatabaseConnection of type {} with id '{}'", messageArgs);
            return true;
        }
    }

    public static void unregisterAsService(AbstractConnection connection) {
        connection.beforeUnregisterAsService();
        logger.info("Start unregistering OSGi services for Elasticseach connection with id '{}'", connection.getId());
        ServiceRegistration serviceRegistration = (ServiceRegistration) serviceRegistrations.get(connection.getId());
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistrations.remove(connection.getId());
        }
        logger.info("OSGi services successfully unregistered for Elasticseach connection with id '{}'", connection.getId());
    }

    private static String[] getInterfacesNames(Object obj) {
        List<Class<?>> interfaces = ClassUtils.getAllInterfaces(obj.getClass());
        List<String> interfacesNames = new ArrayList();
        Iterator var4 = interfaces.iterator();

        while(var4.hasNext()) {
            Class<?> interfaceClass = (Class)var4.next();
            interfacesNames.add(interfaceClass.getName());
        }

        return (String[])interfacesNames.toArray(new String[0]);
    }

    private static Hashtable<String, String> createProperties(String connectionId) {
        Hashtable<String, String> properties = new Hashtable();
        properties.put("connectionId", connectionId);
        return properties;
    }

    public static String createFilter(String connectionId) {
        return "(&(" + "connectionId" + "=" + connectionId + "))";
    }
}
