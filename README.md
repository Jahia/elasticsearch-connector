# Elasticsearch connector

This connector is intended for use with database connector.

## Usage 

To use you need to have elasticsearch running with transport tcp enabled at port 9300. To enable it you need to add 

transport.tcp.port: 9300

to your elasticsearch.yml.

#### Setting up a connection

You can set up a connection by going to Administration -> Configuration -> Database connector. Click on "plus" button and 
fill out the form. Please note that while this connector is in development stage you may find UI buggy and after 
adding connection you can see two displayed in this case simply refresh the page.

#### Using connection

1. To use connection in a module add a dependency on database-connector and elasticsearch-connector.

2. Import elasticsearch connector registry <osgi:reference id="esRegistry" interface="org.jahia.modules.elasticsearchconnector.connection.ElasticSearchConnectionRegistry" />

3. Use registry to get your connection (ElasticSearchTransportClient) esRegistry.getConnectionService(ElasticSearchConnection.DATABASE_TYPE, "yourConnectionName")


 
 

