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

    public ElasticSearchConnectionData(String id) {
        this.id = id;
    }

    @JsonIgnore
    public String getJson() {
        ObjectMapper mapper = new ObjectMapper();
        final JsonNode jsonElasticSearchConnectionData = mapper.valueToTree(this);
        return jsonElasticSearchConnectionData != null ? jsonElasticSearchConnectionData.toString() : null;
    }
}
