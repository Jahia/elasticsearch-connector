|  | Badges | 
| --- | --- |
| Module | ![ID](https://img.shields.io/badge/ID-elasticsearch--connector-blue) [![Store](https://img.shields.io/badge/Jahia%20Store-Yes-brightgreen)](https://store.jahia.com/contents/modules-repository/org/jahia/modules/elasticsearch-connector.html) |
| CI / CD | [![CircleCI](https://circleci.com/gh/Jahia/elasticsearch-connector/tree/master.svg?style=svg)](https://circleci.com/gh/Jahia/elasticsearch-connector/tree/master) ![Unit Tests](https://img.shields.io/badge/Unit%20Tests-No-red) ![Integration Tests](https://img.shields.io/badge/Integration%20Tests-No-red) ![Build Snapshot](https://img.shields.io/badge/Build%20Snapshot-Yes-brightgreen) ![Build Release](https://img.shields.io/badge/Build%20Release-No-red) | 
| Artifacts | [![Snapshot](https://img.shields.io/badge/Snapshot-Nexus-blue)](https://devtools.jahia.com/nexus/content/repositories/jahia-enterprise-snapshots/org/jahia/modules/elasticsearch-connector-7/) [![Release](https://img.shields.io/badge/Release-Nexus-blue)](https://devtools.jahia.com/nexus/content/repositories/jahia-enterprise-releases/org/jahia/modules/elasticsearch-connector-7/) |
| Slack | [![Discussion](https://img.shields.io/badge/Discussion-%23module--elasticsearch--connector-blue)](https://jahia.slack.com/archives/C013XUC9P1C) [![Notifications](https://img.shields.io/badge/Notifications-%23cci--elasticsearch--connector-blue)](https://jahia.slack.com/archives/C014EGFLGQH)|

# Elasticsearch connector

This connector is intended for use with database connector.

## Jahia Cloud

IMPORTANT: The REST API exposed by this module is used by the cloud team, its methods MUST NOT be modified without prior consultation with the Jahia Cloud team as it might have impacts on their implementation

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


 
 

