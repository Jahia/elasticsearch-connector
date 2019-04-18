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
 * Created by stefan on 2017-05-30.
 */
public interface TransportClientService {

    JSONObject getStatus() throws JSONException;

    boolean testConnection();

    /**
     * Retrieves the underlying ElasticSearch transport client. This client may be different depending on whether the
     * standard TransportClient is used or if the X-Pack transport client is used.
     * @return
     */
    TransportClient getTransportClient();
}
