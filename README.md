|  | Badges | 
| --- | --- |
| Module | ![ID](https://img.shields.io/badge/ID-elasticsearch--connector-blue) [![Store](https://img.shields.io/badge/Jahia%20Store-Yes-brightgreen)](https://store.jahia.com/contents/modules-repository/org/jahia/modules/elasticsearch-connector.html) |
| Artifacts | [![Snapshot](https://img.shields.io/badge/Snapshot-Nexus-blue)](https://devtools.jahia.com/nexus/content/repositories/jahia-enterprise-snapshots/org/jahia/modules/elasticsearch-connector-7/) [![Release](https://img.shields.io/badge/Release-Nexus-blue)](https://devtools.jahia.com/nexus/content/repositories/jahia-enterprise-releases/org/jahia/modules/elasticsearch-connector-7/) |

# Elasticsearch connector

Note that as of version 4.0.0 `database-connector` is no longer necessary to establish connection to `Elasticsearch`.

#### Setting up a connection

You can set up a connection by modifying [org.jahia.modules.elasticsearchConnector.cfg]
(src%2Fmain%2Fresources%2FMETA-INF%2Fconfigurations%2Forg.jahia.modules.elasticsearchConnector.cfg) file. 

#### Using connection

1. To use connection in a module add a dependency on `elasticsearch-connector`.

2. Import `ElasticsearchClientWrapper` service. You can do so by using `@Reference` annotation, using a service tracker or `BundleUtils.getOSGIService()`.

3. Use `ElasticsearchClient` via `getClient()` or low-level rest client via `getRest5Client()` APIs available on the client wrapper.

As of 4.0.0 you can only have one connection, and it can only be configured via the `.cfg` file mentioned above. There is no UI. 
Connection will be created/updated on demand once any of the `ElasticsearchClientWrapper` API is used.
