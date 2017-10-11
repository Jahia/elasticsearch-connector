<md-content flex layout-padding>
    <div layout="row" layout-padding layout-wrap class="box-wrap">
        <div flex="100" layout-padding>
            <h2 class="md-title bg-info center-align">{{egsc.title}}</h2>
        </div>
        <div flex="50">
            <dl>
                <dt message-key="dc_databaseConnector.label.statistics.hostName"></dt>
                <dd>{{egsc.connectionStatus.aboutConnection.host}}</dd>
                <dt message-key="ec_elasticsearchConnector.label.statistics.port"></dt>
                <dd>{{egsc.connectionStatus.aboutConnection.port}} </dd>
                <dt message-key="ec_elasticsearchConnector.label.statistics.clusterName"></dt>
                <dd><time>{{egsc.connectionStatus.clusterName.value}} </time></dd>
                <dt message-key="ec_elasticsearchConnector.label.statistics.clusterStatus"></dt>
                <dd>{{egsc.connectionStatus.status}}</dd>
                <dt message-key="dc_databaseConnector.label.version"></dt>
                <dd>{{egsc.connectionStatus.aboutConnection.dbVersion}}</dd>
            </dl>
        </div>
        <div flex="50">
            <dl>
                <dt message-key="ec_elasticsearchConnector.label.statistics.indexNumber"></dt>
                <dd>{{egsc.connectionStatus.indicesStats.indexCount}}</dd>
                <dt message-key="ec_elasticsearchConnector.label.statistics.nodesNumber"></dt>
                <dd>{{egsc.connectionStatus.nodes.length}}</dd>
                <dt message-key="ec_elasticsearchConnector.label.statistics.storeSize"></dt>
                <dd>{{(egsc.connectionStatus.indicesStats.store.sizeInBytes / (1024 * 1024)).toFixed(2)}} MB</dd>
                <dt message-key="ec_elasticsearchConnector.label.statistics.maxHeap"></dt>
                <dd>{{(egsc.connectionStatus.nodesStats.jvm.heapMax / (1024 * 1024)).toFixed(2)}} MB</dd>
            </dl>
        </div>
    </div>
</md-content>