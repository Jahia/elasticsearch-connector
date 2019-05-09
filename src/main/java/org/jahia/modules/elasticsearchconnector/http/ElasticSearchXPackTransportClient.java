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
 *     Copyright (C) 2002-2019 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.elasticsearchconnector.http;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * This is a special client wrapper that uses a built-in class loader to avoid loading the X-Pack classes into the
 * global class loader.
 */
public class ElasticSearchXPackTransportClient extends AbstractTransportClientService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchXPackTransportClient.class);

    private ChildFirstClassLoader childFirstClassLoader = null;

    public ElasticSearchXPackTransportClient(Settings.Builder settingsBuilder, String transportClientClassName,
                                             String transportClientJarDirectory,
                                             String transportClientProperties,
                                             Class<? extends Plugin>... plugins) {
        transportClient = newTransportClient(settingsBuilder, transportClientClassName, transportClientJarDirectory, transportClientProperties, plugins);
    }

    private TransportClient newTransportClient(Settings.Builder settingsBuilder,
                                              String transportClientClassName,
                                              String transportClientJarDirectory,
                                              String transportClientProperties,
                                              Class<? extends Plugin>... plugins) {

        ArrayList<URL> urls = new ArrayList<>();
        File pluginLocationFile = new File(transportClientJarDirectory);

        File[] pluginLocationFiles = pluginLocationFile.listFiles();
        if (pluginLocationFiles != null) {
            for (File pluginFile : pluginLocationFiles) {
                if (pluginFile.getName().toLowerCase().endsWith(".jar")) {
                    try {
                        urls.add(pluginFile.toURI().toURL());
                    } catch (MalformedURLException e) {
                        logger.error("Error adding plugin JAR URL", e);
                    }
                }
            }
        }

        if (childFirstClassLoader == null) {
            childFirstClassLoader = new ChildFirstClassLoader(this.getClass().getClassLoader(), urls.toArray(new URL[0]));
        }

        if (StringUtils.isNotBlank(transportClientProperties)) {
            String[] clientProperties = transportClientProperties.split(",");
            if (clientProperties.length > 0) {
                for (String clientProperty : clientProperties) {
                    String[] clientPropertyParts = clientProperty.split("=");
                    settingsBuilder.put(clientPropertyParts[0], clientPropertyParts[1]);
                }
            }
        }

        try {
            Class<?> transportClientClass = childFirstClassLoader.loadClass(transportClientClassName);
            Constructor<?> transportClientConstructor = transportClientClass.getConstructor(Settings.class, Class[].class);
            return (TransportClient) transportClientConstructor.newInstance(settingsBuilder.build(), plugins);
        } catch (ClassNotFoundException e) {
            logger.error("Couldn't find class " + transportClientClassName, e);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            logger.error("Error creating transport client with class" + transportClientClassName, e);
        }
        return null;
    }

    @Override
    public void close() {
        super.close();
        if (childFirstClassLoader != null) {
            childFirstClassLoader = null;
        }
    }
}
