package org.jahia.modules.elasticsearchConnector.connection;

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

    private String writeConcern;

    private String authDb;

    public ElasticSearchConnectionData(String id) {
        this.id = id;
    }

    public String getWriteConcern() {
        return this.writeConcern;
    }

    public void setWriteConcern(String writeConcern) {
        this.writeConcern = writeConcern;
    }

    public String getAuthDb() {
        return this.authDb;
    }

    public void setAuthDb(String authDb) {
        this.authDb = authDb;
    }

    @JsonIgnore
    public String getJson() {
        ObjectMapper mapper = new ObjectMapper();
        final JsonNode jsonElasticSearchConnectionData = mapper.valueToTree(this);
        return jsonElasticSearchConnectionData != null ? jsonElasticSearchConnectionData.toString() : null;
    }
}
