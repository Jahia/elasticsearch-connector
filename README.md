<a href="https://www.jahia.com/">
    <img src="https://www.jahia.com/modules/jahiacom-templates/images/jahia-3x.png" alt="Jahia logo" title="Jahia" align="right" height="60" />
</a>

# Elasticsearch connector

Note that as of version 4.0.0 `database-connector` is no longer necessary to establish connection to `Elasticsearch`.

## Setting up a connection

You can set up a connection by modifying [org.jahia.modules.elasticsearchConnector.cfg](src%2Fmain%2Fresources%2FMETA-INF%2Fconfigurations%2Forg.jahia.modules.elasticsearchConnector.cfg) file. 

## Using connection

1. To use connection in a module add a dependency on `elasticsearch-connector`.

2. Import `ElasticsearchClientWrapper` service. You can do so by using `@Reference` annotation, using a service tracker or `BundleUtils.getOSGIService()`.

3. Use `ElasticsearchClient` via `getClient()` or low-level rest client via `getRest5Client()` APIs available on the client wrapper.

As of 4.0.0 you can only have one connection, and it can only be configured via the `.cfg` file mentioned above. There is no UI. 
Connection will be created/updated on demand once any of the `ElasticsearchClientWrapper` API is used. 

## Open-Source

This is an Open-Source module, you can find more details about Open-Source @ Jahia [in this repository](https://github.com/Jahia/open-source).
