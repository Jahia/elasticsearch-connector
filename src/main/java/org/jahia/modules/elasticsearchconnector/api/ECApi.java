package org.jahia.modules.elasticsearchconnector.api;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.databaseConnector.connection.DatabaseConnectionAPI;
import org.jahia.modules.databaseConnector.services.DatabaseConnectorService;
import org.jahia.modules.elasticsearchconnector.connection.ElasticSearchConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * 2017-05-17
 *
 * @author Astrit Ademi
 */

@Path("/dbconn/elasticsearch")
@Produces({"application/hal+json"})
public class ECApi extends DatabaseConnectionAPI {
    private static final Logger logger = LoggerFactory.getLogger(ECApi.class);
    public final static String ENTRY_POINT = "/elasticsearch";
    private final DatabaseConnectorService databaseConnectorService;

    public ECApi() {
        super(ECApi.class);
        this.databaseConnectorService = (DatabaseConnectorService) getDatabaseConnector();
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

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addConnection(String data) {
        try {
            JSONObject connectionParameters = new JSONObject(data);
            JSONArray missingParameters = new JSONArray();
            if (!connectionParameters.has("id") || StringUtils.isEmpty(connectionParameters.getString("id"))) {
                missingParameters.put("id");
            }
            if (!connectionParameters.has("host") || StringUtils.isEmpty(connectionParameters.getString("host"))) {
                missingParameters.put("host");
            }
            if (missingParameters.length() > 0) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"missingParameters\":" + missingParameters.toString() + "}").build();
            } else {
                String id = connectionParameters.has("id") ? connectionParameters.getString("id") : null;
                String host = connectionParameters.has("host") ? connectionParameters.getString("host") : null;
                Integer port = connectionParameters.has("port") && !StringUtils.isEmpty(connectionParameters.getString("port")) ? connectionParameters.getInt("port") : null;
                Boolean isConnected = connectionParameters.has("isConnected") && connectionParameters.getBoolean("isConnected");
                String password = connectionParameters.has("password") ? connectionParameters.getString("password") : null;
                String user = connectionParameters.has("user") ? connectionParameters.getString("user") : null;
                String clusterName = connectionParameters.has("clusterName") ? connectionParameters.getString("clusterName") : null;
                String options = connectionParameters.has("options") ? connectionParameters.getString("options") : null;
                ElasticSearchConnection connection = new ElasticSearchConnection(id);
                connection.setHost(host);
                connection.setPort(port);
                connection.setPassword(password);
                connection.setUser(user);
                connection.isConnected(isConnected);
                connection.setClusterName(clusterName);
                connection.setOptions(options);
                connection.setDatabaseType(ElasticSearchConnection.DATABASE_TYPE);
                JSONObject jsonAnswer = new JSONObject();
                if (!databaseConnectorService.testConnection(connection)) {
                    connection.isConnected(false);
                    jsonAnswer.put("connectionVerified", false);
                } else {
                    jsonAnswer.put("connectionVerified", true);
                }
                databaseConnectorService.addEditConnection(connection, false);
                jsonAnswer.put("success", "Connection successfully added");
                logger.info("Successfully created ElasticSearchDB connection: " + id);
                return Response.status(Response.Status.OK).entity(jsonAnswer.toString()).build();
            }
        } catch (JSONException e) {
            logger.error("Cannot parse json data : {}", data);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"Cannot parse json data\"}").build();
        }
    }

    @DELETE
    @Path("/remove/{connectionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeConnection(@PathParam("connectionId") String connectionId) {
        databaseConnectorService.removeConnection(connectionId, ElasticSearchConnection.DATABASE_TYPE);
        logger.info("Successfully deleted ElasticSearch connection: " + connectionId);
        return Response.status(Response.Status.OK).entity("{\"success\": \"Successfully removed ElasticSearch connection\"}").build();
    }

    @PUT
    @Path("/edit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editConnection(String data) {
        try {
            JSONObject connectionParameters = new JSONObject(data);
            JSONArray missingParameters = new JSONArray();
            if (!connectionParameters.has("id") || StringUtils.isEmpty(connectionParameters.getString("id"))) {
                missingParameters.put("id");
            }
            if (!connectionParameters.has("oldId") || StringUtils.isEmpty(connectionParameters.getString("oldId"))) {
                missingParameters.put("oldId");
            }
            if (!connectionParameters.has("host") || StringUtils.isEmpty(connectionParameters.getString("host"))) {
                missingParameters.put("host");
            }
            if (missingParameters.length() > 0) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"missingParameters\":" + missingParameters.toString() + "}").build();
            } else {
                String id = connectionParameters.has("id") ? connectionParameters.getString("id") : null;
                String oldId = connectionParameters.has("oldId") ? connectionParameters.getString("oldId") : null;
                String host = connectionParameters.has("host") ? connectionParameters.getString("host") : null;
                Integer port = connectionParameters.has("port") && !StringUtils.isEmpty(connectionParameters.getString("port")) ? connectionParameters.getInt("port") : null;
                Boolean isConnected = connectionParameters.has("isConnected") && connectionParameters.getBoolean("isConnected");
                String clusterName = connectionParameters.has("clusterName") ? connectionParameters.getString("clusterName") : null;
                String password = connectionParameters.has("password") ? connectionParameters.getString("password") : null;
                String user = connectionParameters.has("user") ? connectionParameters.getString("user") : null;
                String options = connectionParameters.has("options") ? connectionParameters.getString("options") : null;

                ElasticSearchConnection connection = new ElasticSearchConnection(id);

                connection.setOldId(oldId);
                connection.setHost(host);
                connection.setPort(port);
                connection.setPassword(password);
                connection.setUser(user);
                connection.isConnected(isConnected);
                connection.setClusterName(clusterName);
                connection.setOptions(options);
                connection.setDatabaseType(ElasticSearchConnection.DATABASE_TYPE);

                JSONObject jsonAnswer = new JSONObject();
                if (!databaseConnectorService.testConnection(connection)) {
                    connection.isConnected(false);
                    jsonAnswer.put("connectionVerified", false);
                } else {
                    jsonAnswer.put("connectionVerified", true);
                }
                databaseConnectorService.addEditConnection(connection, true);
                jsonAnswer.put("success", "ElasticSearch Connection successfully edited");
                logger.info("Successfully edited ElasticSearch connection: " + id);
                return Response.status(Response.Status.OK).entity(jsonAnswer.toString()).build();
            }
        } catch (JSONException e) {
            logger.error("Cannot parse json data : {}", data);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"Cannot parse json data\"}").build();
        }
    }

    @PUT
    @Path("/connect/{connectionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response connect(@PathParam("connectionId") String connectionId) {
        JSONObject jsonAnswer = new JSONObject();
        try {
            if (databaseConnectorService.updateConnection(connectionId, ElasticSearchConnection.DATABASE_TYPE, true)) {
                jsonAnswer.put("success", "Successfully connected to ElasticSearch");
                logger.info("Successfully enabled ElasticSearch connection, for connection with id: " + connectionId);
            } else {
                jsonAnswer.put("failed", "Connection failed to update");
                logger.info("Failed to establish ElasticSearch connection, for connection with id: " + connectionId);
            }
        } catch (JSONException ex) {
            logger.error(ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"\"Invalid connection parameter\"}").build();
        }
        return Response.status(Response.Status.OK).entity(jsonAnswer.toString()).build();
    }

    @PUT
    @Path("/disconnect/{connectionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response disconnect(@PathParam("connectionId") String connectionId) {
        databaseConnectorService.updateConnection(connectionId, ElasticSearchConnection.DATABASE_TYPE, false);
        logger.info("Successfully disconnected ElasticSearch connection, for connection with id: " + connectionId);
        return Response.status(Response.Status.OK).entity("{\"success\": \"Successfully disconnected from ElasticSearch\"}").build();
    }

    @GET
    @Path("/isconnectionvalid/{connectionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response isConnectionIdAvailable(@PathParam("connectionId") String connectionId) {
        try {
            return Response.status(Response.Status.OK).entity(databaseConnectorService.isConnectionIdAvailable(connectionId, ElasticSearchConnection.DATABASE_TYPE)).build();
        } catch (InstantiationException ex) {
            logger.error("Cannot instantiate connection class" + ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"Cannot access connection\"}").build();
        } catch (IllegalAccessException ex) {
            logger.error("Cannot access connection class" + ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"Cannot access connection\"}").build();
        }
    }

    @POST
    @Path("/testconnection")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testConnection(String data) {
        try {
            JSONObject connectionParameters = new JSONObject(data);
            JSONArray missingParameters = new JSONArray();
            if (!connectionParameters.has("id") || StringUtils.isEmpty(connectionParameters.getString("id"))) {
                missingParameters.put("id");
            }
            if (!connectionParameters.has("host") || StringUtils.isEmpty(connectionParameters.getString("host"))) {
                missingParameters.put("host");
            }
            if (missingParameters.length() > 0) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"missingParameters\":" + missingParameters.toString() + "}").build();
            } else {
                String id = connectionParameters.has("id") ? connectionParameters.getString("id") : null;
                String host = connectionParameters.has("host") ? connectionParameters.getString("host") : null;
                Integer port = connectionParameters.has("port") && !StringUtils.isEmpty(connectionParameters.getString("port")) ? connectionParameters.getInt("port") : null;
                Boolean isConnected = connectionParameters.has("isConnected") && connectionParameters.getBoolean("isConnected");
                String clusterName = connectionParameters.has("clusterName") ? connectionParameters.getString("clusterName") : null;
                String options = connectionParameters.has("options") ? connectionParameters.getString("options") : null;

                ElasticSearchConnection connection = new ElasticSearchConnection(id);

                connection.setHost(host);
                connection.setPort(port);
                connection.isConnected(isConnected);
                connection.setClusterName(clusterName);
                connection.setOptions(options);

                boolean connectionTestPassed = databaseConnectorService.testConnection(connection);
                logger.info(connectionTestPassed ? "Connection test successfully passed" : "Connection test failed" + " for ElasticSearch with id: " + id);
                return Response.status(Response.Status.OK).entity("{\"result\": " + connectionTestPassed + "}").build();
            }
        } catch (JSONException e) {
            logger.error("Cannot parse json data : {}", data);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"Cannot parse json data\"}").build();
        }
    }

    @GET
    @Path("/status/{connectionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getServerStatus(@PathParam("connectionId") String connectionId) {
        try {
            Map<String, Object> serverStatus = databaseConnectorService.getServerStatus(connectionId, ElasticSearchConnection.DATABASE_TYPE);
            if (serverStatus.containsKey("failed")) {
                logger.info("Failed to retrieve Status for ElasticSearch connection with id: " + connectionId);
            } else {
                logger.info("Successfully retrieved Status for ElasticSearch connection with id: " + connectionId);
            }
            return Response.status(Response.Status.OK).entity(serverStatus).build();
        } catch (Exception e) {
            logger.error("Failed retrieve Status for ElasticSearch connection with id: " + connectionId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"failed\":\"Cannot get database status\"}").build();
        }
    }
}
