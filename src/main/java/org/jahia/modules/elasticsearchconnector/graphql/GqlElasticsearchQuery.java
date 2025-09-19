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
package org.jahia.modules.elasticsearchconnector.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import javax.inject.Inject;
import org.jahia.modules.elasticsearchconnector.service.ElasticsearchClientWrapper;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;

@GraphQLName("GqlElasticsearchQuery")
@GraphQLDescription("Elasticsearch queries object")
public class GqlElasticsearchQuery {

    @Inject
    @GraphQLOsgiService(service = ElasticsearchClientWrapper.class)
    private ElasticsearchClientWrapper elasticsearchClient;

    @GraphQLField
    @GraphQLDescription("Return true if an elasticsearch connection can be established " +
        "from existing configuration, false otherwise.\nCheck logs for more details.")
    public boolean testConnection() {
        return elasticsearchClient.testConnection();
    }
}
