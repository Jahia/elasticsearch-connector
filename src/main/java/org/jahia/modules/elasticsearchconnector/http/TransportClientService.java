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

import org.elasticsearch.client.transport.TransportClient;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Common interface to use underlying ElasticSearch TransportClient instances
 */
public interface TransportClientService {

    /**
     * Get the status of the underlying ElasticSearch cluster. Returns a map with the name of the cluster and its
     * status (RED, YELLOW, GREEN)
     * @return a map with two entries : "clusterName" contains a String with the name of the cluster, and "status"
     * contains a String with the name of the cluster status so either "RED", "YELLOW" or "GREEN"
     * @throws JSONException in case there was a problem generating the JSON object
     */
    JSONObject getStatus() throws JSONException;

    /**
     * Tests the connection by trying to retrieve the ElasticSearch cluster status.
     * @return true if the cluster status could be retrieved, false otherwise
     */
    boolean testConnection();

    /**
     * Retrieves the underlying ElasticSearch transport client. This client may be different depending on whether the
     * standard TransportClient is used or if the X-Pack transport client is used.
     * @return an instance of a regular or an X-Pack TransportClient
     */
    TransportClient getTransportClient();

    /**
     * Used to close properly the underlying ElasticSearch TransportClient as well as cleanup any other remaining used
     * objects.
     */
    void close();

}
