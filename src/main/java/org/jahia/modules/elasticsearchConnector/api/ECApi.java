package org.jahia.modules.elasticsearchConnector.api;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.databaseConnector.connection.DatabaseConnectionAPI;
import org.jahia.modules.databaseConnector.services.DatabaseConnectorService;
import org.jahia.modules.elasticsearchConnector.connection.ElasticSearchConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

    @GET
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHello() {
        return Response.status(Response.Status.OK).entity("{\"success\":\"Successfully setup ECApi\"}").build();
    }

    @GET
    @Path("/getconnections")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConnections() {
        try {
            return Response.status(Response.Status.OK).entity(databaseConnectorService.getConnections(ElasticSearchConnection.DATABASE_TYPE)).build();
        } catch (InstantiationException ex) {
            logger.error("Cannot instantiate connection class" + ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"Cannot access connection\"}").build();
        } catch (IllegalAccessException ex) {
            logger.error("Cannot access connection class" + ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"Cannot access connection\"}").build();
        }
    }

//    @POST
//    @Path("/add")
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response addConnection(String data) {
//        try {
//            JSONObject connectionParameters = new JSONObject(data);
//            JSONArray missingParameters = new JSONArray();
//            if (!connectionParameters.has("id") || StringUtils.isEmpty(connectionParameters.getString("id"))) {
//                missingParameters.put("id");
//            }
//            if (!connectionParameters.has("host") || StringUtils.isEmpty(connectionParameters.getString("host"))) {
//                missingParameters.put("host");
//            }
//            if (!connectionParameters.has("dbName") || StringUtils.isEmpty(connectionParameters.getString("dbName"))) {
//                missingParameters.put("dbName");
//            }
//            if (missingParameters.length() > 0) {
//                return Response.status(Response.Status.BAD_REQUEST).entity("{\"missingParameters\":" + missingParameters.toString() + "}").build();
//            } else {
//                String id = connectionParameters.has("id") ? connectionParameters.getString("id") : null;
//                String host = connectionParameters.has("host") ? connectionParameters.getString("host") : null;
//                Integer port = connectionParameters.has("port") && !StringUtils.isEmpty(connectionParameters.getString("port")) ? connectionParameters.getInt("port") : null;
//                Boolean isConnected = connectionParameters.has("isConnected") && connectionParameters.getBoolean("isConnected");
//                String dbName = connectionParameters.has("dbName") ? connectionParameters.getString("dbName") : null;
//                String user = connectionParameters.has("user") ? connectionParameters.getString("user") : null;
//                String password = connectionParameters.has("password") ? connectionParameters.getString("password") : null;
//                String authDb = connectionParameters.has("authDb") ? connectionParameters.getString("authDb") : null;
//                String options = connectionParameters.has("options") ? connectionParameters.getString("options") : null;
//                ElasticSearchConnection connection = new ElasticSearchConnection(id);
//                connection.setHost(host);
//                connection.setPort(port);
//                connection.isConnected(isConnected);
//                connection.setDbName(dbName);
//                connection.setUser(user);
//                connection.setPassword(password);
////                if (connectionParameters.has("writeConcern") && !StringUtils.isEmpty(connectionParameters.getString("writeConcern"))) {
////                   connection.setWriteConcern(connectionParameters.getString("writeConcern"));
////                }
//                // connection.setAuthDb(authDb);
//                connection.setOptions(options);
//                JSONObject jsonAnswer = new JSONObject();
//                if (!databaseConnectorService.testConnection(connection)) {
//                    connection.isConnected(false);
//                    jsonAnswer.put("connectionVerified", false);
//                } else {
//                    jsonAnswer.put("connectionVerified", true);
//                }
//                databaseConnectorService.addEditConnection(connection, false);
//                jsonAnswer.put("success", "Connection successfully added");
//                logger.info("Successfully created ElasticSearchDB connection: " + id);
//                return Response.status(Response.Status.OK).entity(jsonAnswer.toString()).build();
//            }
//        } catch (JSONException e) {
//            logger.error("Cannot parse json data : {}", data);
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"Cannot parse json data\"}").build();
//        }
//    }
}
