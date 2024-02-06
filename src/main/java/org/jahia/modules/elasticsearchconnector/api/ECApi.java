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
 *     Copyright (C) 2002-2020 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.elasticsearchconnector.api;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.databaseConnector.connection.DatabaseConnectionAPI;
import org.jahia.modules.databaseConnector.services.DatabaseConnectorService;
import org.jahia.modules.elasticsearchconnector.ESConstants;
import org.jahia.modules.elasticsearchconnector.connection.ElasticSearchConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
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
    public static final String ENTRY_POINT = "/elasticsearch";
    private static final String ERROR_CANNOT_ACCESS_CONNECTION = "{\"error\":\"Cannot access connection\"}";
    private static final MessageFormat missingParametersMessage = new MessageFormat("'{'\"missingParameters\":\"{0}\"'}'");
    private static final String CONNECTION_VERIFIED = "connectionVerified";
    private static final String CANNOT_PARSE_JSON_DATA = "Cannot parse json data";
    private static final String ERROR_CANNOT_PARSE_JSON_DATA = "{\"error\":\"Cannot parse json data\"}";
    private final DatabaseConnectorService databaseConnectorService;

    /**
     * Public constructor
     */
    public ECApi() {
        super(ECApi.class);
        this.databaseConnectorService = getDatabaseConnector();
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
            logger.error("Cannot instantiate connection class {}", ex.getMessage(), ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ERROR_CANNOT_ACCESS_CONNECTION).build();
        } catch (IllegalAccessException ex) {
            logger.error("Cannot access connection class {}", ex.getMessage(), ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ERROR_CANNOT_ACCESS_CONNECTION).build();
        }
    }

    /**
     * Try to add a connection to the system return JSON object with status (success or failure)
     *
     * @param data JSON object with all the needed parameters
     * @return Json Response object {"connectionVerified":true | false}
     */
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
                return Response.status(Response.Status.BAD_REQUEST).entity(missingParametersMessage.format(missingParameters.toString())).build();
            } else {
                JSONObject jsonAnswer = processConnection(connectionParameters);
                return Response.status(Response.Status.OK).entity(jsonAnswer.toString()).build();
            }
        } catch (JSONException e) {
            logger.error(CANNOT_PARSE_JSON_DATA, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ERROR_CANNOT_PARSE_JSON_DATA).build();
        }
    }

    private JSONObject processConnection(JSONObject connectionParameters) throws JSONException {
        ElasticSearchConnection connection = createEsConnection(connectionParameters);
        connection.setDatabaseType(ElasticSearchConnection.DATABASE_TYPE);

        JSONObject jsonAnswer = new JSONObject();
        if (!databaseConnectorService.testConnection(connection)) {
            connection.isConnected(false);
            jsonAnswer.put(CONNECTION_VERIFIED, false);
        } else {
            jsonAnswer.put(CONNECTION_VERIFIED, true);
        }
        databaseConnectorService.addEditConnection(connection, false);
        jsonAnswer.put(ESConstants.SUCCESSKEY, "Connection successfully added");
        logger.info("Successfully created ElasticSearchDB connection: {}", connection.getId());
        return jsonAnswer;
    }

    /**
     * Delete an identified connection from the system
     *
     * @param connectionId id of the connection to be deleted
     * @return JSON objectr containing status result of operation
     */
    @DELETE
    @Path("/remove/{connectionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeConnection(@PathParam("connectionId") String connectionId) {
        databaseConnectorService.removeConnection(connectionId, ElasticSearchConnection.DATABASE_TYPE);
        logger.info("Successfully deleted ElasticSearch connection: {}", connectionId);
        return Response.status(Response.Status.OK).entity("{\"success\": \"Successfully removed ElasticSearch connection\"}").build();
    }

    /**
     * Update a connection
     *
     * @param data Updated JSON data of the connection
     * @return Json Response object {"connectionVerified":true | false}
     */
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
            if (!connectionParameters.has(ESConstants.OLD_ID) || StringUtils.isEmpty(connectionParameters.getString(ESConstants.OLD_ID))) {
                missingParameters.put(ESConstants.OLD_ID);
            }
            if (!connectionParameters.has("host") || StringUtils.isEmpty(connectionParameters.getString("host"))) {
                missingParameters.put("host");
            }
            if (missingParameters.length() > 0) {
                return Response.status(Response.Status.BAD_REQUEST).entity(missingParametersMessage.format(missingParameters.toString())).build();
            } else {
                JSONObject jsonAnswer = updateConnection(connectionParameters);
                return Response.status(Response.Status.OK).entity(jsonAnswer.toString()).build();
            }
        } catch (JSONException e) {
            logger.error(CANNOT_PARSE_JSON_DATA, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ERROR_CANNOT_PARSE_JSON_DATA).build();
        }
    }

    private JSONObject updateConnection(JSONObject connectionParameters) throws JSONException {
        ElasticSearchConnection connection = createEsConnection(connectionParameters);
        String oldId = connectionParameters.has(ESConstants.OLD_ID) ? connectionParameters.getString(ESConstants.OLD_ID) : null;
        connection.setOldId(oldId);
        connection.setDatabaseType(ElasticSearchConnection.DATABASE_TYPE);

        JSONObject jsonAnswer = new JSONObject();
        if (!databaseConnectorService.testConnection(connection)) {
            connection.isConnected(false);
            jsonAnswer.put(CONNECTION_VERIFIED, false);
        } else {
            jsonAnswer.put(CONNECTION_VERIFIED, true);
        }
        databaseConnectorService.addEditConnection(connection, true);
        jsonAnswer.put(ESConstants.SUCCESSKEY, "ElasticSearch Connection successfully edited");
        logger.info("Successfully edited ElasticSearch connection: {}", connection.getId());
        return jsonAnswer;
    }

    /**
     * Try to connect to the defined server
     *
     * @param connectionId id of the connection object
     * @return {"success"} or {"failed"}
     */
    @PUT
    @Path("/connect/{connectionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response connect(@PathParam("connectionId") String connectionId) {
        JSONObject jsonAnswer = new JSONObject();
        try {
            if (databaseConnectorService.updateConnection(connectionId, ElasticSearchConnection.DATABASE_TYPE, true)) {
                jsonAnswer.put(ESConstants.SUCCESSKEY, "Successfully connected to ElasticSearch");
                logger.info("Successfully enabled ElasticSearch connection, for connection with id: {}", connectionId);
            } else {
                jsonAnswer.put("failed", "Connection failed to update");
                logger.info("Failed to establish ElasticSearch connection, for connection with id: {}", connectionId);
            }
        } catch (JSONException ex) {
            logger.error("Invalid connection parameter: {}", ex.getMessage(), ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"\"Invalid connection parameter\"}").build();
        }
        return Response.status(Response.Status.OK).entity(jsonAnswer.toString()).build();
    }

    /**
     * Disconnect from identified server connection
     *
     * @param connectionId id of the connection to disconnect
     * @return {"success"}
     */
    @PUT
    @Path("/disconnect/{connectionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response disconnect(@PathParam("connectionId") String connectionId) {
        databaseConnectorService.updateConnection(connectionId, ElasticSearchConnection.DATABASE_TYPE, false);
        logger.info("Successfully disconnected ElasticSearch connection, for connection with id: {}", connectionId);
        return Response.status(Response.Status.OK).entity("{\"success\": \"Successfully disconnected from ElasticSearch\"}").build();
    }

    /**
     * Check if the new identifier is available
     *
     * @param connectionId id to be checked for abvailability
     * @return {true} | {false}
     */
    @GET
    @Path("/isconnectionvalid/{connectionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response isConnectionIdAvailable(@PathParam("connectionId") String connectionId) {
        try {
            boolean connectionIdAvailable = databaseConnectorService.isConnectionIdAvailable(connectionId, ElasticSearchConnection.DATABASE_TYPE);
            return Response.status(Response.Status.OK).entity(connectionIdAvailable).build();
        } catch (InstantiationException ex) {
            logger.error("Cannot instantiate connection class: {}", ex.getMessage(), ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ERROR_CANNOT_ACCESS_CONNECTION).build();
        } catch (IllegalAccessException ex) {
            logger.error("Cannot access connection class: {}", ex.getMessage(), ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ERROR_CANNOT_ACCESS_CONNECTION).build();
        }
    }

    /**
     * Test if a connection is valid
     *
     * @param data parameters of the connection to be tested
     * @return {"result":true}|false}
     */
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
                return Response.status(Response.Status.BAD_REQUEST).entity(missingParametersMessage.format(missingParameters.toString())).build();
            } else {
                boolean connectionTestPassed = isConnectionTestPassed(connectionParameters);
                return Response.status(Response.Status.OK).entity("{\"result\": " + connectionTestPassed + "}").build();
            }
        } catch (JSONException e) {
            logger.error(CANNOT_PARSE_JSON_DATA, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ERROR_CANNOT_PARSE_JSON_DATA).build();
        }
    }

    private boolean isConnectionTestPassed(JSONObject connectionParameters) throws JSONException {
        ElasticSearchConnection connection = createEsConnection(connectionParameters);
        return databaseConnectorService.testConnection(connection);
    }

    /**
     * Get status of the server (cluster info, statistics, etc.)
     *
     * @param connectionId id of the connection
     * @return JSON object with all the info
     */
    @GET
    @Path("/status/{connectionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getServerStatus(@PathParam("connectionId") String connectionId) {
        try {
            Map<String, Object> serverStatus = databaseConnectorService.getServerStatus(connectionId, ElasticSearchConnection.DATABASE_TYPE);
            if (serverStatus.containsKey("failed")) {
                logger.info("Failed to retrieve Status for ElasticSearch connection with id: {}", connectionId);
            } else {
                logger.info("Successfully retrieved Status for ElasticSearch connection with id: {}", connectionId);
            }
            return Response.status(Response.Status.OK).entity(serverStatus).build();
        } catch (Exception e) {
            logger.error("Failed retrieve Status for ElasticSearch connection with id: {}", connectionId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"failed\":\"Cannot get database status\"}").build();
        }
    }

    private ElasticSearchConnection createEsConnection(JSONObject connectionParameters) {
        String id = connectionParameters.has("id") ? connectionParameters.getString("id") : null;
        String host = connectionParameters.has("host") ? connectionParameters.getString("host") : null;
        Integer port = null;
        try {
            port = connectionParameters.getInt("port");
        } catch (JSONException e) { /* Do nothing; default to null */ }
        Boolean isConnected = connectionParameters.has(ESConstants.IS_CONNECTED) && connectionParameters.getBoolean(ESConstants.IS_CONNECTED);
        String user = connectionParameters.has("user") ? connectionParameters.getString("user") : null;
        String password = connectionParameters.has(ESConstants.CREDKEY) ? connectionParameters.getString(ESConstants.CREDKEY) : null;
        String options = connectionParameters.has(ESConstants.OPTIONSKEY) ? connectionParameters.get(ESConstants.OPTIONSKEY).toString() : null;

        ElasticSearchConnection connection = new ElasticSearchConnection(id);
        connection.setHost(host);
        connection.setPort(port);
        connection.isConnected(isConnected);
        connection.setUser(user);
        connection.setPassword(password);
        connection.setOptions(options);

        return connection;
    }
}
