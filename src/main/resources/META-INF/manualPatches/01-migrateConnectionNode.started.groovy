import org.jahia.osgi.BundleUtils
import org.jahia.services.content.JCRTemplate
import org.jahia.services.content.JCRNodeWrapper
import org.jahia.services.content.JCRSessionWrapper
import javax.jcr.query.Query

import groovy.json.JsonSlurper

/**
 * Migration script to transfer Elasticsearch connection settings from JCR nodes to OSGI configuration.
 * Note that we still keep the JCR connection node after migration.
 */

// Optional: Fill in this variable to migrate a specific connection ID (dc:id node property)
// Otherwise, leaving it empty will migrate a single connection or do nothing if there's more than one.
String connectionId = "";

/**
 * Finds an Elasticsearch connection node based on optional connection ID
 * @param session The JCR session
 * @param connectionId Optional connection ID to find a specific connection
 * @return The connection node or null if not found or if multiple found
 */
def findElasticsearchConnectionNode = {JCRSessionWrapper session -> {
    // Query for all nodes of type ec:elasticsearchConnection
    String queryString = "SELECT * FROM [ec:elasticsearchConnection] WHERE [dc:databaseType] = 'ELASTICSEARCH'";

    // If connectionId is specified, add constraint for dc:id
    boolean hasConnectionIdFilter = connectionId != null && !connectionId.isEmpty()
    if (hasConnectionIdFilter) {
        log.info("Querying with specified connection ID: {}", connectionId);
        queryString += ' AND [dc:id] = $connectionId' // Use single quotes to avoid groovy interpolation
    }

    Query query = session.getWorkspace().getQueryManager().createQuery(queryString, Query.JCR_SQL2)
    if (hasConnectionIdFilter) {
        query.bindValue("connectionId", session.getValueFactory().createValue(connectionId));
    }
    query.setLimit(2)
    def nodes = query.execute().getNodes()

    // Check if there are multiple connection nodes
    int nodeCount = 0
    JCRNodeWrapper connectionNode = null

    while (nodes.hasNext()) {
        nodeCount++
        if (nodeCount == 1) {
            connectionNode = nodes.nextNode()
        } else {
            // More than one node found
            log.warn("Multiple Elasticsearch connection nodes found. Migration aborted.")
            return null
        }
    }

    if (nodeCount == 0) {
        log.info("No Elasticsearch connection nodes found. Nothing to migrate.")
        return null
    }

    // We have exactly one connection node, proceed with migration
    log.info("Found one Elasticsearch connection node. Proceeding with migration.")
    return connectionNode
}}

/**
 * Extracts connection properties from the JCR node
 * @param connectionNode The JCR node containing Elasticsearch connection properties
 * @return Map with extracted connection properties
 */
