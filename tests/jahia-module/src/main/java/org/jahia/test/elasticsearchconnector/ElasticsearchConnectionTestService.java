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
 *     Copyright (C) 2002-2025 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.test.elasticsearchconnector;

import org.jahia.modules.elasticsearchconnector.service.ElasticsearchClientWrapper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ElasticsearchConnectionTestService.class, immediate = true)
public class ElasticsearchConnectionTestService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConnectionTestService.class);

    @Reference
    private ElasticsearchClientWrapper elasticsearchClient;

    @Activate
    public void activate() {
        logger.info("Activate ElasticsearchConnectionTestService and test existing elasticsearch connection...");
        elasticsearchClient.testConnection();
        logger.info("Elasticsearch connection test completed.");
    }
}
