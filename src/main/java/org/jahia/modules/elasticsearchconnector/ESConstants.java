package org.jahia.modules.elasticsearchconnector;

public final class ESConstants {

    private ESConstants() {
        throw new AssertionError("Class should not be instantiated");
    }

    public static final String EVENT_TOPIC = "org/jahia/modules/elasticsearch-connector";
    public static final String DB_TYPE = "ELASTICSEARCH";
    public static final String DB_ID = "ELASTICSEARCH_CONNECTION";

    /* Default ES config values */
    public static final String DEFAULT_HOST = "elasticsearch";
    public static final String DEFAULT_PORT_STR = "9200";
    public static final int DEFAULT_PORT = 9200;
    public static final String DEFAULT_USE_XPACK_SECURITY_STR = "false";
    public static final boolean DEFAULT_USE_XPACK_SECURITY = false;
    public static final String DEFAULT_USE_ENCRYPTION_STR = "false";
    public static final boolean DEFAULT_USE_ENCRYPTION = false;
    public static final String DEFAULT_SNIFFER_INTERVAL = "5s";
    public static final String DEFAULT_USER = "elastic";
}
