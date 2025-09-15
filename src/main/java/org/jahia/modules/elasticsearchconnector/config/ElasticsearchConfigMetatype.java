package org.jahia.modules.elasticsearchconnector.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import static org.jahia.modules.elasticsearchconnector.ESConstants.*;

@ObjectClassDefinition(
        name = "Elasticsearch Connector Configuration",
        description = "Configuration for the Elasticsearch Connector module."
)
public @interface ElasticsearchConfigMetatype {

    @AttributeDefinition(
            name = "Host",
            description = "The hostname of the Elasticsearch server",
            defaultValue = DEFAULT_HOST
    )
    String elasticsearchConnector_host() default DEFAULT_HOST;

    @AttributeDefinition(
            name = "Port",
            description = "The port of the Elasticsearch server",
            defaultValue = DEFAULT_PORT_STR
    )
    int elasticsearchConnector_port() default DEFAULT_PORT;

    @AttributeDefinition(
            name = "User",
            description = "The username for Elasticsearch authentication",
            defaultValue = DEFAULT_USER
    )
    String elasticsearchConnector_user() default DEFAULT_USER;

    @AttributeDefinition(
            name = "Password",
            description = "The encoded password for Elasticsearch authentication"
    )
    String elasticsearchConnector_password() default "";

    @AttributeDefinition(
            name = "Use XPack Security",
            description = "Enable XPack security integration",
            defaultValue = DEFAULT_USE_XPACK_SECURITY_STR
    )
    boolean elasticsearchConnector_useXPackSecurity() default DEFAULT_USE_XPACK_SECURITY;

    @AttributeDefinition(
            name = "Use Encryption",
            description = "Enable encryption for Elasticsearch connections",
            defaultValue = DEFAULT_USE_ENCRYPTION_STR
    )
    boolean elasticsearchConnector_useEncryption() default DEFAULT_USE_ENCRYPTION;

    @AttributeDefinition(
            name = "Nodes Sniffer Interval",
            description = "Interval for node sniffing (e.g., 5s)",
            defaultValue = "5s"
    )
    String elasticsearchConnector_snifferInterval() default DEFAULT_SNIFFER_INTERVAL;

    @AttributeDefinition(
            name = "Additional Host Addresses",
            description = "List of additional Elasticsearch host addresses. Each entry represents a separate host"
    )
    String[] elasticsearchConnector_additionalHostAddresses() default {};
}
