package org.jahia.modules.elasticsearchconnector.http;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by stefan on 2017-05-30.
 */
public interface TransportClientService {

    public JSONObject getStatus() throws JSONException;

    public boolean testConnection();
}