def extractConnectionProperties = {JCRNodeWrapper connectionNode -> {
    Map<String, Object> properties = [:]

    // Extract basic connection properties
    properties.id = connectionNode.hasProperty("dc:id") ? connectionNode.getPropertyAsString("dc:id") : null
    properties.host = connectionNode.hasProperty("dc:host") ? connectionNode.getPropertyAsString("dc:host") : null
    properties.port = connectionNode.hasProperty("dc:port") ? connectionNode.getPropertyAsString("dc:port") : null
    properties.user = connectionNode.hasProperty("dc:user") ? connectionNode.getPropertyAsString("dc:user") : null
    properties.password = connectionNode.hasProperty("dc:password") ? connectionNode.getPropertyAsString("dc:password") : null
    properties.options = connectionNode.hasProperty("dc:options") ? connectionNode.getPropertyAsString("dc:options") : null

    // Parse JSON options if available
    if (properties.options && !properties.options.isEmpty()) {
        log.info("Found connection options: {}", properties.options)
        try {
            def jsonSlurper = new JsonSlurper()
            def optionsMap = jsonSlurper.parseText(properties.options as String)

            // Extract boolean options
            if (optionsMap.containsKey("useXPackSecurity")) {
                properties.useXPackSecurity = Boolean.valueOf(optionsMap.useXPackSecurity)
                log.info("Found useXPackSecurity option: {}", properties.useXPackSecurity)
            }

            if (optionsMap.containsKey("useEncryption")) {
                properties.useEncryption = Boolean.valueOf(optionsMap.useEncryption)
                log.info("Found useEncryption option: {}", properties.useEncryption)
            }

            // Extract nodesSnifferInterval
            if (optionsMap.containsKey("nodesSnifferInterval")) {
                properties.nodesSnifferInterval = optionsMap.nodesSnifferInterval
                log.info("Found nodesSnifferInterval option: {}", properties.nodesSnifferInterval)
            }

            // Extract and process additional host addresses
            if (optionsMap.containsKey("additionalHostAddresses") && optionsMap.additionalHostAddresses instanceof List) {
                properties.additionalHostAddresses = []
                optionsMap.additionalHostAddresses.each { hostObj ->
                    if (hostObj instanceof Map) {
                        String host = hostObj.host
                        String port = hostObj.port
                        if (host) {
                            if (port) {
                                // Convert GString to Java String to avoid ConfigAdmin errors
                                String hostWithPort = host + ":" + port
                                properties.additionalHostAddresses.add(hostWithPort)
                                log.info("Found additional host: {}:{}", host, port)
                            } else {
                                properties.additionalHostAddresses.add(String.valueOf(host))
                                log.info("Found additional host (no port): {}", host)
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing options JSON: {}. Will use default values.", e.getMessage())
        }
    }
    return properties
}}

/**
 * Add {propertyName: value} in props, if value is not null; otherwise, remove propertyName key in props
 */
def handleProperty = {Dictionary props, String propertyName, Object value -> {
    if (value != null) {
        props.put(propertyName, value)
        // Avoid logging sensitive info
        if (propertyName != "elasticsearchConnector.password") {
            log.info("Setting {} property: {}", propertyName, value)
        }
    } else if (props.get(propertyName) != null) {
        // If value is null but property exists in config, remove it
        props.remove(propertyName)
        log.info("Removing {} property as it's not in extracted props", propertyName)
    }
}}

def migrationCallback = {session ->
    try {
        // Find the Elasticsearch connection node
        JCRNodeWrapper connectionNode = findElasticsearchConnectionNode(session)

        // If no connection node found, exit
        if (connectionNode == null) {
            return null
        }

        // Extract connection properties from the JCR node
        Map<String, Object> connectionProps = extractConnectionProperties(connectionNode)

        // Log the migration (without sensitive information)
        log.info("Migrating Elasticsearch connection with ID={}", connectionProps.id)

        try {
            // Get the configuration admin through OSGI service
            def configAdmin = BundleUtils.getOsgiService("org.osgi.service.cm.ConfigurationAdmin", null)
            if (configAdmin == null) {
                log.error("Could not get ConfigurationAdmin service. Migration aborted.")
                return null
            }

            def configs = configAdmin.listConfigurations("(service.pid=org.jahia.modules.elasticsearchConnector)")
            if (configs == null || configs.length <= 0) {
                return null;
            }
            def configuration = configs[0]

            Dictionary props = configuration.getProperties()
            if (props == null) {
                props = new java.util.Hashtable<>()
            }

            // Update properties - either set if not null, or remove if null and exists in config
            handleProperty(props, "elasticsearchConnector.host", connectionProps.host);
            handleProperty(props, "elasticsearchConnector.port", connectionProps.port);
            handleProperty(props, "elasticsearchConnector.user", connectionProps.user);
            handleProperty(props, "elasticsearchConnector.password", connectionProps.password);
            handleProperty(props, "elasticsearchConnector.useXPackSecurity", connectionProps.useXPackSecurity);
            handleProperty(props, "elasticsearchConnector.useEncryption", connectionProps.useEncryption);
            handleProperty(props, "elasticsearchConnector.snifferInterval", connectionProps.nodesSnifferInterval);

            // Handle additional host addresses specially since it's an array
            if (connectionProps.additionalHostAddresses && !connectionProps.additionalHostAddresses.isEmpty()) {
                // Convert the list to a Vector which is compatible with OSGI config
                java.util.Vector<String> addressVector = new java.util.Vector<String>(connectionProps.additionalHostAddresses)
                props.put("elasticsearchConnector.additionalHostAddresses", addressVector)

                // Log each address for debugging
                connectionProps.additionalHostAddresses.eachWithIndex { address, index ->
                    log.info("  additionalHostAddress[{}]: {}", index, address)
                }
            } else if (props.get("elasticsearchConnector.additionalHostAddresses") != null) {
                // Remove the property if it exists in config but is null/empty in extracted props
                props.remove("elasticsearchConnector.additionalHostAddresses")
                log.info("Removing elasticsearchConnector.additionalHostAddresses property as it's not in extracted props")
            }

            // Update the configuration
            configuration.update(props);

            log.info("Successfully migrated Elasticsearch connection settings to OSGI configuration.")
        } catch (Exception e) {
            log.error("Error during ConfigurationAdmin operations: {}", e.getMessage())
            e.printStackTrace()
            throw e;
        }

        return null
    } catch (Exception e) {
        log.error("Error during Elasticsearch connection migration: {}", e.getMessage())
        e.printStackTrace()
        throw e;
    }
}

log.info("Starting migration of Elasticsearch connection from JCR to OSGI configuration")
JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, "default", null, migrationCallback)
log.info("Elasticsearch connection migration completed")
