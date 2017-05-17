package org.jahia.modules.elasticsearchConnector.api;

import org.jahia.modules.databaseConnector.connection.DatabaseConnectionAPI;
import org.jahia.modules.databaseConnector.services.DatabaseConnectorService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * 2017-05-17
 *
 * @author Astrit Ademi
 */

public class ECApi extends DatabaseConnectionAPI {
    private static final Logger logger = LoggerFactory.getLogger(ECApi.class);
    private BundleContext context;
    public final static String ENTRY_POINT = "/elasticsearch";
    private DatabaseConnectorService databaseConnectorService;


    public ECApi(Class apiClass) {
        super(apiClass);
        databaseConnectorService = getDatabaseConnector();
    }
}
