---
page:
  '$path': '/sites/academy/home/documentation/augmented-search/augmented-search-4-x/overview-and-set-up/installing-augmented-search-on-premises'
content:
  '$subpath': document-area/creating-the-connection-between-jahia-and-elasticsearch
---

## Creating the connection between Jahia and Elasticsearch

Since 4.0.0, elasticsearch connection is configured through org.jahia.modules.elasticsearchConnector.cfg OSGi configuration file.

For basic configuration, specify the following properties:
- `elasticsearchConnector.host` - The IP/hostname of your Elasticsearch server
- `elasticsearchConnector.port` - The port used by your Elasticsearch server

:::warning
Note: only one Elasticsearch connection can be configured at a time
:::

### Secured connection to Elasticsearch cluster

If your Elasticsearch cluster uses security and/or authentication, you can also configure the following additional settings:
- `elasticsearchConnector.useXPackSecurity` - Enable XPack security integration (authentication)
- `elasticsearchConnector.useEncryption` - Enable HTTPS encryption for connections
- `elasticsearchConnector.user` - Username for Elasticsearch authentication
- `elasticsearchConnector.password` - Encrypted password for Elasticsearch authentication

Use Jahia's encryption utility to generate an encrypted password. One way is to execute this code in tools > groovy console:
```java
org.jahia.utils.EncryptionUtils.passwordBaseEncrypt("myPassword")
```

### Testing the connection

If the connection setup is ok, then you should be able to go to Augmented Search UI (Administration > Configuration > Augmented Search) without any errors displayed. 

It is also possible to test your elasticsearch connection using the graphql query:
```graphql
query testESConnection {
  admin {
    elasticsearch {
      testConnection
    }
  }
}
```
