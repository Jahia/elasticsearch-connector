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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jahia.modules.databaseConnector.connection.ConnectionData;

/**
 * 2017-05-17
 *
 * @author Astrit Ademi
 */

public class ElasticSearchConnectionData extends ConnectionData {
    private static final long serialVersionUID = 1;

    /**
     * Instantiate a new Elasticsearch Connection with the specified identifier
     * @param id the identifier of the instance
     */
    public ElasticSearchConnectionData(String id) {
        this.id = id;
    }

    @JsonIgnore
    @Override
    public String getJson() {
        ObjectMapper mapper = new ObjectMapper();
        final JsonNode jsonElasticSearchConnectionData = mapper.valueToTree(this);
        return jsonElasticSearchConnectionData != null ? jsonElasticSearchConnectionData.toString() : null;
    }
}
